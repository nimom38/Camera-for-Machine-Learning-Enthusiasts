package com.example.android.aicamera

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.android.aicamera.databinding.FragmentInfoBinding
import com.google.android.material.snackbar.Snackbar

private const val ARG_PARAM = "type"


class InfoFragment : Fragment() {
    private lateinit var binding: FragmentInfoBinding
    private var param: String? = null
    private lateinit var description: String
    private lateinit var title: String
    private lateinit var direction: NavDirections

    private lateinit var safeContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param = it.getString(ARG_PARAM)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//        val mOrientationListener: OrientationEventListener = object : OrientationEventListener(
//            safeContext
//        ) {
//            override fun onOrientationChanged(orientation: Int) {
//                if (orientation == 0) {
//                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                }
//                else if(orientation == 180) {
//                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
//                }
//                else if (orientation == 90) {
//                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
//                }
//                else if (orientation == 270) {
//                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//                }
//            }
//        }

        binding = FragmentInfoBinding.inflate(layoutInflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            var isToolbarShown = false

            // scroll change listener begins at Y = 0 when image is fully collapsed
            aicameraDetailScrollview.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->

                    // User scrolled past image to height of toolbar and the title text is
                    // underneath the toolbar, so the toolbar should be shown.
                    val shouldShowToolbar = scrollY > toolbar.height

                    // The new state of the toolbar differs from the previous state; update
                    // appbar and toolbar attributes.
                    if (isToolbarShown != shouldShowToolbar) {
                        isToolbarShown = shouldShowToolbar

                        // Use shadow animator to add elevation if toolbar is shown
                        appbar.isActivated = shouldShowToolbar

                        // Show the plant name if toolbar is shown
                        toolbarLayout.isTitleEnabled = shouldShowToolbar
                    }
                }
            )

            setHasOptionsMenu(true)
            toolbar.setNavigationOnClickListener { view ->
                view.findNavController().navigateUp()
            }

            if(param == "object detection") {
                description = "<h4>This is Object Detection</h4>"
                title = "Object Detection"
                direction = InfoFragmentDirections.actionInfoFragmentToObjCamera()
            }
            else if(param == "image labelling") {
                description = "<h4>This is Image Labelling</h4>"
                title = "Image Labelling"
                direction = InfoFragmentDirections.actionInfoFragmentToImageLabelling()
            }
            else if(param == "pose estimation") {
                description = "<h4>This is Pose Estimation</h4>"
                title = "Pose Estimation"
                direction = InfoFragmentDirections.actionInfoFragmentToPoseEstimation()
            }
            else if(param == "face detection") {
                description = "<h4>This is Face Detection</h4>"
                title = "Face Detection"
                direction = InfoFragmentDirections.actionInfoFragmentToFaceDetection()
            }

            aicameraDescription.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(description)
            }

            fab.setOnClickListener{
                findNavController().navigate(direction)
            }

            aicameraDetailName.text = title

        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

}