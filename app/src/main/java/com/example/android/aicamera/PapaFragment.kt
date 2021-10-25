package com.example.android.aicamera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.android.aicamera.databinding.FragmentPapaBinding

class PapaFragment : Fragment() {

    private lateinit var binding: FragmentPapaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPapaBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}