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
import com.example.campusconnect.databinding.FragmentAppVersionBinding
import com.example.campusconnect.model.AppVersion
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AppVersionFragment : Fragment() {

    private var _binding: FragmentAppVersionBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppVersionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        fetchVersionInfo()
    }

    private fun fetchVersionInfo() {
        database.reference.child("AppConfig").child("versionInfo")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val versionInfo = snapshot.getValue(AppVersion::class.java) ?: AppVersion()
                    updateUI(versionInfo)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Fallback to default in model if database fails
                    if (_binding != null) updateUI(AppVersion())
                }
            })
    }

    private fun updateUI(info: AppVersion) {
        binding.tvVersionCode.text = info.version
        binding.tvBuildRef.text = "Build Ref: ${info.buildRef}"
        
        binding.llChangelog.removeAllViews()
        info.changelog?.forEach { change ->
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                }
                text = "•  $change"
                setTextColor(requireContext().getColor(R.color.gray_text))
                textSize = 13f
            }
            binding.llChangelog.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
