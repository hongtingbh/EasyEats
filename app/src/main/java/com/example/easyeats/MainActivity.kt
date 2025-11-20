package com.example.easyeats

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.easyeats.data.UserDatabaseHelper
import com.example.easyeats.ui.LoginScreen
import com.example.easyeats.ui.SignUpScreen
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class MainActivity : ComponentActivity() {

    // Declare ShakeDetector as a nullable member variable
    // It will be initialized when the RestaurantListScreen first composes.
    private var shakeDetector: ShakeDetector? = null

    //For debugging/logging
    private val TAG = "MyActivityTag"
    private val client = OkHttpClient()

    // keep your working API key
    private val apiKey = "AIzaSyB9CsEAyTV9wASEa7BRHV9ODKC80WmucqQ"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = UserDatabaseHelper(this)
        dbHelper.writableDatabase   // ensures DB is created

        setContent {
            EasyEatsApp()
        }
    }
    // Control the sensor using Activity lifecycle
    override fun onResume() {
        super.onResume()
        // If the detector exists, restart listening when the Activity resumes (e.g., returning from MapActivity)
        shakeDetector?.startListening()
    }

    override fun onPause() {
        super.onPause()
        // Stop listening when the Activity is paused (e.g., MapActivity comes into the foreground)
        shakeDetector?.stopListening()
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
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

        // Get the current context
        val activity = LocalContext.current as MainActivity
        val context = LocalContext.current
        // Create the shake detector to clear the search, passing the action to clear the state
        val onClearSearch: () -> Unit = {
            query = TextFieldValue("")
            Toast.makeText(context, "Search Cleared!", Toast.LENGTH_SHORT).show()
        }
        //INTEGRATE SHAKE DETECTION WITH COMPOSE LIFECYCLE
        // Initialize the ShakeDetector ONCE and assign it to the Activity
        DisposableEffect(Unit) {
            if (activity.shakeDetector == null) {
                activity.shakeDetector = ShakeDetector(context, onClearSearch)
            }

            // This startListening call is technically optional here due to onResume(), but keeps it simple
            activity.shakeDetector?.startListening()

            onDispose {
                // onPause() in the Activity handles it
                // when the Activity is hidden by another Activity (like MapActivity).
            }
        }

        // 3. Define the action function to launch the map activity (This replaces your existing Card logic)
        val onLaunchMap: (Restaurant) -> Unit = { restaurant ->
            // When this function runs:
            // A. The Activity's onPause() will soon be called, stopping the sensor.
            // B. We manually execute the original Activity launch logic here.
            val intent = Intent(context, MapActivity::class.java)
            Log.d(TAG, "Launching Map for: $restaurant")
            intent.putExtra("restaurant", restaurant)
            context.startActivity(intent)
            // Sensor is stopped by the onPause override in MainActivity (See Step 2)
        }


        Scaffold(
            topBar = { SimpleTopBar("EasyEats") }
        ) { padding ->
            Column(modifier = Modifier
                .padding(padding)
                .padding(16.dp)) {
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
                        RestaurantCard(restaurant, onLaunchMap)
                    }
                }
            }
        }
    }

    @Composable
    fun RestaurantCard(restaurant: Restaurant, onLaunchMap: (Restaurant) -> Unit) {
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
                        Text(text = restaurant.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = "⭐ ${restaurant.rating ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = restaurant.address, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    // Call the function passed from the parent Composable
                    onLaunchMap(restaurant)
                }) {
                    Text("View on Map")
                }
            }
        }
    }

    // ----------------------------
    // GOOGLE PLACES SEARCH
    // ----------------------------
    @SuppressLint("SuspiciousIndentation")
    private fun searchRestaurants(query: String, onResult: (List<Restaurant>) -> Unit) {
        Log.d(TAG, "1. Search button registered and searchRestaurants function called.")
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
            .post(RequestBody.create("application/json".toMediaType(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { onResult(emptyList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "2. searchRestaurants → onResponse")
                val body = response.body?.string() ?: return
                val json = JsonParser.parseString(body).asJsonObject
                val places = json.getAsJsonArray("places") ?: return

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

                    Restaurant(name, rating, photoUrl, address, lat, lng)
                }

                runOnUiThread { onResult(results) }
            }
        })
    }
}