package com.example.android.aicamera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.example.android.aicamera.databinding.FragmentFaceDetectionBinding
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.internal.Contexts.getApplication
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetection : Fragment() {
    private lateinit var binding: FragmentFaceDetectionBinding
    private var needUpdateGraphicOverlayImageSourceInfo: Boolean = true
    private lateinit var cameraExecutor: ExecutorService
    private var graphicOverlay: GraphicOverlay? = null


    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFaceDetectionBinding.inflate(layoutInflater, container, false)
        graphicOverlay = binding.graphicOverlay
        setHasOptionsMenu(true)
        binding.objToolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp()
        }
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindUseCases(0, false)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun bindUseCases(which_camera: Int, isFlashOn: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            Log.d("FaceDetection", "huhuhuh")

            imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(
                    // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                    // thus we can just runs the analyzer itself on main thread.
                    ContextCompat.getMainExecutor(safeContext),
                    ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        Log.d("FaceDetection", "huhuhuh2")
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            val isImageFlipped = which_camera == CameraSelector.LENS_FACING_FRONT
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                Log.d("FaceDetection", "huhuhuh4")
                            graphicOverlay!!.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                            } else {
                                Log.d("FaceDetection", "huhuhuh5")
                            graphicOverlay!!.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
                                Log.d("FaceDetection", "huhuhuh6")
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                    try {
                        Log.d("FaceDetection", "huhuhuh3")
                        processImageProxy(imageProxy, graphicOverlay)
                    } catch (e: MlKitException) {
                        Log.e("FaceDetection", "Failed to process image. Error: " + e.localizedMessage)
                        Toast.makeText(safeContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    }
                )
            }

//             Select back camera as a default
            val cameraSelector = if (which_camera == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            } else {
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer).cameraControl.enableTorch(isFlashOn)

            } catch(exc: Exception) {
                Log.e("FaceDetection", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy, graphicOverlay: GraphicOverlay?) {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(highAccuracyOpts)

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    graphicOverlay!!.clear()
                    for (face in faces) {
                        graphicOverlay.add(FaceGraphic(graphicOverlay, face))
                    }
                    graphicOverlay.postInvalidate()
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetection", "Face detection failed $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("FaceDetection", "executor shutdown")
        cameraExecutor.shutdown()

        Log.d("FaceDetection", "FragmentA destroyed")
    }
}