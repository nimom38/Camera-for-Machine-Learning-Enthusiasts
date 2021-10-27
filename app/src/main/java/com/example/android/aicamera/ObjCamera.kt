package com.example.android.aicamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.example.android.aicamera.databinding.FragmentObjCameraBinding
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val MODEL: String = "detect.tflite"
val LABEL: String =  "labelmap.txt"

class ObjCamera : Fragment() {

    private lateinit var binding: FragmentObjCameraBinding

    private var needUpdateGraphicOverlayImageSourceInfo: Boolean = true
    private lateinit var cameraExecutor: ExecutorService
    private var graphicOverlay: GraphicOverlay? = null


    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var safeContext: Context

    private var imageRotationDegrees: Int = 0
    private val tfImageBuffer = TensorImage(DataType.UINT8)
    private lateinit var bitmapBuffer: Bitmap
    private var lensFacing: Int = 0
    private lateinit var imageAnalyzerSize: Size

    private val tfImageProcessor by lazy {
        val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    // nnAPiDelegate must be released by explicitly calling its close() function.
    //     https://github.com/android/camera-samples/issues/417
    private var nnapiInitialized = false
    private val nnApiDelegate by lazy  {
        NnApiDelegate().apply { nnapiInitialized = true }
    }

    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(safeContext, MODEL),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    private val detector by lazy {
        ObjectDetectionHelper(
            tflite,
            FileUtil.loadLabels(safeContext, LABEL)
        )
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentObjCameraBinding.inflate(layoutInflater, container, false)
        graphicOverlay = binding.graphicOverlay
        setHasOptionsMenu(true)
        binding.objToolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindUseCases(1, false)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    @SuppressLint("RestrictedApi")
    private fun bindUseCases(which_camera: Int, isFlashOn: Boolean) {
        lensFacing = which_camera
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

            imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also {
                it.setAnalyzer(
                    // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                    // thus we can just runs the analyzer itself on main thread.
                    ContextCompat.getMainExecutor(safeContext),
                    ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
                            bitmapBuffer = Bitmap.createBitmap(
                                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                        }
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            val isImageFlipped = which_camera == CameraSelector.LENS_FACING_FRONT
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay!!.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
//                                graphicOverlay!!.setImageSourceInfo(binding.previewView.width, binding.previewView.height, isImageFlipped)
                            } else {
                                graphicOverlay!!.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
//                                graphicOverlay!!.setImageSourceInfo(binding.previewView.height, binding.previewView.width, isImageFlipped)
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                        try {
                            processImageProxy(imageProxy, graphicOverlay)
                        } catch (e: Exception) {
                            Log.e("ObjCamera", "Failed to process image. Error: " + e.localizedMessage)
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

                imageAnalyzerSize = imageAnalyzer!!.attachedSurfaceResolution ?: Size(0, 0)

            } catch(exc: Exception) {
                Log.e("ObjCamera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy, graphicOverlay: GraphicOverlay?) {
        // Copy out RGB bits to our shared buffer
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)  }

        // Process the image in Tensorflow
        val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })
        Log.d("ObjCamera", "lululul")

        // Perform the object detection for the current frame
        val predictions = detector.predict(tfImage).toMutableList()
        Log.d("ObjCamera", "lululul2")
        var idx = -1
        for(item in predictions) {
            ++idx
            predictions[idx].location = mapOutputCoordinates(item.location, imageProxy.width, imageProxy.height)
//            predictions[idx].location = mapOutputCoordinates(item.location, binding.previewView.width, binding.previewView.height)
        }
        graphicOverlay!!.clear()
        for(item in predictions) {
            if(item.score < 0.5) continue
            graphicOverlay.add(ObjectGraphic(graphicOverlay, item))
        }
        graphicOverlay.postInvalidate()
        imageProxy.close()
    }

    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
    private fun mapOutputCoordinates(location: RectF, width: Int, height: Int): RectF {

        // Step 1: map location to the preview coordinates
        val previewLocation = RectF(
            location.left * width,
            location.top * height,
            location.right * width,
            location.bottom * height
        )

        // Step 2: compensate for camera sensor orientation and mirroring
        val isFrontFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
        val correctedLocation = if (isFrontFacing) {
            RectF(
                width - previewLocation.right,
                previewLocation.top,
                width - previewLocation.left,
                previewLocation.bottom)
        } else {
            previewLocation
        }

        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = width/height
        val midX = (correctedLocation.left + correctedLocation.right) / 2f
        val midY = (correctedLocation.top + correctedLocation.bottom) / 2f
        return if (width < height) {
            RectF(
                midX - (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY - (1f - margin) * correctedLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY + (1f - margin) * correctedLocation.height() / 2f
            )
        } else {
            RectF(
                midX - (1f - margin) * correctedLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * correctedLocation.height() / 2f,
                midX + (1f - margin) * correctedLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * correctedLocation.height() / 2f
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("ObjCamera", "executor shutdown")
        cameraExecutor.shutdown()

        Log.d("ObjCamera", "FragmentA destroyed")
    }
}