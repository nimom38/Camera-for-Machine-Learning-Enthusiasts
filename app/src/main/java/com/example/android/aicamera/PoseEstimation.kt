package com.example.android.aicamera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.android.aicamera.databinding.FragmentPoseEstimationBinding

class PoseEstimation : Fragment() {
    private lateinit var binding: FragmentPoseEstimationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPoseEstimationBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}