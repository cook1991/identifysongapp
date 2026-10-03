package net.spritzlive.identifysong.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

/** Full-width square preview of an artwork image; tap anywhere to dismiss. */
@Composable
fun EnlargedArtworkDialog(artworkUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = "Album artwork, enlarged",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onDismiss)
        )
    }
}
