package com.example.rmas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun FilterDialog(navHostController: NavHostController) {
    var typeFilters by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(onDismissRequest = {  },
        confirmButton = { Button (
            onClick = {

            }
        ){
            Text("OK")
        }


        },
        dismissButton = {
            Button(
                onClick = {navHostController.popBackStack()}
            ){
                Text("Cancel")
            }
        }, text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ("Kozmeticki salon"),
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Checkbox(
                        checked = typeFilters.contains("Kozmeticki salon"),
                        onCheckedChange = { typeFilters = typeFilters.toSet().also { it.takeIf { it.isEmpty() }?.let { typeFilters = it } }
                        })

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frizerski salon",
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = typeFilters.contains("Frizerski salon"),
                        onCheckedChange = { typeFilters = typeFilters.toSet().also { it.takeIf { it.isEmpty() }?.let { typeFilters = it } }
                        })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Drogerija",
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = typeFilters.contains("Drogerija"),
                        onCheckedChange = { typeFilters = typeFilters.toSet().also { it.takeIf { it.isEmpty() }?.let { typeFilters = it } }
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parfimerija",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = typeFilters.contains("Parfimerija"),
                            onCheckedChange = { typeFilters = typeFilters.toSet().also { it.takeIf { it.isEmpty() }?.let { typeFilters = it } }
                            })
                    }
                }
            }
        })

}