package com.firza.headshotapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import com.firza.headshotapp.R

sealed class Screen(val route: String, val icon: ImageVector, @StringRes val resourceId: Int) {
    object MainScreen : Screen("mainScreen", Icons.Default.Home, R.string.main_screen)
    object CameraScreen : Screen("cameraScreen", Icons.Default.Add, R.string.camera_screen)
    object FormScreen : Screen("formScreen", Icons.Default.Edit, R.string.form_screen)
}