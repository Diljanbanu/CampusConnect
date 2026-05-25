package com.example.campusconnect.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.R
import com.example.campusconnect.databinding.FragmentPrivacyPolicyBinding
import com.example.campusconnect.model.PrivacyPolicy
import com.example.campusconnect.repository.DataRepository

class PrivacyPolicyFragment : Fragment() {

    private var _binding: FragmentPrivacyPolicyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        loadPolicy()
    }

    private fun loadPolicy() {
        val policy = DataRepository.privacyPolicy
        if (policy != null && !policy.sections.isNullOrEmpty()) {
            updateUI(policy)
        }
    }

    private fun updateUI(policy: PrivacyPolicy) {
        val sections = policy.sections ?: return
        binding.llPolicyContainer.removeAllViews()
        
        sections.forEachIndexed { index, section ->
            val titleView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) setMargins(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
                }
                text = "${index + 1}. ${section.title}"
                setTextColor(requireContext().getColor(R.color.black))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            binding.llPolicyContainer.addView(titleView)

            val contentView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
                }
                text = section.content
                setTextColor(requireContext().getColor(R.color.gray_text))
                textSize = 13f
                setLineSpacing(4f * resources.displayMetrics.density, 1f)
            }
            binding.llPolicyContainer.addView(contentView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
