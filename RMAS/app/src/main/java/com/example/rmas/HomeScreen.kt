import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rmas.navigation.Screens
import kotlinx.coroutines.launch
import com.example.rmas.data.Request
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

  //OVO TREBA JOS DA SE ISPRAVLJA, + TREBA DA SE DODA DATUM ZA REQUEST
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Home(id: String, navHostController: NavHostController) {
    var fullName by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()

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
                title = { Text(text = "Home") },
                actions = {
                    Button(onClick = {
                        navHostController.navigate(Screens.CreateRequest.screen)
                    }) {
                        Text(text = "Dodaj zahtev")
                    }
                },
                modifier = Modifier
                    .padding(bottom = 20.dp)
            )
        },
        content = {
            if (requests.isEmpty()) {
                Text(text = "Nema zahteva")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = requests,
                        itemContent = { requestId ->
                            requestListItem(requestId, navHostController)
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun requestListItem(requestId: String, navHostController: NavHostController) {

    var type by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }

    Firebase.firestore.collection("requests").document(requestId).get()
        .addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                val request = snapshot.toObject(Request::class.java)
                if (request != null) {
                    type = request.type
                    purpose = request.purpose
                    description = request.description
                    creator = request.creatorID
                }
            }
        }

    Card(
        modifier = Modifier
            .height(200.dp)
            .padding(horizontal = 10.dp)
            .clickable {
                navHostController.navigate(Screens.RequestDetails.screen)
            }

    )
    {

            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Tip: " + type,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(2f)
                        .height(20.dp)
                )

                Text(
                    text = creator,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delatnost: " + purpose,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Text(
                text = "Opis: " + description,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Left

            )
            Text(
                text = "Vidi detalje",
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                    .clickable {
                        navHostController.navigate(Screens.RequestDetails.screen)

                    }

            )


        }
    }
