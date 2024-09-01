import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rmas.R
import com.example.rmas.StartActivity
import com.example.rmas.navigation.Screens
import kotlinx.coroutines.launch
import com.example.rmas.data.Request
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Home screen composable function
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Home(idUser: String, navHostController: NavHostController, onRequestClicked: (String) -> Unit) {
    var requests by remember { mutableStateOf(emptyList<Request>()) }
    var filteredRequests by remember { mutableStateOf(emptyList<Request>()) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            FirebaseFirestore.getInstance().collection("requests").get()
                .addOnSuccessListener { snapshots ->
                    requests = snapshots.documents.mapNotNull { it.toObject(Request::class.java) }
                    filteredRequests = requests
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Home", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { navHostController.navigate(Screens.CreateRequest.screen) }) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircle,
                            contentDescription = "Add Request"
                        )
                    }
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, StartActivity::class.java).apply {
                            // Clear the activity stack and start the StartActivity
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFE0A9A5)
                )
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = paddingValues.calculateTopPadding() + 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter chips row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Kozmeticki salon", "Frizerski salon", "Drogerija", "Parfimerija").forEach { type ->
                            FilterChip(
                                onClick = {
                                    selectedType = if (selectedType == type) null else type
                                    filteredRequests = if (selectedType != null) {
                                        requests.filter { it.type == selectedType }
                                    } else {
                                        requests
                                    }
                                },
                                label = { Text(type) },
                                selected = selectedType == type,
                                leadingIcon = {
                                    if (selectedType == type) {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        color = if (selectedType == type) Color(0xFFE6D8E6) else Color.Transparent, // Subtle lilac color
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }
                }

                // Items
                items(filteredRequests) { request ->
                    RequestListItem(request, onRequestClicked)
                }
            }
        }
    )
}

// Request list item composable function
@Composable
fun RequestListItem(request: Request, onRequestClicked: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8DCDE),
        modifier = Modifier
            .height(200.dp)
            .padding(vertical = 4.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable { onRequestClicked(request.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFE2756D)
                ) {
                    Text(
                        text = request.type,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = request.purpose,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(text = request.date) // Display the date as is

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Created by: ${request.creatorID}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    shape = RoundedCornerShape(8.dp),
                    onClick = { onRequestClicked(request.id) }
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(100.dp, 140.dp)
            ) {
                Image(
                    painter = when (request.type) {
                        "Kozmeticki salon" -> painterResource(id = R.drawable.beautysalon)
                        "Frizerski salon" -> painterResource(id = R.drawable.hairsalon)
                        "Drogerija" -> painterResource(id = R.drawable.store)
                        "Parfimerija" -> painterResource(id = R.drawable.parfume)
                        else -> painterResource(id = R.drawable.default_image)
                    },
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
        }
    }
}
