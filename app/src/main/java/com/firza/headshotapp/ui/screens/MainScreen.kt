import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.firza.headshotapp.R
import com.firza.headshotapp.db.entity.UserEntity
import com.firza.headshotapp.ui.viewmodels.SharedViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: SharedViewModel) {
    val userList by viewModel.userList.observeAsState(emptyList())

    LaunchedEffect(key1 = Unit) {
        viewModel.getUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Galery Headshot App",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Scaffold(
            floatingActionButton = {
                FabIconWithDrawable {
                    navController.navigate("camera")
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (userList.isEmpty()) {
                // Tampilkan teks jika data kosong
                Text("Data masih kosong", fontSize = 18.sp, color = Color.Gray)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxWidth(), // Memastikan grid mengisi lebar maksimum
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userList) { user ->
                        var isEditing by remember { mutableStateOf(false) }
                        var editedName by remember { mutableStateOf(user.name) }
                        var editedPhone by remember { mutableStateOf(user.phone) }


                        HeadshotCard(
                            name = if (isEditing) editedName else user.name,
                            imageUri = user.imageUri,
                            nohp = if (isEditing) editedPhone else user.phone,
                            userId = user.id,
                            onDelete = { userId ->
                                viewModel.deleteUser(userId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FabIconWithDrawable(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .border(BorderStroke(1.dp, Color.Transparent))
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add_photo),
            contentDescription = "Open Camera",
            modifier = Modifier
                .size(80.dp)
                .padding(10.dp)
        )
    }
}

@Composable
fun HeadshotCard(
    name: String,
    imageUri: String,
    nohp: String,
    userId: Int,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Image(
                    painter = rememberImagePainter(data = imageUri),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(fraction = 5f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.2.dp,
                            Color.Gray,
                            RoundedCornerShape(10.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nama : $name",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No Hp : $nohp",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))
                IconButton(
                    onClick = { onDelete(userId) },
                    modifier = Modifier
                        .size(26.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Delete",
                        modifier = Modifier
                            .size(26.dp)
                    )
                }
            }
        }
    }
}