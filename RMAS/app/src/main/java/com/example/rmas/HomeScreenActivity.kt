package com.example.rmas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.example.rmas.ui.theme.RMASTheme


class HomeScreenActivity : ComponentActivity(){

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            RMASTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    HomeScreen(
                        onClickButton = {
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                    )
                }
            }
        }
    }

}
@Composable
fun HomeScreen(onClickButton : ()-> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Login image",
            modifier = Modifier.size(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
               onClickButton()
            },
            modifier = Modifier.width(200.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFDC1C5), contentColor = Color(0xFFB5485D))
        ) {
            Text(text = "LOGIN")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Nemate nalog?", color = Color(0xFFB5485D))

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {},
            modifier = Modifier.width(200.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFDC1C5), contentColor = Color(0xFFB5485D))
        ) {
            Text(text = "SIGNUP")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    RMASTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ){
            HomeScreen({})
        }
    }
}