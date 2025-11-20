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
import androidx.compose.runtime.snapshots.SnapshotStateList
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


// Define the callback function type in your MainActivity
typealias NavigateToMenu = (Restaurant) -> Unit
typealias CartUpdateCallback = () -> Unit


// Inside MainActivity.kt or a dedicated file

const val AVG_DELIVERY_TIME_MINUTES = 30
const val DELIVERY_FEE = 5.00
const val TAX_RATE = 0.13

class MainActivity : ComponentActivity() {
    // Declare ShakeDetector as a nullable member variable
    // It will be initialized when the RestaurantListScreen first composes.
    var shakeDetector: ShakeDetector? = null


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

        // If the detector exists (i.e., the Composable has created it), start listening.
        // This handles returning from the MapActivity.
        shakeDetector?.startListening()
        Log.d("Fix", "onResume: Called startListening()")
    }

    override fun onPause() {
        super.onPause()
        // Stop listening when the Activity is paused (e.g., MapActivity comes into the foreground)
        shakeDetector?.stopListening()
        Log.d("Fix", "onPause: Called stopListening()")
    }

    @Composable
    fun EasyEatsApp() {
        var currentScreen by remember { mutableStateOf("login") }

        var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
        var currentCart by remember { mutableStateOf(emptyList<CartItem>()) }

        when (currentScreen) {
            "login" -> LoginScreen(
                onLoginSuccess = { currentScreen = "main" },
                onNavigateToSignUp = { currentScreen = "signup" }
            )
            "signup" -> SignUpScreen(onSignUpSuccess = { currentScreen = "login" })

            "main" -> RestaurantListScreen(
                onViewMenu = { restaurant ->
                    selectedRestaurant = restaurant
                    currentScreen = "menu"
                }
            )

            "menu" -> selectedRestaurant?.let {
                MenuScreen(
                    restaurant = it,
                    onNavigateToCheckout = { cart ->
                        currentCart = cart
                        currentScreen = "checkout"
                    }
                )
            } ?: run { currentScreen = "main" }

            "checkout" -> CheckoutScreen(
                finalCartItems = currentCart,
                // ✅ Added the callback to return to main
                onNavigateToMain = {
                    currentCart = emptyList() // Clear cart upon returning to main
                    currentScreen = "main"
                }
            )
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
    fun RestaurantListScreen(onViewMenu: NavigateToMenu) {
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
            // Creation: Create the detector and assign it to the Activity member
            val detector = ShakeDetector(context, onClearSearch)
            activity.shakeDetector = detector
            Log.d("Fix", "Composable: Created and assigned new ShakeDetector.")

            // Initial Start: Manually start the sensor on first composition
            detector.startListening()

            // Cleanup: When this Composable is DESTROYED (which happens if you navigate away from "main" screen),
            // we clean up resources.
            onDispose {
                detector.stopListening()
                detector.release() // Release the SoundPool resources
                activity.shakeDetector = null // Clear the Activity reference
                Log.d("Fix", "Composable: Released and unassigned detector.")
            }
        }

        // Define the action function to launch the map activity (This replaces your existing Card logic)
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
                        // ✅ FIX: Now pass all three required parameters to RestaurantCard
                        RestaurantCard(
                            restaurant = restaurant,
                            onLaunchMap = onLaunchMap,
                            onViewMenu = onViewMenu // <-- Passed the required function
                        )
                    }
                }
            }
        }
    }



    @Composable
    fun RestaurantCard(restaurant: Restaurant, onLaunchMap: (Restaurant) -> Unit, onViewMenu: NavigateToMenu) {
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
                    onViewMenu(restaurant) //Button action for viewing menu
                }) {
                    Text("View Menu")
                }

                Button(onClick = {
                    // Call the function passed from the parent Composable
                    onLaunchMap(restaurant)
                }) {
                    Text("View on Map")
                }

            }
        }
    }

    @Composable
    fun MenuScreen(restaurant: Restaurant, onNavigateToCheckout: (List<CartItem>) -> Unit) {
        // State to hold the current items in the cart (for the current session)
        val cartItems = remember { mutableStateListOf<CartItem>() }

        val menuSource = if (restaurant.name.contains("Dominos") || restaurant.name.contains("Pizza")) {
            DOMINOS_MENU
        } else {
            // Fallback for demo: show Dominos menu for any restaurant
            DOMINOS_MENU
        }
        // State variable to force recomposition when quantity changes
        var cartUpdateTrigger by remember { mutableStateOf(0) }

        // This callback simply changes the trigger state
        val onQuantityChangeCallback: CartUpdateCallback = {
            cartUpdateTrigger++
        }

        // Add a log to verify data loading (optional, but helpful for debugging)
        Log.d("MenuDebug", "Attempting to load menu for: ${restaurant.name}. Items loaded: ${menuSource.size}")

        // Calculate total items for the button text
        val totalItems = cartItems.sumOf { it.quantity }
        val subtotal = cartItems.sumOf { it.menuItem.price * it.quantity }

        // UI layout here (Scaffold, TopBar with restaurant name, LazyColumn for menuSource)
        Scaffold(
            topBar = { SimpleTopBar(restaurant.name) },
            bottomBar = {
                if (totalItems > 0) {
                    Surface(tonalElevation = 8.dp) {
                        Button(
                            onClick = { onNavigateToCheckout(cartItems.toList()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("View Cart (${totalItems} items) - $${"%.2f".format(subtotal)}")
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(menuSource) { item ->
                    MenuItemRow(
                        item = item,
                        cartItems = cartItems,
                        onQuantityChange = onQuantityChangeCallback
                    )
                }
            }
        }
    }


    @Composable
    fun MenuItemRow(item: MenuItem, cartItems: SnapshotStateList<CartItem>, onQuantityChange: CartUpdateCallback) {
        // Logic to find if the item is already in the cart
        var existingCartItem = cartItems.find { it.menuItem.id == item.id }
        var quantity by remember { mutableStateOf(existingCartItem?.quantity ?: 0) }

        // --- Main Row to hold details (left) and controls (right) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 1. Name
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                // 2. Description
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 3. Price
                Text(
                    text = "$${"%.2f".format(item.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT SIDE: QUANTITY CONTROLS
            Row(verticalAlignment = Alignment.CenterVertically) {
                //Decrease button
                Button(
                    onClick = {
                        if (quantity > 0) {
                            quantity--
                            // Update cart state
                            if (quantity == 0) {
                                cartItems.removeAll { it.menuItem.id == item.id }
                            } else {
                                existingCartItem?.quantity = quantity
                            }
                            onQuantityChange()
                        }
                    },
                    enabled = quantity > 0,
                    // Make buttons smaller for better fit
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("-") }

                Text(text = quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))

                //Increase button
                Button(
                    onClick = {
                        quantity++
                        // Update cart state: if not found, add new CartItem; otherwise, increment
                        if (existingCartItem == null) {
                            val newCartItem = CartItem(item, 1)
                            cartItems.add(newCartItem)
                            existingCartItem = newCartItem
                        }
                        else {
                            // Update the quantity AND force a list mutation.
                            existingCartItem!!.quantity = quantity

                            val index = cartItems.indexOf(existingCartItem!!)
                            if (index != -1) {
                                // Remove and re-add or just re-set the item at its position.
                                // Re-setting is the safest way to ensure observation.
                                cartItems[index] = existingCartItem!!
                            }
                        }
                        onQuantityChange()
                    },
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+") }
            }
        }
        // Add a divider for separation
        Divider(modifier = Modifier.padding(top = 8.dp))
    }



    @Composable
    fun CheckoutScreen(finalCartItems: List<CartItem>, onNavigateToMain: () -> Unit) {
        // State for mock user inputs
        var address by remember { mutableStateOf(DeliveryAddress()) }
        var paymentInfo by remember { mutableStateOf("") }
        var orderPlaced by remember { mutableStateOf(false) }

        // Calculation properties
        val subtotal = finalCartItems.sumOf { it.menuItem.price * it.quantity }
        val tax = subtotal * TAX_RATE
        val total = subtotal + tax + DELIVERY_FEE

        Scaffold(
            topBar = { SimpleTopBar("Checkout") }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // --- Order Summary ---
                Text("Order Summary", style = MaterialTheme.typography.titleLarge)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(finalCartItems) { cartItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${cartItem.menuItem.name} (x${cartItem.quantity})")
                            Text("$${"%.2f".format(cartItem.menuItem.price * cartItem.quantity)}")
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                    }

                    item {
                        // --- Totals ---
                        Text("Subtotal: $${"%.2f".format(subtotal)}")
                        Text("Tax (${(TAX_RATE * 100).toInt()}%): $${"%.2f".format(tax)}")
                        Text("Delivery Fee: $${"%.2f".format(DELIVERY_FEE)}")
                        Spacer(Modifier.height(4.dp))
                        Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- Input Fields ---
                    item {
                        Text("Delivery Details", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = address.street,
                            onValueChange = { address = address.copy(street = it) },
                            label = { Text("Street Address") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = paymentInfo,
                            onValueChange = { paymentInfo = it },
                            label = { Text("Payment Info (Mock Card No.)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }

                // --- Place Order Button and Confirmation ---
                if (orderPlaced) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 Order Placed Successfully! 🎉", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("Estimated Delivery Time: **${AVG_DELIVERY_TIME_MINUTES} minutes**.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onNavigateToMain) {
                            Text("Return to Main Menu")
                        }
                    }
                } else {
                    Button(
                        onClick = { orderPlaced = true },
                        enabled = address.street.isNotBlank() && paymentInfo.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Place Order ($${"%.2f".format(total)})")
                    }
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