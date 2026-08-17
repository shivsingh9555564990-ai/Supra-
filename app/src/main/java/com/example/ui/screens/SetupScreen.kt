package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ChatViewModel
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(viewModel: ChatViewModel, onSetupComplete: () -> Unit) {
    val progress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val statusText by viewModel.setupStatus.collectAsStateWithLifecycle()
    val isComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "sphere")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        viewModel.startModelSetup()
    }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Sphere
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation = 50.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary, ambientColor = MaterialTheme.colorScheme.secondary)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "SuperNova Edge AI",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Direct interactive action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.startModelSetup() },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Retry Download", color = MaterialTheme.colorScheme.onBackground)
            }

            val canEnter = isComplete || viewModel.areModelsDownloaded()
            androidx.compose.material3.Button(
                onClick = {
                    if (canEnter) {
                        onSetupComplete()
                    } else {
                        viewModel.startModelSetup()
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (canEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (canEnter) "Enter Chat" else "Download Required",
                    color = if (canEnter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
