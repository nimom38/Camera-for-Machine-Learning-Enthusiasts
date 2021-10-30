package com.example.android.aicamera

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseEstimationViewModel (safeContext: Application) : AndroidViewModel(safeContext) {
    var prothom: Boolean = true
    var isFlash: Boolean = false
    val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)
    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

    val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()


    val poseDetector = PoseDetection.getClient(options)


    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PoseEstimationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PoseEstimationViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}