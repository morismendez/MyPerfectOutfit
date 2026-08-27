package com.myperfectoutfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.myperfectoutfit.ui.screens.MainScreen
import com.myperfectoutfit.ui.theme.MyPerfectOutfitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyPerfectOutfitTheme {
                MainScreen()
            }
        }
    }
}