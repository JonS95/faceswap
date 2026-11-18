package com.jons95.faceswap.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jons95.faceswap.R
import com.jons95.faceswap.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FaceSwapViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pickSource = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onSourcePicked(it) }
    }
    val pickTarget = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onTargetPicked(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ImagePickerCard(
                    title = stringResource(R.string.source_face),
                    imageUri = uiState.sourceUri,
                    onClick = {
                        pickSource.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )
                ImagePickerCard(
                    title = stringResource(R.string.target_photo),
                    imageUri = uiState.targetUri,
                    onClick = {
                        pickTarget.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = { viewModel.swap() },
                enabled = uiState.canSwap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.swap_faces))
            }

            if (uiState.isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "  " + stringResource(R.string.processing),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = androidx.compose.ui.graphics.Color.Red)
            }

            uiState.resultBitmap?.let { bitmap ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.result_title))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.result_title),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.saveResult() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.save))
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = ImageUtils.shareableUriFor(context, bitmap)
                                context.startActivity(
                                    Intent.createChooser(ImageUtils.shareIntentFor(uri), "Share")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.share))
                        }
                    }
                    uiState.savedMessage?.let { Text(it) }
                    OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.start_over))
                    }
                }
            }
        }
    }
}
