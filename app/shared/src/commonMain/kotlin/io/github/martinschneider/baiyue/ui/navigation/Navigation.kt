package io.github.martinschneider.baiyue.ui.navigation

enum class Screen(val route: String, val label: String) {
    Map("map", "Map"),
    List("list", "List"),
    Stats("stats", "Stats"),
    Settings("settings", "Settings")
}
