package com.bsit.cyclesync.ui.home

import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsit.cyclesync.model.Place
import com.bsit.cyclesync.model.Ride
import com.bsit.cyclesync.model.Session
import com.bsit.cyclesync.model.User
import com.bsit.cyclesync.model.response.Prediction
import com.bsit.cyclesync.services.LocationService
import com.bsit.cyclesync.services.network.DirectionsApi
import com.bsit.cyclesync.services.network.PlacesApi
import com.bsit.cyclesync.services.prefs.PreferencesManager
import com.bsit.cyclesync.ui.utils.FirebaseUtils
import com.bsit.cyclesync.ui.utils.PolyLineUtils
import com.bsit.cyclesync.ui.utils.TableConstants
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.PolyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placesApi: PlacesApi,
    private val directionsApi: DirectionsApi,
    private val locationService: LocationService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val defaultLatLng = LatLng(14.746043, 120.9761594)
    private val database = FirebaseUtils.database.reference
    private val userRef = database.child(TableConstants.USERS)
    private val sessionRef = database.child(TableConstants.SESSION)
    private var currentUser: User? by mutableStateOf(null)
    val currentFirebaseUser by mutableStateOf(FirebaseUtils.auth.currentUser)

    var currentLocation by mutableStateOf(defaultLatLng)
    var directionPolyLine: List<LatLng> by mutableStateOf(emptyList())
    var bikers by mutableStateOf(listOf<User>())
    var tempBikers by mutableStateOf(listOf<User>())
    val selectedBikers = mutableStateListOf<User>()
    var showBottomSheet by mutableStateOf(false)

    var trialSessionsLeft by mutableIntStateOf(3)
        private set

    var fromQuery by mutableStateOf("")
        private set
    var toQuery by mutableStateOf("")
        private set

    var fromPredictions by mutableStateOf<List<Prediction>>(emptyList())
        private set
    var toPredictions by mutableStateOf<List<Prediction>>(emptyList())
        private set

    var fromLatLng by mutableStateOf<LatLng?>(null)
        private set
    var toLatLng by mutableStateOf<LatLng?>(null)
        private set
    var isRideActive by mutableStateOf(false)
        private set
    var isActiveSession by mutableStateOf(false)
    var activeSession: Session? by mutableStateOf(null)

    var distance by mutableDoubleStateOf(0.0)

    var elapsedTime by mutableLongStateOf(0L)
    var startTime by mutableLongStateOf(0L)

    var speed by mutableDoubleStateOf(0.0)

    var isAsMemberActive by mutableStateOf(false)
    var canStartSession by mutableStateOf(false)
        private set
    var sessionCheckMessage by mutableStateOf<String?>(null)
        private set
    var memberSpeeds by mutableStateOf<Map<String, Double>>(emptyMap())
    private var tempBikersListener: ValueEventListener? = null
    private var membersListener: ValueEventListener? = null


    // Index of the last segment the user was on, to optimize searching
    private var lastKnownSegmentIndex = 0

    // The original, full route from the Directions API or a predefined list
    private var originalPolylinePoints: List<LatLng> = emptyList()
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage


    fun clearToastMessage() {
        _toastMessage.value = null
    }


    var sessionId: String by mutableStateOf("")

    // TODO implement a function that can handle if a user is only a member of an active session
    // TODO that can also control whether the user wants to leave
    init {
        viewModelScope.launch {
            delay(500L)
            getUserData()
            checkIfMember()
        }

        viewModelScope.launch {
            preferencesManager.activeSessionId
                .flowOn(Dispatchers.IO)
                .map { uuid ->
                    uuid
                }
                .collect { uuid ->
                    uuid?.let {
                        if (it.isNotEmpty()) {
                            val currentSession = sessionRef.child(it)
                            currentSession.get()
                                .addOnSuccessListener { snapshot ->
                                    val session = snapshot.getValue(Session::class.java)
                                    session?.let {
                                        activeSession = session
                                        currentFirebaseUser?.let { user ->
                                            if (session.sessionOwner == user.uid) {
                                                isActiveSession = session.active
                                                loadMembersRealtime()

                                                if (directionPolyLine.isEmpty()) {
                                                    directionPolyLine =
                                                        PolyLineUtils.getDirectionPolyLines()
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
        }

        viewModelScope.launch {
            delay(1000L)
            loadTempBikers()
        }
    }

    /**
     * Update current user location and
     * reflect it to firebase
     */
    fun updateCurrentLocationToFirebase() {
        fetchCurrentLocation()
    }

    fun updateDistance(previousLocation: LatLng, newPoint: LatLng) {
        if (directionPolyLine.isNotEmpty()) {
            this.distance += calculateDistance(previousLocation, newPoint)
        }
    }

    fun updateElapsedTime() {
        if (directionPolyLine.isNotEmpty()) {
            this.elapsedTime = (System.currentTimeMillis() - startTime) / 1000
        } else {
            startTime = System.currentTimeMillis()
        }
    }

    fun updateSpeed() {
        this.speed = if (elapsedTime > 0) (distance / elapsedTime) * 3.6 else 0.0
        val uid = currentFirebaseUser?.uid ?: return
        userRef.child(uid).child("currentSpeed").setValue(this.speed)
    }

    fun onFromQueryChanged(query: String) {
        fromQuery = query
        viewModelScope.launch {
            val response = placesApi.autocomplete(query)
            fromPredictions = response.predictions
        }
    }


    fun onToQueryChanged(query: String) {
        toQuery = query
        viewModelScope.launch {
            val response = placesApi.autocomplete(query)
            toPredictions = response.predictions
        }
    }


    fun onFromPredictionSelected(prediction: Prediction) {
        fromQuery = prediction.description
        fromPredictions = emptyList()
        try {
            viewModelScope.launch {
                val response = placesApi.placeDetails(prediction.place_id)
                val loc = response.result.geometry.location
                fromLatLng = LatLng(loc.lat, loc.lng)
                tryGetRoute()
            }
        } catch (e: Exception) {
            Log.e(this::class.toString(), e.message.toString())
        }
    }


    fun onToPredictionSelected(prediction: Prediction) {
        toQuery = prediction.description
        toPredictions = emptyList()

        viewModelScope.launch {
            try {
                val response = placesApi.placeDetails(prediction.place_id)
                val loc = response.result.geometry.location
                toLatLng = LatLng(loc.lat, loc.lng)
                tryGetRoute()
            } catch (e: Exception) {
                Log.e(this::class.toString(), e.message.toString())
            }
        }
    }

    fun decrementTrialSession() {
        val uid = FirebaseUtils.auth.currentUser?.uid ?: return
        if (trialSessionsLeft > 0) {
            trialSessionsLeft -= 1
            userRef.child(uid).child("trialSessionsLeft").setValue(trialSessionsLeft)
        }
    }


    private fun tryGetRoute(customFrom: LatLng? = null, customTo: LatLng? = null) {
        if (customFrom != null && customTo != null) {
            viewModelScope.launch {
                val response = directionsApi.getRoute(
                    origin = "${customFrom.latitude},${customFrom.longitude}",
                    destination = "${customTo.latitude},${customTo.longitude}",
                    key = TableConstants.DIRECTIONS_KEY
                )
                val encodedPolyline = response.routes.firstOrNull()?.overviewPolyline?.points
                encodedPolyline?.let {
                    directionPolyLine = PolyUtil.decode(it)
                    PolyLineUtils.setDirectionPolyLines(directionPolyLine)
                    originalPolylinePoints = directionPolyLine
                }
            }
        } else if (fromLatLng != null && toLatLng != null) {
            viewModelScope.launch {
                val response = directionsApi.getRoute(
                    origin = "${fromLatLng!!.latitude},${fromLatLng!!.longitude}",
                    destination = "${toLatLng!!.latitude},${toLatLng!!.longitude}",
                    key = TableConstants.DIRECTIONS_KEY
                )
                val encodedPolyline = response.routes.firstOrNull()?.overviewPolyline?.points
                encodedPolyline?.let {
                    directionPolyLine = PolyUtil.decode(it)
                    PolyLineUtils.setDirectionPolyLines(directionPolyLine)
                    originalPolylinePoints = directionPolyLine
                }
            }
        }
    }


    fun loadTempBikers() {
        tempBikersListener?.let { userRef.removeEventListener(it) }  // Remove old
        tempBikersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    it.getValue(User::class.java)
                }
                currentUser?.let { thisUser ->
                    if (thisUser.friends.isNotEmpty()) {
                        tempBikers = users.filter { user ->
                            user.uid != thisUser.uid && thisUser.friends.containsKey(user.uid)
                        }.map { user ->
                            if (user.username.isEmpty()) user.copy(username = "Unknown") else user
                        }
                        Log.d("HomeViewModel", "Loaded ${tempBikers.size} friends")
                    } else {
                        tempBikers = emptyList()
                    }
                } ?: run {
                    tempBikers = emptyList()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "loadTempBikers error: ${error.message}")
            }
        }
        userRef.addValueEventListener(tempBikersListener!!)
    }


    /**
     * Load members from datastore
     */
    fun loadMembersRealtime() {
        val session = activeSession ?: return
        membersListener?.let { userRef.removeEventListener(it) }  // Remove old
        membersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isActiveSession && !isAsMemberActive) return
                val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                val sessionMemberUids = session.members.map { it.uid }.toSet()
                bikers = users.filter { user ->
                    sessionMemberUids.contains(user.uid)
                }.map { user ->
                    if (user.username.isEmpty()) user.copy(username = "Unknown") else user
                }
                Log.d("HomeViewModel", "Loaded ${bikers.size} session members")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "loadMembersRealtime error: ${error.message}")
            }
        }
        userRef.addValueEventListener(membersListener!!)
    }


    private fun getUserData() {
        val uid = FirebaseUtils.auth.currentUser?.uid ?: return

        userRef.child(uid).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            // Convert snapshot to map for flexible parsing
            val data = snapshot.value as? Map<*, *> ?: return@addOnSuccessListener

            // Safely extract all user fields
            val friendsRaw = data["friends"]
            val friendsMap = when (friendsRaw) {
                is Map<*, *> -> friendsRaw.mapKeys { it.key.toString() }
                    .mapValues { true }  // Convert to Map<String, Boolean>
                is List<*> -> friendsRaw.filterIsInstance<String>()
                    .associateWith { true }  // Fallback for old list data
                else -> emptyMap()
            }

            val user = User(
                uid = data["uid"] as? String ?: uid,
                email = data["email"] as? String ?: "",
                username = data["username"] as? String ?: "",
                firstName = data["firstName"] as? String ?: "",
                lastName = data["lastName"] as? String ?: "",
                password = data["password"] as? String ?: "",
                currentLocation = (data["currentLocation"] as? Map<*, *>)?.let {
                    Place(
                        latitude = (it["latitude"] as? Double) ?: 0.0,
                        longitude = (it["longitude"] as? Double) ?: 0.0
                    )
                },
                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                friends = friendsMap,
                trialSessionsLeft = (data["trialSessionsLeft"] as? Long)?.toInt() ?: 3
            )

            this.currentUser = user
            trialSessionsLeft = user.trialSessionsLeft

            if (user.currentLocation != null) {
                currentLocation = LatLng(
                    user.currentLocation.latitude,
                    user.currentLocation.longitude
                )
            } else {
                fetchCurrentLocation()
            }

            bikers = listOf(user)
        }
    }


    private fun fetchCurrentLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation()?.let {
                currentLocation = it
                val uid = FirebaseUtils.auth.currentUser?.uid
                uid?.let {
                    userRef
                        .child(uid)
                        .updateChildren(
                            mapOf(
                                "currentLocation" to currentLocation
                            )
                        ).addOnSuccessListener {
                            println("ViewModel: Success updating user location")
                        }.addOnFailureListener {
                            println("ViewModel: Fail updating user location")
                        }
                }
            }
        }
    }


    fun onSelectBiker(user: User) {
        if (selectedBikers.contains(user)) {
            selectedBikers.remove(user)
        } else {
            selectedBikers.add(user)
        }
    }

    // Function to check if all selected participants can start (creator + members)
    fun checkSessionValidity() {
        val currentUid = currentFirebaseUser?.uid ?: return
        val memberUids = selectedBikers.map { it.uid }
        val allUids = (memberUids + currentUid).distinct()

        if (allUids.isEmpty()) {
            canStartSession = false
            sessionCheckMessage = "No participants selected."
            return
        }

        var validCount = 0
        var totalCount = allUids.size
        val invalidUsers = mutableListOf<String>()

        allUids.forEach { uid ->
            userRef.child(uid).child("trialSessionsLeft").get()
                .addOnSuccessListener { snapshot ->
                    val trials = snapshot.getValue(Int::class.java) ?: 3
                    if (trials > 0) validCount++ else invalidUsers.add(
                        tempBikers.find { it.uid == uid }?.username ?: uid
                    )

                    if (validCount + invalidUsers.size == totalCount) {
                        canStartSession = invalidUsers.isEmpty()
                        sessionCheckMessage = if (canStartSession) null else {
                            "Cannot start: ${invalidUsers.joinToString(", ")} have no trial sessions."
                        }
                    }
                }
                .addOnFailureListener {
                    // On failure, assume invalid for safety
                    invalidUsers.add(tempBikers.find { it.uid == uid }?.username ?: uid)
                    if (validCount + invalidUsers.size == totalCount) {
                        canStartSession = false
                        sessionCheckMessage = "Error checking trials. Please try again."
                    }
                }
        }
    }

    private fun getValidParticipantsWithTrials(
        creatorUid: String,
        memberUids: List<String>,
        callback: (List<String>) -> Unit
    ) {
        val allUids = (memberUids + creatorUid).distinct()
        val validUids = mutableListOf<String>()
        var processedCount = 0
        allUids.forEach { uid ->
            userRef.child(uid).child("trialSessionsLeft").get()
                .addOnSuccessListener { snapshot ->
                    val trials = snapshot.getValue(Int::class.java) ?: 3
                    if (trials > 0) validUids.add(uid)
                    processedCount++
                    if (processedCount == allUids.size) {
                        // Return the filtered list (only those with trials >0)
                        callback(validUids)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Failed to fetch trials for $uid: ${e.message}")
                    processedCount++
                    if (processedCount == allUids.size) {
                        callback(validUids)  // Still proceed with what's valid
                    }
                }
        }
    }

    // Update decrementTrialForParticipants to accept a pre-filtered list
    private fun decrementTrialForParticipants(participantUids: List<String>) {
        participantUids.forEach { uid ->
            val userTrialRef = userRef.child(uid).child("trialSessionsLeft")
            userTrialRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentTrials = currentData.getValue(Int::class.java) ?: 3
                    return if (currentTrials > 0) {
                        currentData.value = currentTrials - 1
                        Transaction.success(currentData)
                    } else {
                        Transaction.abort()  // Shouldn't happen since we pre-filtered, but safety check
                    }
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("HomeViewModel", "Failed to deduct trial for $uid: ${error.message}")
                    } else if (committed) {
                        val remaining = currentData?.getValue(Int::class.java) ?: 0
                        Log.d("HomeViewModel", "Deducted trial for $uid. Remaining: $remaining")
                        if (uid == currentFirebaseUser?.uid) {
                            trialSessionsLeft = remaining  // Update local state for current user
                        }
                    } else {
                        Log.w(
                            "HomeViewModel",
                            "Unexpected: Deduction aborted for $uid (shouldn't happen)"
                        )
                    }
                }
            })
        }
    }


    // Update onShowBottomSheet to use the new logic
    fun onShowBottomSheet(startRide: Boolean = false) {
        if (!startRide) {
            showBottomSheet = !showBottomSheet
            return
        }

        if (!canStartSession) {
            _toastMessage.value = sessionCheckMessage ?: "Cannot start session due to trial issues."
            return
        }

        // Proceed with the rest of your existing logic (getValidParticipantsWithTrials, etc.)
        val currentUid = currentFirebaseUser?.uid ?: return
        val memberUids = selectedBikers.map { it.uid }
        if (fromLatLng == null || toLatLng == null) return

        getValidParticipantsWithTrials(currentUid, memberUids) { validUids ->
            if (!validUids.contains(currentUid)) {
                _toastMessage.value = "Cannot start session: You have no trial sessions left."
                return@getValidParticipantsWithTrials
            }

            val finalMembers = validUids.filter { it != currentUid }
            val allParticipants = validUids

            val uuid = UUID.randomUUID()
            val members = allParticipants.map { Ride(it) }
            val session = Session(
                sessionId = uuid.toString(),
                active = true,
                sessionOwner = currentUid,
                startPosition = Place(
                    name = fromQuery,
                    latitude = fromLatLng!!.latitude,
                    longitude = fromLatLng!!.longitude
                ),
                destinationPosition = Place(
                    name = toQuery,
                    latitude = toLatLng!!.latitude,
                    longitude = toLatLng!!.longitude
                ),
                members = members
            )

            sessionRef.child(uuid.toString()).setValue(session)
                .addOnSuccessListener {
                    decrementTrialForParticipants(allParticipants)
                    viewModelScope.launch { preferencesManager.setActiveSession(uuid.toString()) }
                    fromQuery = ""
                    toQuery = ""
                    selectedBikers.clear()
                    checkIfMember()
                    _toastMessage.value = "Session started successfully!"
                }
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Failed to create session: ${e.message}")
                    _toastMessage.value = "Failed to start session. Please try again."
                }
        }

        showBottomSheet = !showBottomSheet
    }

    private fun calculateDistanceInKm(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        val distanceInMeters = results[0]
        return (distanceInMeters / 1000.0) // convert to km
    }


    fun onStopSession() {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = formatter.format(calendar.time)
        val sessionsRef = FirebaseUtils.database.getReference(TableConstants.SESSION)

        activeSession?.let { session ->
            val start = session.startPosition
            val end = session.destinationPosition

            // Calculate accurate distance
            val accurateDistance = if (start != null && end != null) {
                calculateDistanceInKm(
                    start.latitude, start.longitude,
                    end.latitude, end.longitude
                )
            } else 0.0

            // Update Firebase session as inactive
            sessionsRef.child(session.sessionId)
                .setValue(
                    session.copy(
                        active = false,
                        sessionDate = formattedDate,
                        sessionOwnerSpeed = speed
                    )
                )
                .addOnSuccessListener {
                    val creatorUid = currentFirebaseUser?.uid ?: return@addOnSuccessListener

                    // Use all actual session members (not selectedBikers)
                    val participants = mutableListOf<User>()
                    session.members.forEach { ride ->
                        participants.add(User(uid = ride.uid))
                    }

                    // Save personalized ride history for everyone
                    saveRideHistory(
                        speed = speed,
                        distance = accurateDistance,
                        timeInSeconds = elapsedTime,
                        creatorUid = creatorUid,
                        selectedBikers = participants,
                        activeSession = activeSession
                    )

                    // Reset all session-related states
                    isActiveSession = false
                    directionPolyLine = emptyList()
                    originalPolylinePoints = emptyList()
                    distance = 0.0
                    speed = 0.0
                    elapsedTime = 0
                    PolyLineUtils.setDirectionPolyLines(emptyList())

                    viewModelScope.launch {
                        preferencesManager.setActiveSession("")
                        delay(500L)  // Small delay to ensure Firebase updates propagate
                        getUserData()  // Refresh current user's trial count
                        Log.d("HomeViewModel", "Starting manual fetch for tempBikers after stop")
                        userRef.get().addOnSuccessListener { snapshot ->
                            val users =
                                snapshot.children.mapNotNull { it.getValue(User::class.java) }
                            Log.d("HomeViewModel", "Fetched ${users.size} users from Firebase")
                            currentUser?.let { thisUser ->
                                tempBikers = users.filter { user ->
                                    user.uid != thisUser.uid && thisUser.friends.containsKey(user.uid)
                                }.map { user ->
                                    Log.d(
                                        "HomeViewModel",
                                        "Processing friend ${user.username}: trials = ${user.trialSessionsLeft}"
                                    )
                                    if (user.username.isEmpty()) user.copy(username = "Unknown") else user
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("HomeViewModel", "Failed to refresh tempBikers: ${e.message}")
                        }
                    }

                    // Keep only current user after session stops
                    bikers = bikers.filter { it.uid == FirebaseUtils.auth.uid }
                    membersListener?.let { userRef.removeEventListener(it) }
                    membersListener = null
                    tempBikersListener?.let { userRef.removeEventListener(it) }
                    tempBikersListener = null
                }
        }
    }


    fun saveRideHistory(
        speed: Double,
        distance: Double,
        timeInSeconds: Long,
        creatorUid: String,
        selectedBikers: List<User>,
        activeSession: Session?
    ) {

        val db = FirebaseUtils.database.reference
        val usersRef = db.child("users")
        val rideId = db.child("rides").push().key ?: return
        val startPos = activeSession?.startPosition
        val endPos = activeSession?.destinationPosition
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timestamp = System.currentTimeMillis()

        // Shared ride info
        val sharedData = mapOf(
            "rideId" to rideId,
            "sessionDate" to date,
            "timestamp" to timestamp,
            "startPosition" to startPos,
            "destinationPosition" to endPos
        )

        // Combine creator + bikers
        val allParticipants = selectedBikers.map { it.uid }.toMutableList().apply {
            if (!contains(creatorUid)) add(creatorUid)
        }

        // Get all usernames first
        val participantNames = mutableMapOf<String, String>()
        var processedCount = 0

        allParticipants.forEach { uid ->
            usersRef.child(uid).get().addOnSuccessListener { snapshot ->
                val name = snapshot.child("username").getValue(String::class.java) ?: "No Name"
                participantNames[uid] = name
                processedCount++

                if (processedCount == allParticipants.size) {
                    val friendNames = participantNames.values.toList()

                    // NEW: Fetch speeds for all participants
                    val participantSpeeds = mutableMapOf<String, Double>()
                    var speedFetchCount = 0

                    allParticipants.forEach { pUid ->
                        usersRef.child(pUid).child("currentSpeed").get()
                            .addOnSuccessListener { speedSnapshot ->
                                val fetchedSpeed = speedSnapshot.getValue(Double::class.java) ?: 0.0
                                participantSpeeds[pUid] = fetchedSpeed
                                speedFetchCount++
                                if (speedFetchCount == allParticipants.size) {
                                    // Proceed to save with fetched speeds
                                    saveWithSpeeds(
                                        participantSpeeds,
                                        allParticipants,
                                        friendNames,
                                        distance,
                                        timeInSeconds,
                                        date,
                                        timestamp,
                                        startPos,
                                        endPos,
                                        db,
                                        rideId,
                                        sharedData,
                                        participantNames
                                    )
                                }
                            }
                            .addOnFailureListener {
                                participantSpeeds[pUid] = 0.0  // Default on failure
                                speedFetchCount++
                                if (speedFetchCount == allParticipants.size) {
                                    saveWithSpeeds(
                                        participantSpeeds,
                                        allParticipants,
                                        friendNames,
                                        distance,
                                        timeInSeconds,
                                        date,
                                        timestamp,
                                        startPos,
                                        endPos,
                                        db,
                                        rideId,
                                        sharedData,
                                        participantNames
                                    )
                                }
                            }
                    }
                }
            }
        }
    }

    // NEW: Helper to save with speeds
    private fun saveWithSpeeds(
        participantSpeeds: Map<String, Double>,
        allParticipants: List<String>,
        friendNames: List<String>,
        distance: Double,
        timeInSeconds: Long,
        date: String,
        timestamp: Long,
        startPos: Place?,
        endPos: Place?,
        db: DatabaseReference,
        rideId: String,
        sharedData: Map<String, Any?>,
        participantNames: Map<String, String>
    ) {
        allParticipants.forEach { pUid ->
            val userName = participantNames[pUid] ?: "Unknown"
            val timeFormatted = formatRideTime(timeInSeconds)
            val participantSpeed = participantSpeeds[pUid] ?: 0.0
            val etaMinutes =
                if (participantSpeed > 0) ((distance / participantSpeed) * 60).toInt() else 0
            val eta = if (etaMinutes > 0) "$etaMinutes min" else "N/A"
            val ride = Ride(
                uid = pUid,
                username = userName,
                speed = participantSpeed,  // Now personalized from Firebase
                distance = distance,
                time = formatElapsedTime(timeInSeconds),
                sessionDate = date,
                timestamp = timestamp,
                friends = friendNames.filter { it != userName },
                eta = eta,  // Now based on participant's speed
                startPosition = startPos,
                destinationPosition = endPos
            )
            db.child("rides").child(pUid).child(rideId)
                .setValue(ride)
                .addOnSuccessListener {
                    db.child("shared_rides").child(rideId).setValue(sharedData)
                }
        }
    }

    // Helper to format time
    private fun formatRideTime(timeInSeconds: Long): String {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60
        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }


    fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a =
            sin(dLat / 2).pow(2) + cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) * sin(
                dLng / 2
            ).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    // Call this every time you get a new user location update
    fun updatePolylineTracks(currentLocation: LatLng) {
        if (directionPolyLine.isEmpty()) return

        println("Update direction polyline")

        // Use PolyUtil to find the closest point on the original path
        // The 'true' parameter indicates a geodesic calculation, which is more accurate.
        val snappedPointIndex = PolyUtil.locationIndexOnPath(
            currentLocation,
            originalPolylinePoints,
            true, // Geodesic
            20.0 // Tolerance in meters (e.g., 50m)
        )

        // If locationIndexOnPath returns -1, the user is too far from the path.
        if (snappedPointIndex == -1) {
            // Optional: Handle case where user is off-route.
            // You might do nothing, or show the full route again.
            return
        }

        // Optimization: Only search from the last known segment to avoid re-checking the start
        if (snappedPointIndex < lastKnownSegmentIndex) {
            // The user has likely not moved backward significantly, so we ignore this update
            // to prevent the polyline from "growing back".
            return
        }
        lastKnownSegmentIndex = snappedPointIndex

        // The 'snappedPointIndex' is the index of the *first* point of the segment
        // the user is currently on.

        // Get the segment points
        val startOfSegment = originalPolylinePoints[snappedPointIndex]

        // For a more accurate "snapped" point (optional but recommended):
        // You'd typically find the exact projection of the user's location onto the segment.
        // For simplicity here, we'll just start the new polyline from the beginning of the segment.
        // To be more precise, you would calculate the projected point and insert it at the start.

        // Create the new list for the visible polyline
        // It starts from the beginning of the current segment and includes all remaining points.
        val remainingPath =
            originalPolylinePoints.subList(snappedPointIndex, originalPolylinePoints.size)

        // Update the state for the Composable to recompose
        directionPolyLine = remainingPath

        // --- Calculate Traveled Distance (Bonus) ---
        // Calculate the distance of the part of the route that has been "removed"
//        val traveledPath = originalPolylinePoints.subList(0, snappedPointIndex + 1)
//        traveledDistance.value = PolyUtil.computeLength(traveledPath)
    }

    private fun checkIfMember() {
        sessionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessionList = snapshot.children.mapNotNull {
                    it.getValue(Session::class.java)
                }

                // Handle inactive session for current sessionId
                if (sessionId.isNotEmpty()) {
                    sessionList.find { it.sessionId == sessionId }?.let { session ->
                        if (!session.active) {
                            sessionId = ""
                            isAsMemberActive = false
                            directionPolyLine = emptyList()
                            PolyLineUtils.setDirectionPolyLines(emptyList())
                            bikers = bikers.filter { biker -> biker.uid == currentUser?.uid }
                            // NEW: Remove the nested listener to prevent re-fires
                            tempBikersListener ?.let { userRef.removeEventListener(it) }
                            tempBikersListener = null
                        }
                    }
                }

                // Find active session where current user is a member (not owner)
                val activeSession = sessionList.find { session ->
                    session.active && session.sessionOwner != FirebaseUtils.auth.uid &&
                            session.members.any { it.uid == FirebaseUtils.auth.uid }
                }

                if (activeSession != null && !isAsMemberActive) {
                    isAsMemberActive = true
                    sessionId = activeSession.sessionId

                    // Load only session members (now includes creator)
                    tempBikersListener ?.let { userRef.removeEventListener(it) }  // Remove old
                    tempBikersListener  = object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val users = userSnapshot.children.mapNotNull { it.getValue(User::class.java) }
                            val sessionMemberUids = activeSession.members.map { it.uid }.toSet()
                            bikers = users.filter { user ->
                                sessionMemberUids.contains(user.uid)
                            }.map { user ->
                                if (user.username.isEmpty()) user.copy(username = "Unknown") else user
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e(this::class.toString(), "Database Error: ${error.message}")
                        }
                    }
                    userRef.addValueEventListener(tempBikersListener !!)

                    // Load route if needed
                    if (directionPolyLine.isEmpty()) {
                        activeSession.startPosition?.let { start ->
                            activeSession.destinationPosition?.let { dest ->
                                tryGetRoute(LatLng(start.latitude, start.longitude), LatLng(dest.latitude, dest.longitude))
                            }
                        }
                    }
                } else if (activeSession == null) {
                    // Reset if no longer a member of any active session
                    isAsMemberActive = false
                    sessionId = ""
                    bikers = bikers.filter { it.uid == currentUser?.uid }  // Show only self
                    // NEW: Remove listener here too
                    tempBikersListener ?.let { userRef.removeEventListener(it) }
                    tempBikersListener  = null
                    loadTempBikers()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(this::class.toString(), "Database Error: ${error.message}")
            }
        })
    }
}
