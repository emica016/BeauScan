package com.example.rmas

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rmas.data.LocationInfo
import com.example.rmas.database.FirebaseDatabase
import com.example.rmas.database.FirebaseDatabase.getUser
import com.example.rmas.ui.theme.RMASTheme

@Composable
fun Profile(id: String) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load user data
    getUser(id) {
        if (it != null) {
            email = it.email
            username = it.username
            fullName = it.fullName
            phoneNumber = it.phoneNumber
            image = it.image
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .border(BorderStroke(2.dp, Color(0xFFB5485D)), RoundedCornerShape(60.dp)),
            color = Color.Gray
        ) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = fullName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // User Info
        ProfileInfo(icon = Icons.Default.Person, info = username)
        ProfileInfo(icon = Icons.Default.Email, info = email)
        ProfileInfo(icon = Icons.Default.Phone, info = phoneNumber)

        Spacer(modifier = Modifier.height(16.dp))

        // Location Service Toggle
        if (FirebaseDatabase.getCurrentUser() == id) {
            var location by remember { mutableStateOf(LocationInfo.locationServiceStatus) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Enable Location Service:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Checkbox(
                    checked = location,
                    onCheckedChange = {
                        location = it
                        if (location) {
                            LocationInfo.enableLocation(context)
                        } else {
                            LocationInfo.disableLocation(context)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change Profile Image Button
            Button(
                onClick = { isEditing = !isEditing },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Change Profile Image")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProfileInfo(icon: ImageVector, info: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = info,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfile() {
    RMASTheme {
        Profile("")
    }
}
