package com.firza.headshotapp.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.firza.headshotapp.ui.viewmodels.SharedViewModel
import com.google.accompanist.coil.rememberCoilPainter
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.firza.headshotapp.R
import com.firza.headshotapp.db.entity.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    navController: NavHostController,
    viewModel: SharedViewModel,
    user: UserEntity? = null
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val imageUriString = sharedPreferences.getString("imageUri", null)
    val imageUri = imageUriString?.let { Uri.parse(it) }

    Log.d("ImageUri", "FormScreen: $imageUri")

    fun validateAndSubmit() {
        val isNameValid = name.isNotBlank()
        val isPhoneValid = phone.isNotBlank() && phone.all { it.isDigit() }

        nameError = !isNameValid
        phoneError = !isPhoneValid

        if (isNameValid && isPhoneValid) {
            viewModel.insertUser(name, phone, imageUri.toString())
            navController.navigate("main") {
                // Bersihkan seluruh back stack
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberImagePainter(data = imageUri),
                    contentDescription = "Captured Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(fraction = 5f)
                )
            } else {
                Text("No image available")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field for name
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false // Reset error state when user starts typing
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError // Show error state if nameError is true
        )
        if (nameError) {
            Text("Name cannot be empty", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Field for phone number
        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneError = false // Reset error state when user starts typing
            },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            isError = phoneError // Show error state if phoneError is true
        )
        if (phoneError) {
            Text("Phone cannot be empty and must be numeric", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Button Save
        Button(
            onClick = {
                validateAndSubmit()
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Text("Save")
        }
    }
}