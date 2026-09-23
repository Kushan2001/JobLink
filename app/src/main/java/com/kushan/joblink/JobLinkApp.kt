package com.kushan.joblink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kushan.joblink.navigation.JobLinkNavHost
import com.kushan.joblink.ui.theme.JobLinkTheme

@Composable
fun JobLinkApp() {
    JobLinkTheme {
        val navController = rememberNavController()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            JobLinkNavHost(navController = navController)
        }
    }
}
