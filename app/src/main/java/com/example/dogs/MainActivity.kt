package com.example.dogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class Dog(val name: String, var isLiked: Boolean = false, val owner: String = "Jack Russel")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DogsTheme {
                val dogs = remember { mutableStateOf(emptySet<Dog>()) }
                var searchText by remember { mutableStateOf("") }
                var searching by remember { mutableStateOf(false) }
                var isError by remember { mutableStateOf(false) }
                var selectedDog by remember { mutableStateOf<Dog?>(null) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.DogList) }

                val filteredDogs = remember(dogs.value, searchText, searching) {
                    val likedDogs = dogs.value.filter { it.isLiked }.sortedBy { it.name }
                    val unlikedDogs = dogs.value.filter { !it.isLiked }.sortedBy { it.name }
                    val allDogs = likedDogs + unlikedDogs

                    if (searchText.isEmpty() || !searching) {
                        allDogs
                    } else {
                        allDogs.filter { it.name.contains(searchText, ignoreCase = true) }
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
                                            onSearch = {
                                                searching = true
                                            },
                                            onClearSearch = {
                                                searchText = ""
                                                searching = false
                                            },
                                            onAdd = { name ->
                                                if (name.isNotBlank()) {
                                                    if (dogs.value.none { it.name.equals(name, ignoreCase = true) }) {
                                                        dogs.value += Dog(name)
                                                        searchText = ""
                                                        isError = false
                                                    } else {
                                                        isError = true
                                                    }
                                                }
                                            },
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
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                        Screen.Settings -> {
                            SettingsScreen(onBackClick = { currentScreen = Screen.DogList })
                        }
                        Screen.Profile -> {
                            ProfileScreen(onBackClick = { currentScreen = Screen.DogList })
                        }
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
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dog.name, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text(text = dog.owner, style = TextStyle(fontSize = 14.sp, color = Color.Gray))
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
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onAdd: (String) -> Unit,
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
            placeholder = { Text("Search or add a dog 🐕") },
            isError = isError
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { if (searchText.isNotEmpty()) onSearch() else onClearSearch() },
            enabled = searchText.isNotEmpty()
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
        IconButton(
            onClick = { onAdd(searchText) },
            enabled = searchText.isNotEmpty()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
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
    modifier: Modifier = Modifier
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFEE82EE), Color(0xFFBA55D3))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🐕", fontSize = 60.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = dog.name,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dog.owner,
                style = TextStyle(fontSize = 18.sp),
                color = Color.Gray
            )
        }
    }
}