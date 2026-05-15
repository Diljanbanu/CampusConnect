package com.example.campusconnect

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentCallBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CallFragment : Fragment() {

    private var _binding: FragmentCallBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private var targetUserId: String? = null
    private var callId: String? = null
    private var isIncoming: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance()
        targetUserId = arguments?.getString("userId")
        callId = arguments?.getString("callId") ?: database.reference.child("Calls").push().key
        isIncoming = arguments?.getBoolean("isIncoming") ?: false

        loadUserData()
        setupCallLogic()

        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnAcceptCall.setOnClickListener { acceptCall() }
    }

    private fun loadUserData() {
        targetUserId?.let { uid ->
            database.reference.child("Users").child(uid).get().addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.tvCallUserName.text = it.fullName
                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        val decoded = Base64.decode(it.profileImageUrl, Base64.DEFAULT)
                        binding.ivCallUserImage.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
                    }
                }
            }
        }
    }

    private fun setupCallLogic() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val callRef = database.reference.child("Calls").child(callId!!)

        if (isIncoming) {
            binding.tvCallStatus.text = "Incoming Call..."
            binding.btnAcceptCall.visibility = View.VISIBLE
        } else {
            binding.tvCallStatus.text = "Calling..."
            // Create call in DB
            val callData = mapOf(
                "callerId" to currentUserId,
                "receiverId" to targetUserId,
                "status" to "ringing"
            )
            callRef.setValue(callData)
        }

        callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val status = snapshot.child("status").value?.toString()
                when (status) {
                    "accepted" -> binding.tvCallStatus.text = "Connected"
                    "ended" -> {
                        database.reference.child("Calls").child(callId!!).removeValue()
                        findNavController().navigateUp()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun acceptCall() {
        database.reference.child("Calls").child(callId!!).child("status").setValue("accepted")
        binding.btnAcceptCall.visibility = View.GONE
        binding.tvCallStatus.text = "Connected"
    }

    private fun endCall() {
        database.reference.child("Calls").child(callId!!).child("status").setValue("ended")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
