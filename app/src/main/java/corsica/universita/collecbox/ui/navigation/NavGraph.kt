package corsica.universita.collecbox.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Add : Screen("add")
    object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: Long) = "detail/$itemId"
    }
}

