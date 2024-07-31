package com.example.rmas

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavHostController
import com.example.rmas.data.Request
import com.example.rmas.database.FirebaseDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(id: String,  navHostController: NavHostController) {
    var username by remember { mutableStateOf("") }
    FirebaseDatabase.getUser(id) {
        if (it != null) {
            username = it.username
        }
    }

    val listOfTypes = listOf("Kozmeticki salon", "Frizerski salon", "Drogerija", "Parfimerija")
    var type by remember { mutableStateOf(listOfTypes[0]) }

    val listOfPurposes = listOf("Tretman", "Usluga", "Kupovina", "Uzorkovanje proizvoda")
    var purpose by remember { mutableStateOf(listOfPurposes[0]) }
    var description by remember { mutableStateOf("") }
    var typeSize by remember { mutableStateOf(Size.Zero) }
    var purposeSize by remember { mutableStateOf(Size.Zero) }
    var expanded by remember { mutableStateOf(false) }
    var expandedPur by remember { mutableStateOf(false) }
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))




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
                        date = currentDate,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded ,
                    onExpandedChange = {expanded = !expanded} )
                {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = false,
                        value = type ,
                        onValueChange = {},
                        label = {Text("Tip")},
                        trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded )},
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false}) {

                        listOfTypes.forEach{ selectedOption ->
                            DropdownMenuItem(
                                text = { Text(selectedOption)},
                                onClick = {
                                    type = selectedOption
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedPur ,
                    onExpandedChange = {expandedPur = !expandedPur} )
                {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = false,
                        value = purpose ,
                        onValueChange = {},
                        label = {Text("Delatnost")},
                        trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPur )},
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPur,
                        onDismissRequest = { expandedPur = false}) {

                        listOfPurposes.forEach{ selectedOptionPurpose ->
                            DropdownMenuItem(
                                text = { Text(selectedOptionPurpose)},
                                onClick = {
                                    purpose = selectedOptionPurpose
                                    expandedPur = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Description") },
                )
            }
        }
    )
}