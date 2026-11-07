package com.veroflow.veroconnect.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.User
import com.veroflow.veroconnect.ui.theme.BrandGradient

/** Profile picture. Shows initials on a colour until the photo loads (or if it can't load). */
@Composable
fun Avatar(user: User, size: Int = 44, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(user.avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = user.initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.36f).sp
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Pulsing light overlay shown while an image is loading. */
@Composable
private fun ShimmerOverlay() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha)))
}

/**
 * Remote picture with a loading shimmer and an offline-safe fallback
 * (plain coloured box if the picture fails to load).
 */
@Composable
fun RemoteImage(
    url: String?,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var failed by remember(url) { mutableStateOf(false) }
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier.background(
            if (failed || url == null) fallbackColor else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (url != null && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loaded = true },
                onError = { failed = true }
            )
            if (!loaded) ShimmerOverlay()
        }
    }
}

/** Photo or video thumbnail. Videos get a play button and a duration badge. */
@Composable
fun MediaPlaceholder(
    color: Color,
    mediaType: MediaType,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    imageUrl: String? = null,
    duration: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp)),
        contentAlignment = Alignment.Center
    ) {
        RemoteImage(
            url = imageUrl,
            fallbackColor = color,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        if (mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = Color.White)
            }
            if (duration != null) {
                Text(
                    text = duration,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** App logo: gradient circle with a chat bubble. */
@Composable
fun AppLogo(size: Int = 88, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size * 0.72f).dp)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((size * 0.38f).dp)
            )
        }
    }
}

/** Shows a one-shot message (set via MockRepository.toast) as a snackbar on the next screen. */
@Composable
fun ToastEffect(host: SnackbarHostState) {
    val message = MockRepository.toast.value
    LaunchedEffect(message) {
        if (message != null) {
            host.showSnackbar(message)
            MockRepository.toast.value = null
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Inbox
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptySearchState(modifier: Modifier = Modifier) {
    EmptyState(
        title = "No results found",
        subtitle = "Try a different keyword or check your spelling.",
        icon = Icons.Filled.SearchOff,
        modifier = modifier
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
