package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.model.LocalAppProfile
import com.example.util.ProfileManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = LocalAppProfile.current

    LaunchedEffect(Unit) {
        delay(800)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)), // Pure Black (#000000)
        contentAlignment = Alignment.Center
    ) {
        // Transparent logo wrapper with width: 75vw, height: auto, object-fit: contain (anti-squash)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .wrapContentHeight()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = activeProfile.splashLogoRes),
                contentDescription = activeProfile.displayNameEnglish,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

