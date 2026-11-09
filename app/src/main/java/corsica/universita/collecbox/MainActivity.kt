package corsica.universita.collecbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import corsica.universita.collecbox.ui.navigation.Screen
import corsica.universita.collecbox.ui.screens.add.AddItemScreen
import corsica.universita.collecbox.ui.screens.add.AddViewModel
import corsica.universita.collecbox.ui.screens.detail.ItemDetailScreen
import corsica.universita.collecbox.ui.screens.home.HomeScreen
import corsica.universita.collecbox.ui.screens.home.HomeViewModel
import corsica.universita.collecbox.ui.theme.CollecBoxTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // Route de navigation ciblée par un Intent (Widget, Notification FCM ou App Link)
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CollecBoxApplication
        app.analyticsTracker.logAppOpen()

        handleIntent(intent)

        setContent {
            CollecBoxTheme {
                val navController = rememberNavController()

                // Écoute des demandes de navigation provoquées par les Intents système
                val routeToNavigate by pendingRoute.collectAsStateWithLifecycle()
                LaunchedEffect(routeToNavigate) {
                    routeToNavigate?.let { route ->
                        navController.navigate(route)
                        pendingRoute.value = null
                    }
                }

                // Permission runtime pour POST_NOTIFICATIONS (Android 13+ / API 33)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.d("MainActivity", "Permission notifications accordée : $isGranted")
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    // Écran d'accueil (Liste + Filtres + État hors-ligne)
                    composable(Screen.Home.route) {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(
                                repository = app.repository,
                                preferencesRepository = app.userPreferencesRepository,
                                analyticsTracker = app.analyticsTracker
                            )
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToAdd = { navController.navigate(Screen.Add.route) },
                            onNavigateToDetail = { id ->
                                navController.navigate(Screen.Detail.createRoute(id))
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }

                    // Écran d'ajout avec scanner de codes-barres ML Kit
                    composable(Screen.Add.route) {
                        val addViewModel: AddViewModel = viewModel(
                            factory = AddViewModel.Factory(
                                repository = app.repository,
                                analyticsTracker = app.analyticsTracker
                            )
                        )
                        AddItemScreen(
                            viewModel = addViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onItemAdded = { newItemId ->
                                navController.popBackStack()
                                navController.navigate(Screen.Detail.createRoute(newItemId))
                            }
                        )
                    }

                    // Écran de détail
                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                        ItemDetailScreen(
                            itemId = itemId,
                            repository = app.repository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    /**
     * Gère les nouveaux intents quand l'activité est déjà en mémoire (launchMode="singleTop")
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Clic sur le widget d'accueil Glance (ouvre directement le formulaire d'ajout)
        val hasOpenAdd = intent.getBooleanExtra("openAdd", false) ||
                intent.extras?.get("openAdd") == true ||
                intent.extras?.get("openAdd") == "true"
        if (hasOpenAdd) {
            pendingRoute.value = Screen.Add.route
            return
        }

        // 2. Clic sur une notification push FCM (ouvre la fiche correspondante)
        val pushItemId = intent.getLongExtra("itemId", -1L)
        if (pushItemId != -1L) {
            pendingRoute.value = Screen.Detail.createRoute(pushItemId)
            return
        }

        // 3. App Link (https://collecbox.netlify.app/item/X)
        val dataUri = intent.data
        if (dataUri != null && Intent.ACTION_VIEW == intent.action) {
            val pathSegments = dataUri.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == "item") {
                val id = pathSegments[1].toLongOrNull()
                if (id != null) {
                    pendingRoute.value = Screen.Detail.createRoute(id)
                }
            }
        }
    }
}
