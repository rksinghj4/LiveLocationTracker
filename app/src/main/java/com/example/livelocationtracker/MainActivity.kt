package com.example.livelocationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.livelocationtracker.presentation.location.LocationScreen
import com.example.livelocationtracker.presentation.theme.LiveLocationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveLocationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocationScreen()
                }
            }
        }
    }
}