package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppNavigationWrapper
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.QuestViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: QuestViewModel = viewModel()
      MyApplicationTheme(darkTheme = viewModel.isDarkMode.value) {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppNavigationWrapper(viewModel)
        }
      }
    }
  }
}
