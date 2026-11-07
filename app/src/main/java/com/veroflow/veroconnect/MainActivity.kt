package com.veroflow.veroconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.navigation.AppNavGraph
import com.veroflow.veroconnect.ui.theme.VeroConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Backend.init(applicationContext)
        MockRepository.init(applicationContext)

        setContent {
            val darkMode by MockRepository.darkMode
            VeroConnectTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}
