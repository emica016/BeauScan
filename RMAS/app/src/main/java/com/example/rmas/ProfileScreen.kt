package com.example.rmas


import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.security.AllPermission

@Composable
fun Profile(id: String){
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }

    Log.e("USER1", id)
    getUser(id){
        if(it !=null){
            email = it.email
            username= it.username
            fullName = it.fullName
            phoneNumber = it.phoneNumber
            image = it.image
        }
        
    }
    
    Log.e("USER", username)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Surface(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = Color(0xFFB5485D),
                    shape = CircleShape
                )
        ){
            AsyncImage(
                model = image, 
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 24.dp)
        ){
            Row(
                modifier = Modifier.padding(24.dp, 0.dp),
                horizontalArrangement = Arrangement.Start
            ){
                Icon(
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                        .size(26.dp),
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.Start
            ){
                Text(text = fullName)
            }
        }

        Divider(
            modifier = Modifier.width(250.dp),
            color = Color(0xFFB5485D),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 24.dp)

        ){
            Row(
                modifier = Modifier.padding(24.dp, 0.dp),
                horizontalArrangement = Arrangement.Start
            ){
                Icon(
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                        .size(26.dp),
                    imageVector = Icons.Default.Email,
                    contentDescription = null
                )
            }
            Row(
                horizontalArrangement = Arrangement.Start
            ){
                Text(text = email)
            }


        }

        Divider(
            modifier = Modifier.width(250.dp),
            color = Color(0xFFB5485D),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 24.dp)

        ){
            Row(
                modifier = Modifier.padding(24.dp, 0.dp),
                horizontalArrangement = Arrangement.Start
            ){
                Icon(
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                        .size(26.dp),
                    painter = painterResource(id = R.drawable.username),
                    contentDescription = null
                )
            }
            Row(
                horizontalArrangement = Arrangement.Start
            ){
                Text(text = username)
            }


        }
        Divider(
            modifier = Modifier.width(250.dp),
            color = Color(0xFFB5485D),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp, 0.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                        .size(26.dp),
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                )
            }
            Row(
                horizontalArrangement = Arrangement.Start
            ) {
                Text(text = phoneNumber)
            }
        }
        Divider(
            modifier = Modifier.width(250.dp),
            color = Color(0xFFB5485D),
            thickness = 1.dp
        )


    }

    var location by remember { mutableStateOf(LocationInfo.locationServiceStatus) }
    val context = LocalContext.current

    if(FirebaseDatabase.getCurrentUser() == id){
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                Text("Pokreni servis za lokaciju: ")
                Checkbox(checked = location, onCheckedChange = {
                    location = it
                    if(location){
                        LocationInfo.enableLocation(context)

                    }
                    else{
                        LocationInfo.disableLocation(context)
                    }
                })
            }
        }
    }

}

@Preview
@Composable
fun PreviewProfile()
{
    RMASTheme {
        Profile("")
    }
}
