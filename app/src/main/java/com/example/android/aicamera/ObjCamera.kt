package com.example.android.aicamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.android.aicamera.databinding.FragmentObjCameraBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.withTestContext
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
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
import android.view.OrientationEventListener
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext


val MODEL: String = "object_labeler.tflite"

class ObjCamera : Fragment() {

    private lateinit var binding: FragmentObjCameraBinding

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)


//    private lateinit var cameraExecutor: ExecutorService
    private var graphicOverlay: GraphicOverlay? = null

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var safeContext: Context

    private val viewModel: ObjCameraViewModel by lazy {
        ViewModelProvider(this, ObjCameraViewModel.Factory(activity!!.application))
            .get(ObjCameraViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context

        val mOrientationListener: OrientationEventListener = object : OrientationEventListener(
            safeContext
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == 0) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                else if(orientation == 180) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                }
                else if (orientation == 90) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
                else if (orientation == 270) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        Log.d("Baby", "oncreateview")
        binding = FragmentObjCameraBinding.inflate(layoutInflater, container, false)
        graphicOverlay = binding.graphicOverlay
        setHasOptionsMenu(true)
        binding.objToolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp()
        }
        binding.objToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_camera_flip -> {
                    Log.d("Baby", "item")
                    viewModel.which_camera = 1 - viewModel.which_camera
                    if(viewModel.which_camera == 0) {
                        binding.objToolbar.menu.findItem(R.id.action_flash).setVisible(false)
                        viewModel.isFlash = false
                    }
                    else {
                        binding.objToolbar.menu.findItem(R.id.action_flash).setVisible(true);
                        binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_off_white)
                    }
                    bindUseCases(viewModel.which_camera, viewModel.isFlash)
                    true
                }
                R.id.action_flash -> {
                    Log.d("Baby", "item")
                    if (viewModel.isFlash) {
                        item.setIcon(R.drawable.flash_off_white)
                        viewModel.isFlash = false
                    } else {
                        item.setIcon(R.drawable.flash_on_white)
                        viewModel.isFlash = true
                    }
                    bindUseCases(viewModel.which_camera, viewModel.isFlash)
                    true
                }
                R.id.action_info -> {
                    Toast.makeText( safeContext, "Point your camera forward infront of the objects.", Toast.LENGTH_SHORT ).show()
                    true
                }
                else -> false
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Baby", "onviewcreated")
        if(viewModel.prothom) {
            viewModel.isFlash = false
            viewModel.which_camera = 1
            viewModel.prothom = false
        }
        if(viewModel.which_camera == 0) {
            binding.objToolbar.menu.findItem(R.id.action_flash).setVisible(false)
            viewModel.isFlash = false
        }
        if(viewModel.isFlash == true) {
            binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_on_white)
        }
        if((viewModel.isFlash == false) && (viewModel.which_camera == 1)) {
            binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_off_white)
        }
        bindUseCases(viewModel.which_camera, viewModel.isFlash)

//        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun bindUseCases(which_camera: Int, isFlashOn: Boolean) {
        var needUpdateGraphicOverlayImageSourceInfo: Boolean = true
        viewModel.cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner

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
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            val isImageFlipped = which_camera == CameraSelector.LENS_FACING_FRONT
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay!!.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                            } else {
                                graphicOverlay!!.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                        try {
                            processImageProxy(imageProxy, graphicOverlay)
                        } catch (e: MlKitException) {
                            Log.d("ObjCamera", "alalalaal")
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
                viewModel.cameraProvider.unbindAll()

                // Bind use cases to camera
                viewModel.cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer).cameraControl.enableTorch(isFlashOn)

            } catch(exc: Exception) {
                Log.d("ObjCamera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy, graphicOverlay: GraphicOverlay?) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//            lifecycleScope.launch {
//                withContext(Dispatchers.Default) {
                    val result = viewModel.objectDetector.process(image)
                        .addOnSuccessListener(executor, OnSuccessListener { results ->
                            graphicOverlay!!.clear()
                            for (detectedObject in results) {
                                graphicOverlay.add(ObjectGraphic(graphicOverlay, detectedObject))
                            }
                            graphicOverlay.postInvalidate()
                        })
                        .addOnFailureListener(executor, OnFailureListener { e ->
                            Log.d("ObjCamera", "Face detection failed $e")
                        })
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
//                }
//            }


        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("ObjCamera", "executor shutdown")
//        cameraExecutor.shutdown()
        executor.shutdown()
        Log.d("ObjCamera", "FragmentA destroyed")
    }

    override fun onDetach() {
        super.onDetach()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}