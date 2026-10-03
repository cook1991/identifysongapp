package net.spritzlive.identifysong.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.spritzlive.identifysong.data.HistoryEntry

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear history")
                }
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No identifications yet — tap to identify a song and it'll show up here.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = {
                            entry.songLink?.let { link ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all ${history.size} saved identifications. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (entry.artworkUrl != null) {
            AsyncImage(
                model = entry.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = entry.artist, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = DateUtils.getRelativeTimeSpanString(
                entry.identifiedAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString(),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
