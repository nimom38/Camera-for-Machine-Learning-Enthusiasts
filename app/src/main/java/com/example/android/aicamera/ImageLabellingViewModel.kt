package com.example.android.aicamera

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class ImageLabellingViewModel(safeContext: Application) : AndroidViewModel(safeContext) {
    var prothom: Boolean = true
    var isFlash: Boolean = false
    var which_camera: Int = 0
    val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)
    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

    val options = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.7f)
        .build()
    val labeler = ImageLabeling.getClient(options)


    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImageLabellingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ImageLabellingViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}