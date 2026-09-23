package com.kushan.joblink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kushan.joblink.ui.screens.HomeScreen
import com.kushan.joblink.ui.theme.JobLinkTheme

@Composable
fun JobLinkApp() {
    JobLinkTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            HomeScreen()
        }
    }
}
