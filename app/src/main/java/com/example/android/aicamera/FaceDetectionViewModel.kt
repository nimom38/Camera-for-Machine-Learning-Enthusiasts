package com.example.android.aicamera

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionViewModel(safeContext: Application) : AndroidViewModel(safeContext) {
    var prothom: Boolean = true
    var isFlash: Boolean = false
    var which_camera: Int = 0
    val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)
    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(highAccuracyOpts)

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FaceDetectionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FaceDetectionViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}