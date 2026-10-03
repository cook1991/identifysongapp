package net.spritzlive.identifysong

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import net.spritzlive.identifysong.network.SongMatch
import net.spritzlive.identifysong.ui.theme.IdentifySongTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RecognitionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdentifySongTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    IdentifyScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun IdentifyScreen(viewModel: RecognitionViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.identify(context)
        } else {
            permissionDenied = true
        }
    }

    fun startListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            permissionDenied = false
            viewModel.identify(context)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Identify Song",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap the button while music is playing",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            textAlign = TextAlign.Center
        )

        AnimatedContent(targetState = state, label = "recognition-state") { current ->
            when (current) {
                is RecognitionState.Idle -> IdentifyButton(
                    label = "Tap to identify",
                    pulsing = false,
                    onClick = ::startListening
                )

                is RecognitionState.Listening -> IdentifyButton(
                    label = if (current.maxAttempts > 1) {
                        "Listening… (try ${current.attempt}/${current.maxAttempts})"
                    } else {
                        "Listening…"
                    },
                    pulsing = true,
                    onClick = {}
                )

                is RecognitionState.Identifying -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = if (current.maxAttempts > 1) {
                            "Identifying… (try ${current.attempt}/${current.maxAttempts})"
                        } else {
                            "Identifying…"
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                is RecognitionState.Success -> ResultCard(match = current.match, onListenAgain = { viewModel.reset() })

                is RecognitionState.NoMatch -> MessageCard(
                    message = current.message,
                    onRetry = { viewModel.reset() }
                )

                is RecognitionState.Error -> MessageCard(
                    message = current.message,
                    onRetry = { viewModel.reset() }
                )
            }
        }

        if (permissionDenied) {
            Text(
                text = "Microphone permission is required to identify songs.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IdentifyButton(label: String, pulsing: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by if (pulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
            label = "pulse-scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(enabled = !pulsing, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size((72.dp.value * scale).dp)
            )
        }
        Text(text = label, modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ResultCard(match: SongMatch, onListenAgain: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (match.artworkUrl != null) {
            AsyncImage(
                model = match.artworkUrl,
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Text(
            text = match.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = match.artist,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (match.album != null) {
            Text(
                text = match.album,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (match.songLink != null) {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(match.songLink)))
                },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Open song link")
            }
        }

        Text(
            text = "Listen again",
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable(onClick = onListenAgain),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MessageCard(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, end = 8.dp)
        )
        Button(onClick = onRetry) {
            Text("Try again")
        }
    }
}
