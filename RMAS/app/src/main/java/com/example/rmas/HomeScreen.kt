import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rmas.R
import com.example.rmas.navigation.Screens
import kotlinx.coroutines.launch
import com.example.rmas.data.Request
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


//OVO TREBA JOS DA SE ISPRAVLJA, + TREBA DA SE DODA DATUM ZA REQUEST
  @OptIn(ExperimentalMaterial3Api::class)
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SuspiciousIndentation")
  @Composable
fun Home(idUser: String, navHostController: NavHostController, onRequestClicked: (String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        scope.launch {
            val requestSnapshots = Firebase.firestore.collection("requests").get()
            requestSnapshots.addOnSuccessListener { snapshots ->
                requests = snapshots.documents.map { it.id }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Home", fontWeight = FontWeight.Bold)
                    }

                },
                Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp)),

                actions = {
                    IconButton(onClick = { }) {
                        BadgedBox(badge = {
                            Badge(
                                Modifier.size(10.dp)
                            ) {
                            }
                        }) {
                            IconButton(onClick = { navHostController.navigate(Screens.CreateRequest.screen) }) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircle,
                                    contentDescription = "Dodaj zahtev"
                                )
                            }
                        }

                    }

                    IconButton(onClick = { }) {
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
        content = {
            if (requests.isEmpty()) {
                Text(text = "Nema zahteva")
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            onClick = { selected = !selected },
                            label = {
                                Text("Kozmeticki salon")
                            },
                            selected = selected,
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                    selectedType = "Kozmeticki salon"
                                }
                            } else {
                                null
                            },
                        )

                        FilterChip(
                            onClick = { selected = !selected },
                            label = {
                                Text("Frizerski salon")
                            },
                            selected = selected,
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                    selectedType = "Frizerski salon"
                                }
                            } else {
                                null
                            },
                        )

                        FilterChip(
                            onClick = { selected = !selected },
                            label = {
                                Text("Drogerija")
                            },
                            selected = selected,
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                    selectedType = "Drogerija"
                                }
                            } else {
                                null
                            },
                        )

                        FilterChip(
                            onClick = { selected = !selected },
                            label = {
                                Text("Parfimerija")
                            },
                            selected = selected,
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                    selectedType = "Parfimerija"
                                }
                            } else {
                                null
                            },
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = requests,
                            itemContent = { requestId ->
                                requestListItem(requestId, onRequestClicked)
                            })
                    }
                }
            }
        })
}



@Composable
fun requestListItem(requestId: String,
                    onRequestClicked: (String) -> Unit) {

    var type by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    Firebase.firestore.collection("requests").document(requestId).get()
        .addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                val request = snapshot.toObject(Request::class.java)
                if (request != null) {
                    type = request.type
                    purpose = request.purpose
                    description = request.description
                    creator = request.creatorID
                    date = request.date
                }
            }
        }


    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8DCDE),
        modifier = Modifier
            .height(250.dp)
            .padding(10.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(2f),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.wrapContentSize(),
                    color = Color(0xFFE2756D)
                ) {
                    Text(
                        text = type,
                        fontSize =  12.sp,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = purpose,
                    fontSize =  24.sp,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(text = date)

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creator,
                        fontSize =  14.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                   /* Icon(
                        painter = painterResource(id = R.drawable.baseline_star_outline_24),
                        tint = Color(0xFFF6B266),
                        contentDescription = null
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.baseline_star_outline_24),
                        tint = Color(0xFFF6B266),
                        contentDescription = null
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.baseline_star_outline_24),
                        tint = Color(0xFFF6B266),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_star_outline_24),
                        tint = Color(0xFFF6B266),
                        contentDescription = null
                    )*/
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.White
                    ),
                    onClick = { onRequestClicked(requestId) }
                ) {
                    Text(
                        text = "Vidi detalje",
                        fontSize =  11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(width = 100.dp, height = 140.dp)
            ) {
                when (type) {
                    "Kozmeticki salon" -> {
                        Image(
                            painter = painterResource(id = R.drawable.beautysalon),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }

                    "Frizerski salon" -> {
                        Image(
                            painter = painterResource(id = R.drawable.hairsalon),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                    "Drogerija" -> {
                        Image(
                            painter = painterResource(id = R.drawable.store),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }

                    "Parfimerija" -> {
                        Image(
                            painter = painterResource(id = R.drawable.parfume),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                }

            }
        }
    }
}



