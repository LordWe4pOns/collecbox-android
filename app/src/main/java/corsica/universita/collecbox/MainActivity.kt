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
import androidx.core.content.ContextCompat
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CollecBoxApplication

        setContent {
            CollecBoxTheme {
                val navController = rememberNavController()

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

                    // 1. Gestion de l'ouverture depuis un Widget d'accueil
                    if (intent?.getBooleanExtra("openAdd", false) == true) {
                        navController.navigate(Screen.Add.route)
                    }

                    // 2. Gestion du clic sur une notification FCM (PendingIntent avec itemId)
                    val pushItemId = intent?.getLongExtra("itemId", -1L) ?: -1L
                    if (pushItemId != -1L) {
                        navController.navigate(Screen.Detail.createRoute(pushItemId))
                    }

                    // 3. Gestion de l'App Link (URL PWA https://collecbox.netlify.app/item/X)
                    val dataUri = intent?.data
                    if (dataUri != null && Intent.ACTION_VIEW == intent.action) {
                        Log.d("MainActivity", "App Link reçu : $dataUri")
                        val pathSegments = dataUri.pathSegments
                        if (pathSegments.size >= 2 && pathSegments[0] == "item") {
                            val id = pathSegments[1].toLongOrNull()
                            if (id != null) {
                                navController.navigate(Screen.Detail.createRoute(id))
                            }
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

                    // Écran d'ajout avec caméra OCR ML Kit
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
}

