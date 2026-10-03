package net.spritzlive.identifysong.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import net.spritzlive.identifysong.MainActivity
import net.spritzlive.identifysong.data.HistoryEntry
import net.spritzlive.identifysong.data.HistoryRepository

private val AutoIdentifyKey = ActionParameters.Key<Boolean>(MainActivity.EXTRA_AUTO_IDENTIFY)

class IdentifySongWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lastEntry = HistoryRepository(context).history.first().firstOrNull()

        provideContent {
            WidgetContent(lastEntry)
        }
    }
}

@Composable
private fun WidgetContent(lastEntry: HistoryEntry?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1B1033)))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(AutoIdentifyKey to true))),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "🎵 Identify Song",
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            if (lastEntry != null) {
                Text(
                    text = lastEntry.title,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp),
                    maxLines = 1
                )
                Text(
                    text = lastEntry.artist,
                    style = TextStyle(color = ColorProvider(Color(0xFFD8CCFF)), fontSize = 12.sp),
                    maxLines = 1
                )
            } else {
                Text(
                    text = "Tap to identify what's playing",
                    style = TextStyle(color = ColorProvider(Color(0xFFD8CCFF)), fontSize = 12.sp)
                )
            }
        }
    }
}

class IdentifySongWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IdentifySongWidget()
}
