package com.firza.headshotapp.ui.navigation

import MainScreen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.firza.headshotapp.ui.screens.CameraScreen
import com.firza.headshotapp.ui.screens.FormScreen
import com.firza.headshotapp.ui.screens.SplashScreen
import com.firza.headshotapp.ui.viewmodels.SharedViewModel
import com.google.mlkit.vision.segmentation.SegmentationMask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(sharedViewModel: SharedViewModel) {
    val navController = rememberNavController()
    val showSidebar = remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val showTopBar = currentRoute != "splash"

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Headshot App") },
                    navigationIcon = {
                        if (currentRoute != "splash" && currentRoute != "main") {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else if (currentRoute == "main") {
                            IconButton(onClick = { showSidebar.value = !showSidebar.value }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (showSidebar.value) {
                Sidebar(navController, showSidebar)
            }
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController) }
                    composable("main") { MainScreen(navController, sharedViewModel) }
                    composable("camera") {
                        CameraScreen(
                            navController,
                            LocalLifecycleOwner.current,
                            sharedViewModel
                        )
                    }
                    composable("formScreen") {
                        val formViewModel: SharedViewModel = viewModel()
                        FormScreen(navController, formViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(navController: NavController, showSidebar: MutableState<Boolean>) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text("Home", modifier = Modifier
            .clickable {
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                    launchSingleTop = true
                }
                showSidebar.value = false
            }
            .padding(8.dp))

        Text("Camera", modifier = Modifier
            .clickable {
                navController.navigate("camera") {
                    popUpTo("main") {inclusive = true}
                    launchSingleTop = true
                }
                showSidebar.value = false
            }
            .padding(8.dp))
    }
}