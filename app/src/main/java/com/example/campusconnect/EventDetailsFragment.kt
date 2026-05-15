package com.example.campusconnect

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EventDetailsFragment : Fragment() {

    private var _binding: FragmentEventDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var event: Event? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        event = arguments?.getSerializable("event") as? Event

        if (event == null) {
            findNavController().navigateUp()
            return
        }

        setupUI()
        fetchRegistrations()
        checkUserRegistration()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnRegisterBottom.setOnClickListener { registerForEvent() }
        binding.btnShare.setOnClickListener { shareEvent() }
        
        binding.btnEmailOrganizer.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${event?.organizerEmail}")
            }
            startActivity(intent)
        }
    }

    private fun setupUI() {
        event?.let {
            binding.tvEventTitle.text = it.title
            binding.tvEventType.text = it.type
            binding.tvBannerDate.text = it.date
            binding.tvBannerLocation.text = it.location
            binding.tvAboutDesc.text = it.description
            
            binding.tvStatsDate.text = it.date?.take(10) // Simplified
            binding.tvStatsTime.text = it.time ?: "10:00 AM"
            binding.tvStatsVenue.text = it.location?.split(",")?.firstOrNull()?.trim() ?: "TBA"
            
            val remaining = it.capacity - it.registeredCount
            binding.tvStatsSeats.text = "$remaining Seats"
            binding.tvBottomSeats.text = "$remaining"

            binding.tvOrganizerName.text = it.organizerName
            binding.tvOrganizerEmail.text = it.organizerEmail

            // Load Image
            if (!it.imageUrl.isNullOrEmpty()) {
                try {
                    val decodedByte = Base64.decode(it.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                    binding.ivEventBanner.setImageBitmap(bitmap)
                } catch (e: Exception) {}
            }

            // Status Badge
            binding.tvStatusBadge.text = it.eventStatus
            when(it.eventStatus) {
                "LIVE" -> binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_live)
                "COMPLETED" -> binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed)
                else -> binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_upcoming)
            }

            // Highlights
            binding.llHighlights.removeAllViews()
            val highlightList = it.highlights ?: listOf("Technical Workshops", "Expert Talks", "Networking Opportunities")
            highlightList.forEach { highlight ->
                val highlightView = LayoutInflater.from(context).inflate(R.layout.item_highlight_bullet, binding.llHighlights, false)
                highlightView.findViewById<TextView>(R.id.tvHighlightText).text = highlight
                binding.llHighlights.addView(highlightView)
            }
        }
    }

    private fun fetchRegistrations() {
        database.reference.child("Registrations").orderByChild("eventId").equalTo(event?.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val studentIds = snapshot.children.mapNotNull { it.child("studentId").value.toString() }
                    loadStudentAvatars(studentIds)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadStudentAvatars(studentIds: List<String>) {
        binding.llRegisteredStudents.removeAllViews()
        val limit = 5
        studentIds.take(limit).forEach { sid ->
            database.reference.child("Users").child(sid).child("profileImageUrl").get()
                .addOnSuccessListener { ds ->
                    if (_binding == null) return@addOnSuccessListener
                    val img = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()).apply { marginEnd = (-8).dpToPx() }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageResource(R.drawable.ic_profile_placeholder)
                    }
                    val base64 = ds.value?.toString()
                    if (!base64.isNullOrEmpty()) {
                        try {
                            val b = Base64.decode(base64, Base64.DEFAULT)
                            img.setImageBitmap(BitmapFactory.decodeByteArray(b, 0, b.size))
                        } catch (e: Exception) {}
                    }
                    binding.llRegisteredStudents.addView(img)
                }
        }
        
        if (studentIds.size > limit) {
            val more = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()).apply { marginStart = 12.dpToPx() }
                gravity = android.view.Gravity.CENTER
                text = "+${studentIds.size - limit}"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.gray_text))
                setBackgroundResource(R.drawable.bg_rounded_grey)
            }
            binding.llRegisteredStudents.addView(more)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun checkUserRegistration() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("Registrations").orderByChild("studentId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val isRegistered = snapshot.children.any { it.child("eventId").value == event?.id }
                    if (isRegistered) {
                        binding.btnRegisterBottom.text = "Registered"
                        binding.btnRegisterBottom.isEnabled = false
                        binding.btnRegisterBottom.alpha = 0.7f
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun registerForEvent() {
        val bundle = Bundle().apply {
            putSerializable("event", event)
        }
        findNavController().navigate(R.id.action_eventDetailsFragment_to_eventRegistrationFragment, bundle)
    }

    private fun shareEvent() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, event?.title)
            putExtra(Intent.EXTRA_TEXT, "Join me at ${event?.title} on ${event?.date} at ${event?.location}!")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Event"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
