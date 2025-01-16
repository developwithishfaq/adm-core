package com.down.adm_core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adm.core.InProgressVideoUi
import com.adm.core.components.DownloadingState
import ir.kaaveh.sdpcompose.sdp
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress: List<InProgressVideoUi> by viewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var textUrl by remember {
            mutableStateOf("")
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

        VerticalSpacer(20)

        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(progress) {
                Text(text = it.fileName)
                Text(it.progress.toString())
                LinearProgressIndicator(
                    progress = {
                        if (it.progress.isNaN()) {
                            0f
                        } else
                            it.progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                VerticalSpacer()
                Text(it.status.toString())
                Row {
                    Button(onClick = {
                        if (it.status == DownloadingState.Paused) {
                            viewModel.resume(it.id.toLong())
                        } else {

                            viewModel.pause(it.id.toLong())
                        }

                    }) {
                        Text(
                            text = if (it.status == DownloadingState.Paused) "Resume" else "Pause"
                        )
                    }

                    Button(onClick = {
                        viewModel.resume(it.id.toLong())
                    }) {
                        Text(text = "Restart")
                    }

                    Button(onClick = {
                        viewModel.pause(it.id.toLong())
                    }) {
                        Text(text = "Cancel")
                    }
                }
            }
        }

    }
}

@Composable
fun VerticalSpacer(height: Int = 10) {
    Modifier.height(height.sdp)
}