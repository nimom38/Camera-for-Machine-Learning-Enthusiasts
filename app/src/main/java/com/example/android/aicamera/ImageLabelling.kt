package com.example.android.aicamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.android.aicamera.databinding.FragmentImageLabellingBinding
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ImageLabelling : Fragment() {
    private lateinit var binding: FragmentImageLabellingBinding

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var safeContext: Context

    private lateinit var arr: ArrayList<TextView>

    private val viewModel: ImageLabellingViewModel by lazy {
        ViewModelProvider(this, ImageLabellingViewModel.Factory(activity!!.application))
            .get(ImageLabellingViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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

        binding = FragmentImageLabellingBinding.inflate(layoutInflater, container, false)
        arr = arrayListOf(
            binding.tv1,
            binding.tv2,
            binding.tv3,
            binding.tv4,
            binding.tv5,
            binding.tv6,
            binding.tv7,
            binding.tv8,
            binding.tv9,
            binding.tv10,
        )
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
                        binding.objToolbar.menu.findItem(R.id.action_flash).setVisible(true)
                        binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_off)
                    }
                    bindUseCases(viewModel.which_camera, viewModel.isFlash)
                    true
                }
                R.id.action_flash -> {
                    Log.d("Baby", "item")
                    if (viewModel.isFlash) {
                        item.setIcon(R.drawable.flash_off)
                        viewModel.isFlash = false
                    } else {
                        item.setIcon(R.drawable.flash_on)
                        viewModel.isFlash = true
                    }
                    bindUseCases(viewModel.which_camera, viewModel.isFlash)
                    true
                }
                else -> false
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_on)
        }
        if((viewModel.isFlash == false) && (viewModel.which_camera == 1)) {
            binding.objToolbar.menu.findItem(R.id.action_flash).setIcon(R.drawable.flash_off)
        }
        bindUseCases(viewModel.which_camera, viewModel.isFlash)

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
                        try {
                            processImageProxy(imageProxy)
                        } catch (e: MlKitException) {
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
                Log.e("FaceDetection", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val result = viewModel.labeler.process(image)
                .addOnSuccessListener(executor, { labels ->
                    var idx = 0
                    while(idx < 10) {
                        arr[idx++].text = ""
                    }
                    idx = 0
                    for (label in labels) {
                        if(idx == 10) break
                        val text = label.text
                        val confidence = label.confidence
                        val index = label.index
                        arr[idx++].text = text + ": " + confidence
                    }
                })
                .addOnFailureListener(executor, { e ->
                    Log.e("ImageLabelling", "Face detection failed $e")
                })
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("ImageLabelling", "executor shutdown")

        Log.d("ImageLabelling", "FragmentA destroyed")
    }

    override fun onDetach() {
        super.onDetach()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}