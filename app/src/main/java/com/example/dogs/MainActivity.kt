package com.example.dogs

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogs.ui.theme.DogsTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

interface DogApiService {
    @GET("breeds/image/random")
    suspend fun getRandomDogImage(): DogApiResponse
}

@Serializable
data class DogApiResponse(
    val message: String,
    val status: String
)

data class Dog(val name: String, var isLiked: Boolean = false, val breed: String = "Unknown", val imageUrl: String? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DogsTheme {
                val dogs = remember { mutableStateOf(emptySet<Dog>()) }
                var searchText by remember { mutableStateOf("") }
                var isError by remember { mutableStateOf(false) }
                var selectedDog by remember { mutableStateOf<Dog?>(null) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.DogList) }
                var newDog by remember { mutableStateOf<Dog?>(null) }
                var addDogError by remember { mutableStateOf<String?>(null) }

                val filteredDogs = remember(dogs.value, searchText) {
                    val likedDogs = dogs.value.filter { it.isLiked }.sortedBy { it.name }
                    val unlikedDogs = dogs.value.filter { !it.isLiked }.sortedBy { it.name }
                    val allDogs = likedDogs + unlikedDogs

                    if (searchText.isEmpty()) {
                        allDogs
                    } else {
                        allDogs.filter { it.name.startsWith(searchText, ignoreCase = true) }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.DogList -> {
                            if (selectedDog == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    TopAppBar(
                                        onSettingsClick = { currentScreen = Screen.Settings },
                                        onProfileClick = { currentScreen = Screen.Profile }
                                    )
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        DogSearchBar(
                                            searchText = searchText,
                                            onSearchTextChange = {
                                                searchText = it
                                                isError = false
                                            },
                                            onAddClick = { currentScreen = Screen.AddDog },
                                            isError = isError,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (isError) {
                                            Text(
                                                text = "A dog with this name already exists.",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        val likedCount = filteredDogs.count { it.isLiked }
                                        DogCounter(dogCount = filteredDogs.size, likedCount = likedCount)
                                        DogList(
                                            dogs = filteredDogs,
                                            onLikeClick = { dog ->
                                                dogs.value = dogs.value.map {
                                                    if (it.name == dog.name) {
                                                        it.copy(isLiked = !it.isLiked)
                                                    } else {
                                                        it
                                                    }
                                                }.toSet()
                                            },
                                            onRemoveClick = { dog ->
                                                dogs.value = dogs.value.filter { it.name != dog.name }.toSet()
                                            },
                                            onDogClick = { dog ->
                                                selectedDog = dog
                                            }
                                        )
                                    }
                                }
                            } else {
                                DogDetailsScreen(
                                    dog = selectedDog!!,
                                    onBackClick = { selectedDog = null },
                                    onRemoveClick = { dog ->
                                        dogs.value = dogs.value.filter { it.name != dog.name }.toSet()
                                        selectedDog = null
                                    },
                                    modifier = Modifier.padding(innerPadding),
                                    context = this
                                )
                            }
                        }
                        Screen.Settings -> {
                            SettingsScreen(onBackClick = { currentScreen = Screen.DogList })
                        }
                        Screen.Profile -> {
                            ProfileScreen(onBackClick = { currentScreen = Screen.DogList })
                        }
                        Screen.AddDog -> {
                            AddDogScreen(
                                onBackClick = { currentScreen = Screen.DogList },
                                onDogAdded = { dog ->
                                    if (dogs.value.any { it.name.equals(dog.name, ignoreCase = true) }) {
                                        addDogError = "Dog with this name already exists."
                                    } else {
                                        newDog = dog
                                        addDogError = null
                                        currentScreen = Screen.DogList
                                    }
                                },
                                modifier = Modifier.padding(innerPadding),
                                errorMessage = addDogError
                            )
                        }
                    }
                }
                LaunchedEffect(newDog) {
                    newDog?.let { dog ->
                        dogs.value += dog
                        newDog = null
                    }
                }
            }
        }
    }
}


sealed class Screen {
    data object DogList : Screen()
    data object Settings : Screen()
    data object Profile : Screen()
    data object AddDog : Screen()
}
@Composable
fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { TopBarWithBackButton(onBackClick = onBackClick, title = "Settings") },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        }
    }
}

@Composable
fun ProfileScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { TopBarWithBackButton(onBackClick = onBackClick, title = "Profile") },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profile", Modifier.size(80.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Jan Brzechwa",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopBarWithBackButton(
    onBackClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF2E9F3)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = title,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DogList(
    dogs: List<Dog>,
    onLikeClick: (Dog) -> Unit,
    onRemoveClick: (Dog) -> Unit,
    onDogClick: (Dog) -> Unit
) {
    LazyColumn {
        items(dogs) { dog ->
            DogItem(dog, onLikeClick, onRemoveClick, onDogClick)
        }
    }
}

@Composable
fun DogItem(
    dog: Dog,
    onLikeClick: (Dog) -> Unit,
    onRemoveClick: (Dog) -> Unit,
    onDogClick: (Dog) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDogClick(dog) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dog.imageUrl != null) {
                AsyncImage(
                    model = dog.imageUrl,
                    contentDescription = "Dog image of ${dog.name}",
                    modifier = Modifier
                        .size(40.dp) // Ustalony rozmiar kwadratu
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop // Dodane ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFEE82EE), Color(0xFFBA55D3))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🐕", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dog.name, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text(text = dog.breed, style = TextStyle(fontSize = 14.sp, color = Color.Gray))
            }
            IconButton(onClick = { onLikeClick(dog) }) {
                val icon = if (dog.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                val tintColor = if (dog.isLiked) Color(0xFF9400D3) else Color.Gray
                Icon(
                    imageVector = icon,
                    contentDescription = "Like",
                    tint = tintColor
                )
            }
            IconButton(onClick = { onRemoveClick(dog) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.Red)
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray)
        )
    }
}

@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF2E9F3))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Black)
            }

            Text(
                text = "Doggos",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            IconButton(onClick = onProfileClick) {
                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Black)
            }
        }
    }
}

@Composable
fun DogSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search for a dog 🐕") },
            isError = isError
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onAddClick,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add a dog")
        }
    }
}

@Composable
fun DogCounter(dogCount: Int, likedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🐶", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$dogCount", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, contentDescription = "Number of Liked Dogs", tint = Color(0xFF9400D3))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$likedCount", fontSize = 16.sp)
        }
    }
}

@Composable
fun DogDetailsScreen(
    dog: Dog,
    onBackClick: () -> Unit,
    onRemoveClick: (Dog) -> Unit,
    modifier: Modifier = Modifier,
    context: Context
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFF2E9F3))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Text(
                    text = "Details",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { onRemoveClick(dog) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Black)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (dog.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(dog.imageUrl)
                        .build(),
                    contentDescription = "Image of ${dog.name}",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFEE82EE), Color(0xFFBA55D3))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🐕", fontSize = 60.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = dog.name,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dog.breed,
                style = TextStyle(fontSize = 18.sp),
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AddDogScreen(
    onBackClick: () -> Unit,
    onDogAdded: (Dog) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    var dogName by remember { mutableStateOf("") }
    var dogBreed by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = RetrofitClient.apiService.getRandomDogImage()
            imageUrl = response.message
        } catch (e: Exception) {
            println("Image download error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    val isButtonEnabled = dogName.isNotBlank() && dogBreed.isNotBlank()
    val buttonColor = if (isButtonEnabled) {
        Brush.horizontalGradient(listOf(Color(0xFFEE82EE), Color(0xFFBA55D3)))
    } else {
        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
    }
    val buttonTextColor = if (isButtonEnabled) Color.White else Color.DarkGray

    Scaffold(
        topBar = { TopBarWithBackButton(onBackClick = onBackClick, title = "Add a Dog") },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Random dog image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("No photo", color = Color.Gray)
                    }
                }
            }

            OutlinedTextField(
                value = dogName,
                onValueChange = { dogName = it },
                label = { Text("Dog's Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dogBreed,
                onValueChange = { dogBreed = it },
                label = { Text("Dog's Breed") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    if (isButtonEnabled) {
                        onDogAdded(Dog(name = dogName, breed = dogBreed, imageUrl = imageUrl))
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(buttonColor),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Add Dog", color = buttonTextColor)
            }
        }
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://dog.ceo/api/"

    val apiService: DogApiService by lazy {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DogApiService::class.java)
    }
}
