package com.example.easyeats

import android.annotation.SuppressLint
import android.util.Log


import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.easyeats.ui.LoginScreen
import com.example.easyeats.ui.SignUpScreen
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException


class MainActivity : ComponentActivity() {
    private val TAG = "MyActivityTag" // Or the name of your class
    //User location request code
    private val LOCATION_REQUEST_CODE = 1001



    private val client = OkHttpClient()
    private val apiKey = "AIzaSyB9CsEAyTV9wASEa7BRHV9ODKC80WmucqQ"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EasyEatsApp()
        }
    }

    @Composable
    fun EasyEatsApp() {
        var currentScreen by remember { mutableStateOf("login") }

        when (currentScreen) {
            "login" -> LoginScreen(
                onLoginSuccess = { currentScreen = "main" },
                onNavigateToSignUp = { currentScreen = "signup" }
            )
            "signup" -> SignUpScreen(onSignUpSuccess = { currentScreen = "login" })
            "main" -> RestaurantListScreen()
        }
    }

    @Composable
    fun SimpleTopBar(title: String) {
        Surface(
            color = MaterialTheme.colorScheme.primary,   // background color
            tonalElevation = 4.dp                        // adds soft shadow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),                    // inner spacing
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    fun RestaurantListScreen() {
        var query by remember { mutableStateOf(TextFieldValue("")) }
        var restaurants by remember { mutableStateOf(listOf<Restaurant>()) }

        Scaffold(
            topBar = { SimpleTopBar("EasyEats") }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search restaurants...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { searchRestaurants(query.text) { restaurants = it } }) {
                    Text("Search")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(restaurants) { restaurant ->
                        RestaurantCard(restaurant)
                    }
                }
            }
        }
    }

    @Composable
    fun RestaurantCard(restaurant: Restaurant) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = restaurant.photoUrl ?: R.drawable.ic_launcher_foreground
                        ),
                        contentDescription = restaurant.name,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "⭐ ${restaurant.rating ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = restaurant.address,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val context = this@MainActivity
                    val intent = Intent(context, MapActivity::class.java)
                    Log.d(TAG, "7: $restaurant");
                    intent.putExtra("restaurant", restaurant)
                    context.startActivity(intent)
                }) {
                    Text("View on Map")
                }
            }
        }
    }


//    private fun requestLocationPermission() {
//        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) {
//
//            requestPermissions(
//                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_REQUEST_CODE
//            )
//        } else {
//            getUserLocation()
//        }
//    }
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == LOCATION_REQUEST_CODE &&
//            grantResults.isNotEmpty() &&
//            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//            getUserLocation()
//        }
//    }


    @SuppressLint("SuspiciousIndentation")
    private fun searchRestaurants(query: String, onResult: (List<Restaurant>) -> Unit) {
        Log.d(TAG, "1. Search button registered and searchRestaurants function called.");
        val url = "https://places.googleapis.com/v1/places:searchText"
        val jsonBody = """
        {
          "textQuery": "$query restaurant",
          "maxResultCount": 10
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "places.displayName,places.rating,places.photos,places.formattedAddress,places.location")
            //.addHeader("X-Goog-FieldMask", "places.displayName,places.rating,places.photos")
            .post(RequestBody.create("application/json".toMediaType(), jsonBody))
            .build()
            Log.d(TAG, "6.:, $request");

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { onResult(emptyList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "2. searchRestaurants calls onResponse --> Entered onResponse");
                val body = response.body?.string() ?: return
                val json = JsonParser.parseString(body).asJsonObject
                val places = json.getAsJsonArray("places") ?: return

                Log.d(TAG, "3., $places");
                val results = places.map { element ->
                    val obj = element.asJsonObject
                    val name = obj["displayName"].asJsonObject["text"].asString
                    val rating = if (obj.has("rating")) obj["rating"].asDouble else null
                    val photoRef = obj["photos"]?.asJsonArray?.firstOrNull()
                        ?.asJsonObject?.get("name")?.asString
                    val photoUrl = photoRef?.let {
                        "https://places.googleapis.com/v1/$it/media?maxWidthPx=400&key=$apiKey"
                    }
                    val address = obj["formattedAddress"]?.asString ?: "Address not available"
                    val lat = obj["location"]?.asJsonObject?.get("latitude")?.asDouble ?: 43.7
                    val lng = obj["location"]?.asJsonObject?.get("longitude")?.asDouble ?: -79.4
                    val distanceFromMe = obj["addressDescriptor"]?.asJsonObject?.get("addressDescriptor")?.asDouble ?: -79.4
                    Restaurant(name, rating, photoUrl, address, lat, lng)

                }
                Log.d(TAG, "4");
                runOnUiThread {
                    onResult(results)
                }
                Log.d(TAG, "5");
            }
        })
    }
}