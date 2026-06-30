package com.bsit.cyclesync.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.bsit.cyclesync.model.response.Prediction


@Composable
fun PlaceSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    predictions: List<Prediction>,
    onPredictionClick: (Prediction) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value) {
        predictions.filter {
            val keywords = it.description.split(" ")
            keywords.all { keyword ->
                it.description.contains(keyword, ignoreCase = true)
            }
        }
    }

    Column {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotBlank() && filteredSuggestions.isNotEmpty()
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }
                .focusRequester(focusRequester),
        )
        // Only show LazyColumn when focused and there are matches
        if (isFocused && value.isNotBlank() && filteredSuggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                itemsIndexed(items = filteredSuggestions,
                    key = { pos, place ->
                        place.place_id
                    }) { index, place ->
                    Text(
                        text = place.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueChange(place.description)
                                onPredictionClick(place)
                                focusManager.clearFocus()
                            }
                    )
                }
            }
        }
    }
}