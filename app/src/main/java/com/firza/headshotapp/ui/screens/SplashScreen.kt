package com.firza.headshotapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.firza.headshotapp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    // State untuk mengendalikan ukuran gambar
    var sizeState by remember { mutableStateOf(180.dp) }

    // Animasi yang memperbesar dan memperkecil gambar
    LaunchedEffect(key1 = true) {
        while (true) {
            sizeState = 210.dp
            delay(400)
            sizeState = 180.dp
            delay(400)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_headshot_app),
            contentDescription = "Logo",
            modifier = Modifier
                .size(sizeState)
                .animateContentSize()
        )
    }

    LaunchedEffect(key1 = "navigate") {
        delay(3200)
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true }
        }
    }
}

