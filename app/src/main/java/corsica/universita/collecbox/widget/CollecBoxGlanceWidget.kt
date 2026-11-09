package corsica.universita.collecbox.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import corsica.universita.collecbox.CollecBoxApplication
import corsica.universita.collecbox.MainActivity
import kotlinx.coroutines.flow.firstOrNull

/**
 * Widget d'accueil interactif propulsé par Jetpack Glance (Fonctionnalité Native 1).
 * Ouvre directement le formulaire d'ajout d'objet sur l'application.
 */
class CollecBoxGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CollecBoxApplication
        val totalCount = app?.repository?.allItems?.firstOrNull()?.size ?: 3
        val lastItem = app?.repository?.allItems?.firstOrNull()?.firstOrNull()

        // Paramètre envoyé à MainActivity pour ouvrir directement le formulaire d'ajout
        val openAddKey = ActionParameters.Key<Boolean>("openAdd")
        val openAddAction = actionStartActivity<MainActivity>(actionParametersOf(openAddKey to true))

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                        .clickable(openAddAction)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "📦 CollecBox",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFF818CF8)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "$totalCount objets",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color(0xFFF1F5F9)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        Text(
                            text = if (lastItem != null) "Dernier : ${lastItem.title}" else "Collection prête",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // Bouton d'action directe pour ouvrir l'ajout / scan
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(Color(0xFF6366F1))
                                .padding(vertical = 8.dp)
                                .clickable(openAddAction),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Scanner / Ajouter un objet",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color.White),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
