package com.example.android.aicamera

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.android.aicamera.databinding.FragmentPapaBinding

class PapaFragment : Fragment() {

    private lateinit var binding: FragmentPapaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = FragmentPapaBinding.inflate(layoutInflater, container, false)

        binding.objectDetectionInfo.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToInfoFragment("object detection")
            findNavController().navigate(direction)
        }

        binding.imageLabellingInfo.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToInfoFragment("image labelling")
            findNavController().navigate(direction)
        }

        binding.poseEstimationInfo.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToInfoFragment("pose estimation")
            findNavController().navigate(direction)
        }

        binding.faceDetectionInfo.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToInfoFragment("face detection")
            findNavController().navigate(direction)
        }

        binding.objectDetectionPlay.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToObjCamera()
            findNavController().navigate(direction)
        }

        binding.imageLabellingPlay.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToImageLabelling()
            findNavController().navigate(direction)
        }

        binding.poseEstimationPlay.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToPoseEstimation()
            findNavController().navigate(direction)
        }

        binding.faceDetectionPlay.setOnClickListener {
            val direction =
                PapaFragmentDirections.actionPapaFragmentToFaceDetection()
            findNavController().navigate(direction)
        }

        return binding.root
    }
}