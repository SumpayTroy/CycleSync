package com.bsit.cyclesync.ui.home

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object PayMongoService {
    private const val SECRET_KEY = "sk_test_Uv674yiJo1kQ3LLD66QbTma4"
    private const val API_URL = "https://api.paymongo.com/v1/checkout_sessions"

    fun createCheckoutSession(
        itemName: String?,
        amountCentavos: Int,
        successUrl: String?,
        cancelUrl: String?,
        callback: CheckoutCallback
    ) {
        Thread {
            try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty(
                    "Authorization",
                    "Basic " + Base64.encodeToString((SECRET_KEY + ":").toByteArray(), Base64.NO_WRAP)
                )
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val attributes = JSONObject().apply {
                    put("cancel_url", cancelUrl)
                    put("success_url", successUrl)
                    put("payment_method_types", JSONArray().put("gcash"))
                    put("line_items", JSONArray().put(
                        JSONObject().apply {
                            put("name", itemName)
                            put("amount", amountCentavos)
                            put("currency", "PHP")
                            put("quantity", 1)
                        }
                    ))
                }

                val root = JSONObject().put("data", JSONObject().put("attributes", attributes))

                conn.outputStream.use { it.write(root.toString().toByteArray()) }

                val responseCode = conn.responseCode
                val reader = BufferedReader(
                    InputStreamReader(
                        if (responseCode in 200..299) conn.inputStream else conn.errorStream
                    )
                )

                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) response.append(line)

                if (responseCode in 200..299) {
                    val jsonResponse = JSONObject(response.toString())
                    val checkoutUrl = jsonResponse
                        .getJSONObject("data")
                        .getJSONObject("attributes")
                        .getString("checkout_url")
                    Handler(Looper.getMainLooper()).post { callback.onSuccess(checkoutUrl) }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("HTTP $responseCode: $response")
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback.onError(e.message) }
            }
        }.start()
    }

    interface CheckoutCallback {
        fun onSuccess(checkoutUrl: String?)
        fun onError(errorMessage: String?)
    }
}
