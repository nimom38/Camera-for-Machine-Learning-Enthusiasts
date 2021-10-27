package com.example.android.aicamera

import android.app.Application
import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.*
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class ObjCameraViewModel(safeContext: Application) : AndroidViewModel(safeContext) {
//    var isFlash = MutableLiveData<Boolean>(false)
//    var which_camera = MutableLiveData<Int>(0)
    var prothom: Boolean = true
    var isFlash: Boolean = false
    var which_camera: Int = 0
    val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)
    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

    val localModel = LocalModel.Builder()
        .setAssetFilePath(MODEL)
        .build()
    val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(10)
            .build()

    val objectDetector =
        ObjectDetection.getClient(customObjectDetectorOptions)



    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ObjCameraViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ObjCameraViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}