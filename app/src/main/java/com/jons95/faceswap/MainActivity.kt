package com.jons95.faceswap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jons95.faceswap.ui.FaceSwapViewModel
import com.jons95.faceswap.ui.MainScreen
import com.jons95.faceswap.ui.theme.FaceSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceSwapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: FaceSwapViewModel = viewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
