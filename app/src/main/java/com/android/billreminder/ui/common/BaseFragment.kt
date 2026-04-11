package com.android.billreminder.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<B : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> B
) : androidx.fragment.app.Fragment() {

    private var _binding: B? = null
    protected val binding: B get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        _binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onInit()
    }

    protected abstract fun onInit()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
