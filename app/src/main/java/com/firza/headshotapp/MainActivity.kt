package com.firza.headshotapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firza.headshotapp.ui.navigation.AppNavigation
import com.firza.headshotapp.ui.theme.HeadshotAppTheme
import com.firza.headshotapp.ui.viewmodels.SharedViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeadshotAppTheme {
                val sharedViewModel: SharedViewModel = viewModel()
                AppNavigation(sharedViewModel)
            }
        }
    }
}