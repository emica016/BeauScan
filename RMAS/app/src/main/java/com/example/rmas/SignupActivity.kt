package com.example.rmas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.example.rmas.ui.theme.RMASTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rmas.database.FirebaseDatabase
import com.example.rmas.services.CameraService

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RMASTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignupScreen(
                        /* TODO:    Set firestore writing rule to true only to logged users and
                         *          fill username - email collection
                         */
                        onRegisterClick = { email, password, username, fullName, phoneNumber, image ->
                            FirebaseDatabase.createAccount(email, password, username, fullName, phoneNumber, image, {
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }) {
                                Toast.makeText(this, "Couldn't create account", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        onLoginButton = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignupScreen(onRegisterClick: (String, String, String, String, String, ImageBitmap) -> Unit, onLoginButton: () -> Unit) {
    val context = LocalContext.current.applicationContext

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    val icon = if(passwordVisible)
        painterResource(id = R.drawable.visible)
    else
        painterResource(id = R.drawable.visible_off)

    val cameraService = CameraService(context)
    cameraService.Setup {
        imageUri = it
        Log.e("CAMERA", "RegisterScreen: $it", )
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Profile Picture
        Surface(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(150.dp)
                .border(
                    width = 3.dp,
                    color = Color(0xFFB5485D),
                    shape = CircleShape
                )
                .clip(CircleShape)
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Username
        OutlinedTextField(
            modifier = Modifier
                .width(250.dp),
            value = fullName,
            onValueChange = { fullName = it },
            singleLine = true,
            label = { Text("Ime i prezime") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            ),
        )

        // Full Name
        OutlinedTextField(
            modifier = Modifier
                .width(250.dp),
            value = username,
            onValueChange = { username = it },
            singleLine = true,
            label = { Text("Korisničko ime") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            ),
        )

        // Email address
        OutlinedTextField(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 8.dp),
            value = email,
            onValueChange = { email = it },
            singleLine = true,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
        )

        // Password
        OutlinedTextField(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 8.dp),
            value = password,
            onValueChange = { password = it },
            singleLine = true,
            label = { Text("Lozinka") },
            visualTransformation = if(passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },

            trailingIcon={
                IconButton(onClick={
                    passwordVisible=!passwordVisible;
                }){
                    Icon(painter= icon,
                        contentDescription="Visibility icon")
                }
            }

            // Please provide localized description for accessibility services



        )

        // Phone Number
        OutlinedTextField(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 8.dp),
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            singleLine = true,
            label = { Text("Kontakt telefon") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
        )

        Row(
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
                    .clickable{
                            cameraService.uploadPicture()
                    }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
                    .clickable{
                        cameraService.takePicture()
                    }
            )
        }
        Button(
            onClick = { onRegisterClick(email, password, username, fullName, phoneNumber, cameraService.loadImageBitmapFromUri(imageUri)) },
            modifier = Modifier
                .width(300.dp)
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFB5485D), contentColor = Color.White
            )


        ) {
            Text(
                "Signup",
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Imate nalog?", style = TextStyle(fontSize = 16.sp))
                Spacer(modifier = Modifier.width(8.dp))
                ClickableText(
                    text = AnnotatedString("Log in"),
                    onClick = { onLoginButton() },
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }


}

@Preview
@Composable
fun GreetingPreview() {
    RMASTheme {
        SignupScreen(
            onLoginButton = {},
            onRegisterClick = { _, _, _, _, _, _ ->}
        )
    }
}