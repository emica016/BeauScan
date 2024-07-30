package com.example.rmas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.rmas.data.Request
import com.example.rmas.database.FirebaseDatabase



@Composable
fun CreateRequestScreen(id: String,  navHostController: NavHostController) {
    var username by remember { mutableStateOf("") }
    FirebaseDatabase.getUser(id) {
        if (it != null) {
            username = it.username
        }
    }
    val (type, setType) = remember { mutableStateOf("") }
    val (purpose, setPurpose) = remember { mutableStateOf("") }
    val (description, setDescription) = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = {
                    val request = Request(
                        id = Math.random().toString(),
                        type = type,
                        purpose = purpose,
                        description = description,
                        creatorID = username,
                        numberOfResponses = 0,
                        responses = emptyList()
                    )
                    // Save the request to your database
                    FirebaseDatabase.saveRequest(request)
                    navHostController.popBackStack()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = {
                navHostController.popBackStack()
            }) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(value = type, onValueChange = setType)
                TextField(value = purpose, onValueChange = setPurpose)
                TextField(value = description, onValueChange = setDescription)
            }
        }
    )
}