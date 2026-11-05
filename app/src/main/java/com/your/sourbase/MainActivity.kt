package com.your.sourbase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.your.sourbase.navigation.AppNavGraph
import com.your.sourbase.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
/** Hosts the application theme and saved navigation stack. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { AppNavGraph() } }
    }
}
