package com.example.rmas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rmas.database.Firebase
import com.example.rmas.ui.theme.RMASTheme


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RMASTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginClick = { username, password ->
                            Firebase.login(username, password, {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            },
                                {
                                    Toast.makeText(
                                        this,
                                        "Uneli ste nevalidno korisnicko ime ili lozinku",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        },
                        onChangePasswordText = {
                            startActivity(Intent(this, MainActivity::class.java))
                        })
                }
            }
        }
    }
}


@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit, onChangePasswordText: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var invalidUsername by remember { mutableStateOf(false) }
    var invalidPassword by remember { mutableStateOf(false) }

    val icon = if(passwordVisible)
        painterResource(id = R.drawable.visible)
    else
        painterResource(id = R.drawable.visible_off)


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_image),
            contentDescription = "Login image",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = "Dobrodošli nazad",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB5485D)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "Ulogujte se", color = Color(0xFFFB607F))

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; invalidUsername= false },
            label = { Text(text = "Korisničko ime") },
            isError = invalidUsername
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; invalidPassword = false },
            label = { Text(text = "Lozinka") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = invalidPassword,
            trailingIcon = {
                IconButton(onClick = {
                    passwordVisible = !passwordVisible
                }){
                    Icon(
                        painter = icon,
                        contentDescription = "Visibility icon"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )


        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if(username == "")
            {
                invalidUsername = true
            }

            if(password == "")
            {
                invalidPassword = true
            }

            if(username!= "" && password != ""){
                onLoginClick(username, password)
            }
        },
            modifier = Modifier.width(200.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFB5485D), contentColor = Color.White
            )) {
            Text(text = "Login")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Zaboravili ste lozinku?",
            modifier = Modifier.clickable { onChangePasswordText() },
            color = Color(0xFFFB607F)
        )
    }

}


@Preview
@Composable
fun Preview() {
    RMASTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ){
            LoginScreen({_, _->}, {})
        }
    }

}
