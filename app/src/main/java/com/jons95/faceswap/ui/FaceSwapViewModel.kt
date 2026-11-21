package com.jons95.faceswap.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jons95.faceswap.ml.FaceDetector
import com.jons95.faceswap.pipeline.SwapPipeline
import com.jons95.faceswap.pipeline.SwapResult
import com.jons95.faceswap.util.FaceAdjuster
import com.jons95.faceswap.util.FaceAdjustments
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
    val adjustedBitmap: Bitmap? = null,
    val targetBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val processingStep: String = "",
    val errorMessage: String? = null,
    val savedMessage: String? = null,
    val showOriginal: Boolean = false,
    val adjustments: FaceAdjustments = FaceAdjustments(),
    val showEditControls: Boolean = false
) {
    val canSwap: Boolean get() = sourceUri != null && targetUri != null && !isProcessing
    val hasResult: Boolean get() = resultBitmap != null
    val displayBitmap: Bitmap? get() = when {
        showOriginal && targetBitmap != null -> targetBitmap
        adjustedBitmap != null -> adjustedBitmap
        else -> resultBitmap
    }
    val saveBitmap: Bitmap? get() = adjustedBitmap ?: resultBitmap
}

class FaceSwapViewModel(application: Application) : AndroidViewModel(application) {

    private val faceDetector = FaceDetector()
    private val pipeline = SwapPipeline(faceDetector)

    private val _uiState = MutableStateFlow(FaceSwapUiState())
    val uiState: StateFlow<FaceSwapUiState> = _uiState

    fun onSourcePicked(uri: Uri) {
        _uiState.update {
            it.copy(sourceUri = uri, resultBitmap = null, adjustedBitmap = null,
                targetBitmap = null, errorMessage = null, savedMessage = null,
                showOriginal = false, adjustments = FaceAdjustments(), showEditControls = false)
        }
    }

    fun onTargetPicked(uri: Uri) {
        _uiState.update {
            it.copy(targetUri = uri, resultBitmap = null, adjustedBitmap = null,
                targetBitmap = null, errorMessage = null, savedMessage = null,
                showOriginal = false, adjustments = FaceAdjustments(), showEditControls = false)
        }
    }

    fun swapDirection() {
        _uiState.update {
            it.copy(
                sourceUri = it.targetUri,
                targetUri = it.sourceUri,
                resultBitmap = null,
                adjustedBitmap = null,
                targetBitmap = null,
                errorMessage = null,
                savedMessage = null,
                showOriginal = false,
                adjustments = FaceAdjustments(),
                showEditControls = false
            )
        }
    }

    fun toggleOriginal() {
        _uiState.update { it.copy(showOriginal = !it.showOriginal) }
    }

    fun swap() {
        val state = _uiState.value
        val sourceUri = state.sourceUri ?: return
        val targetUri = state.targetUri ?: return

        _uiState.update { it.copy(isProcessing = true, processingStep = "Loading photos…",
            errorMessage = null, savedMessage = null) }

        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val (source, target) = withContext(Dispatchers.Default) {
                    val s = ImageUtils.loadBitmap(context, sourceUri)
                    val t = ImageUtils.loadBitmap(context, targetUri)
                    Pair(s, t)
                }
                if (source == null || target == null) {
                    _uiState.update { it.copy(isProcessing = false, processingStep = "",
                        errorMessage = "Could not load one of the photos.") }
                    return@launch
                }

                _uiState.update { it.copy(processingStep = "Detecting faces…") }
                val outcome = withContext(Dispatchers.Default) {
                    pipeline.run(source, target)
                }

                when (outcome) {
                    is SwapResult.Success -> _uiState.update {
                        it.copy(isProcessing = false, processingStep = "",
                            resultBitmap = outcome.bitmap, targetBitmap = target)
                    }
                    is SwapResult.NoFaceInSource -> _uiState.update {
                        it.copy(isProcessing = false, processingStep = "",
                            errorMessage = "No face detected in the source photo. Try a clearer front-facing photo.")
                    }
                    is SwapResult.NoFaceInTarget -> _uiState.update {
                        it.copy(isProcessing = false, processingStep = "",
                            errorMessage = "No face detected in the target photo. Try a clearer front-facing photo.")
                    }
                    is SwapResult.Failed -> _uiState.update {
                        it.copy(isProcessing = false, processingStep = "",
                            errorMessage = outcome.message)
                    }
                }
            } catch (e: OutOfMemoryError) {
                _uiState.update {
                    it.copy(isProcessing = false, processingStep = "",
                        errorMessage = "Photos are too large. Try smaller images.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, processingStep = "",
                        errorMessage = "Swap failed: ${e.message}")
                }
            }
        }
    }

    fun saveResult() {
        val bitmap = _uiState.value.saveBitmap ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uri = withContext(Dispatchers.IO) { ImageUtils.saveToGallery(context, bitmap) }
            _uiState.update {
                it.copy(savedMessage = if (uri != null) "Saved to gallery!" else "Failed to save")
            }
        }
    }

    fun prepareShare(onReady: (Intent) -> Unit) {
        val bitmap = _uiState.value.saveBitmap ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uri = withContext(Dispatchers.IO) { ImageUtils.shareableUriFor(context, bitmap) }
            onReady(ImageUtils.shareIntentFor(uri))
        }
    }

    fun toggleEditControls() {
        _uiState.update { it.copy(showEditControls = !it.showEditControls) }
    }

    fun onAdjustmentsChanged(adjustments: FaceAdjustments) {
        _uiState.update { it.copy(adjustments = adjustments) }
        applyAdjustments(adjustments)
    }

    fun resetAdjustments() {
        val defaults = FaceAdjustments()
        _uiState.update { it.copy(adjustments = defaults, adjustedBitmap = null) }
    }

    private fun applyAdjustments(adjustments: FaceAdjustments) {
        val result = _uiState.value.resultBitmap ?: return
        val target = _uiState.value.targetBitmap ?: return
        viewModelScope.launch {
            val adjusted = withContext(Dispatchers.Default) {
                FaceAdjuster.apply(result, target, adjustments)
            }
            _uiState.update { state ->
                val old = state.adjustedBitmap
                if (old != null && old !== state.resultBitmap) old.recycle()
                state.copy(adjustedBitmap = adjusted)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, savedMessage = null) }
    }

    fun reset() {
        _uiState.value = FaceSwapUiState()
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
    }
}
