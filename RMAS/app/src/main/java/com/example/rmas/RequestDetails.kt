package com.example.rmas

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
    Log.e("REQUEST1", requestId)
    LaunchedEffect(key1 = requestId.replace("{", "").replace("}", "")) {
        Firebase.firestore.collection("requests").document(requestId.replace("{", "").replace("}", "")).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                type = snapshot.getString("type")!!
                creator = snapshot.getString("creatorID")!!
                description = snapshot.getString("description")!!
            } else {
                Log.e("TAG", "NEUSPESNO")
            }
        }.await()
    }
    Text(text = type)
    Text(text = creator)
    Text(text = description)
    Button(onClick = {
        navHostController.navigate(Screens.CreateRequest.screen)
    },
        ) {
        Text(text = "Kreiraj pin")
    }




        when (type) {
            "Kozmeticki salon" -> {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerWeight / 2)
                        .fillMaxWidth(),
                    painter = painterResource(id = R.drawable.beautysalon),
                    contentDescription = null // Dodajte contentDescription ako je potrebno
                )
            }
            "Frizerski salon" -> {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerWeight / 2)
                        .fillMaxWidth(),
                    painter = painterResource(id = R.drawable.hairsalon),
                    contentDescription = null // Dodajte contentDescription ako je potrebno
                )
            }
            "Drogerija" -> {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerWeight / 2)
                        .fillMaxWidth(),
                    painter = painterResource(id = R.drawable.store),
                    contentDescription = null // Dodajte contentDescription ako je potrebno
                )
            }
            "Parfimerija" -> {
                Image(
                    modifier = Modifier
                        .heightIn(max = containerWeight / 2)
                        .fillMaxWidth(),
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




