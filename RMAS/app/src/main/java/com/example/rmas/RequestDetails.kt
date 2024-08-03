package com.example.rmas

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rmas.data.Request
import com.example.rmas.database.FirebaseDatabase
import com.example.rmas.navigation.Screens
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RequestDetails(userId: String, requestId: String, navHostController: NavHostController) {
    val scrolState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrolState)
                ) {
                    ProfilHeader(requestId = requestId, navHostController,  containerWeight = this@BoxWithConstraints.maxHeight)
                }
            }
        }
    }
}

@Composable
private fun ProfilHeader(
    requestId: String, navHostController: NavHostController,
    containerWeight: Dp
) {
    var type by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var date by remember {mutableStateOf("")}
    var numberOfResponses by remember { mutableStateOf(0) }
    var responses by remember { mutableStateOf(emptyList<String>()) }


    Log.e("REQUEST1", requestId)
    LaunchedEffect(key1 = requestId.replace("{", "").replace("}", "")) {
        Firebase.firestore.collection("requests").document(requestId.replace("{", "").replace("}", "")).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                type = snapshot.getString("type")!!
                purpose = snapshot.getString("purpose")!!
                creator = snapshot.getString("creatorID")!!
                description = snapshot.getString("description")!!
                date = snapshot.getString("date")!!
                numberOfResponses = snapshot.getLong("numberOfResponses")!!.toInt()
                responses = (snapshot.get("responses") as List<String>?)!!

            } else {
                Log.e("TAG", "NEUSPESNO")
            }
        }.await()
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Box(Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ){
            when (type) {
                "Kozmeticki salon" -> {
                    Image(
                        modifier = Modifier
                            .heightIn(max = containerWeight / 2)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        painter = painterResource(id = R.drawable.beautysalon),
                        contentDescription = null // Dodajte contentDescription ako je potrebno
                    )
                }
                "Frizerski salon" -> {
                    Image(
                        modifier = Modifier
                            .heightIn(max = containerWeight / 2)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        painter = painterResource(id = R.drawable.hairsalon),
                        contentDescription = null // Dodajte contentDescription ako je potrebno
                    )
                }
                "Drogerija" -> {
                    Image(
                        modifier = Modifier
                            .heightIn(max = containerWeight / 2)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        painter = painterResource(id = R.drawable.store),
                        contentDescription = null // Dodajte contentDescription ako je potrebno
                    )
                }
                "Parfimerija" -> {
                    Image(
                        modifier = Modifier
                            .heightIn(max = containerWeight / 2)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        painter = painterResource(id = R.drawable.parfume),
                        contentDescription = null // Dodajte contentDescription ako je potrebno
                    )
                }
                else -> {
                    // Handle unexpected types here
                    // You can show a default image or message, or throw an exception if needed
                    Text("Nemam sliku za ovaj tip")
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(25.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment  = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.wrapContentSize(),
                    color = Color(0xFFE2756D)
                ) {
                    Text(
                        text = type,
                        fontSize =  20.sp,
                        //style = MaterialTheme.typography.titleLarge,
                       // modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
                Spacer(modifier  = Modifier.padding(4.dp))
                Text(text = date, fontSize = 14.sp, textAlign = TextAlign.End)
            }

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment  = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.wrapContentSize(),
                    color = Color(0xFF95C9C4)
                ) {
                    Text(
                        text = purpose,
                        fontSize =  16.sp,
                        //style = MaterialTheme.typography.titleLarge,
                        // modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.wrapContentSize(),
                    color = Color(0xFFC586CF)
                ) {
                    Text(
                        text = creator,
                        fontSize =  16.sp,
                        //style = MaterialTheme.typography.titleLarge,
                        // modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }

            Text(text = description, fontSize = 18.sp)

            Surface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.wrapContentSize(),
                color = Color(0xFF7EA9CC)
            ) {
                Text(
                    text = "Broj odgovora: " + numberOfResponses.toString(),
                    fontSize =  14.sp,
                    //style = MaterialTheme.typography.titleLarge,
                    // modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            if (numberOfResponses > 0) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(responses.size) { index ->
                        Text(text = responses[index], fontSize = 16.sp)
                    }
                }
            } else {

                    Text(text = "Jos uvek nema odgovora.")

            }
        }
    }





    }




