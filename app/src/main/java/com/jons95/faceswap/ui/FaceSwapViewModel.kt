package com.jons95.faceswap.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jons95.faceswap.ml.FaceDetector
import com.jons95.faceswap.pipeline.SwapPipeline
import com.jons95.faceswap.pipeline.SwapResult
import com.jons95.faceswap.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FaceSwapUiState(
    val sourceUri: Uri? = null,
    val targetUri: Uri? = null,
    val resultBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val savedMessage: String? = null
) {
    val canSwap: Boolean get() = sourceUri != null && targetUri != null && !isProcessing
}

class FaceSwapViewModel(application: Application) : AndroidViewModel(application) {

    private val faceDetector = FaceDetector()
    private val pipeline = SwapPipeline(faceDetector)

    private val _uiState = MutableStateFlow(FaceSwapUiState())
    val uiState: StateFlow<FaceSwapUiState> = _uiState

    fun onSourcePicked(uri: Uri) {
        _uiState.update { it.copy(sourceUri = uri, resultBitmap = null, errorMessage = null, savedMessage = null) }
    }

    fun onTargetPicked(uri: Uri) {
        _uiState.update { it.copy(targetUri = uri, resultBitmap = null, errorMessage = null, savedMessage = null) }
    }

    fun swap() {
        val state = _uiState.value
        val sourceUri = state.sourceUri ?: return
        val targetUri = state.targetUri ?: return

        _uiState.update { it.copy(isProcessing = true, errorMessage = null, savedMessage = null) }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val outcome = withContext(Dispatchers.Default) {
                val source = ImageUtils.loadBitmap(context, sourceUri)
                val target = ImageUtils.loadBitmap(context, targetUri)
                if (source == null || target == null) {
                    SwapResult.Failed("Could not load one of the photos.")
                } else {
                    pipeline.run(source, target)
                }
            }

            when (outcome) {
                is SwapResult.Success -> _uiState.update {
                    it.copy(isProcessing = false, resultBitmap = outcome.bitmap)
                }
                is SwapResult.NoFaceInSource -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "No face detected in the source photo.")
                }
                is SwapResult.NoFaceInTarget -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "No face detected in the target photo.")
                }
                is SwapResult.Failed -> _uiState.update {
                    it.copy(isProcessing = false, errorMessage = outcome.message)
                }
            }
        }
    }

    fun saveResult() {
        val bitmap = _uiState.value.resultBitmap ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uri = withContext(Dispatchers.IO) { ImageUtils.saveToGallery(context, bitmap) }
            _uiState.update {
                it.copy(savedMessage = if (uri != null) "Saved to gallery" else "Failed to save")
            }
        }
    }

    fun reset() {
        _uiState.value = FaceSwapUiState()
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
    }
}
