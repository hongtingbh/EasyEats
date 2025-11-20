package com.example.easyeats

data class MenuItem(
    val id: Int,
    val restaurantId: Int, // Links to the parent restaurant
    val name: String,
    val description: String,
    val price: Double
)

val DOMINOS_MENU = listOf(
    MenuItem(1, 101, "Pepperoni Pizza", "Classic pepperoni, mozzarella, and tomato sauce.", 18.99),
    MenuItem(2, 101, "Veggie Supreme", "Mushrooms, onions, peppers, and olives.", 21.50),
    MenuItem(3, 101, "Garlic Bread", "Four slices of fresh, toasted garlic bread.", 5.99)
)