package com.down.adm_core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.kaaveh.sdpcompose.sdp
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var textUrl by remember {
            mutableStateOf("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8")
        }
        var fileName by remember {
            mutableStateOf(System.currentTimeMillis().toString() + ".mp4")
        }
        Text("Main Screen")
        VerticalSpacer()
        TextField(
            value = fileName,
            onValueChange = {
                fileName = it
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        TextField(
            value = textUrl,
            onValueChange = {
                textUrl = it
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        Button(onClick = {
            viewModel.download(context, fileName, textUrl)
        }) {
            Text("Download")
        }
        Button(onClick = {
            viewModel.merge(context)
        }) {
            Text("Merge")
        }
        Button(onClick = {
            viewModel.pause(context)
        }) {
            Text("Pause")
        }
        Button(onClick = {
            viewModel.resume(context)
        }) {
            Text("Resume")
        }
        VerticalSpacer(20)
        Text(state.progress.toString())
        LinearProgressIndicator(
            progress = {
                state.progress
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        VerticalSpacer()
        Text(state.status.toString())
    }
}

@Composable
fun VerticalSpacer(height: Int = 10) {
    Modifier.height(height.sdp)
}