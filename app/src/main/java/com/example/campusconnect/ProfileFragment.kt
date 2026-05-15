package com.example.campusconnect

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.campusconnect.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageBitmap: Bitmap? = null
    
    private var targetUserId: String? = null
    private var currentUser: User? = null
    private var userListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        targetUserId = arguments?.getString("userId") ?: auth.currentUser?.uid

        setupClickListeners()
        loadUserData()

        val isOwnProfile = targetUserId == auth.currentUser?.uid

        if (isOwnProfile) {
            binding.btnEditProfile.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.VISIBLE
            binding.llSelfOptions.visibility = View.VISIBLE
            binding.btnChatListFAB.visibility = View.VISIBLE
            
            binding.btnAddImage.setOnClickListener { openGallery() }
            
            binding.btnEditProfile.setOnClickListener {
                val bundle = Bundle()
                currentUser?.let { bundle.putString("userId", it.uid) }
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, bundle)
            }
            
            binding.btnLogout.setOnClickListener {
                auth.signOut()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
            
            binding.btnChatListFAB.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_chatListFragment)
            }
        } else {
            binding.btnEditProfile.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE
            binding.llSelfOptions.visibility = View.GONE
            binding.btnChatListFAB.visibility = View.GONE
            binding.btnAddImage.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.optionMyEvents.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myEventsFragment)
        }
        
        binding.optionMyStartups.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myStartupsFragment)
        }
        
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        
        binding.btnResume.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_resumeBuilderFragment)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivProfileImage.setImageBitmap(selectedImageBitmap)
                binding.tvProfileInitial.visibility = View.GONE
                saveImageToFirebase()
            }
        }
    }

    private fun saveImageToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val bitmap = selectedImageBitmap ?: return
        val base64Image = encodeImage(bitmap)
        
        database.reference.child("Users").child(userId).child("profileImageUrl").setValue(base64Image)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Image Updated", Toast.LENGTH_SHORT).show()
                selectedImageBitmap = null
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun loadUserData() {
        val userId = targetUserId ?: return
        val userRef = database.reference.child("Users").child(userId)
        
        userListener = userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                currentUser = snapshot.getValue(User::class.java)
                currentUser?.let { user ->
                    binding.tvProfileName.text = user.fullName
                    
                    // Set Bio
                    if (!user.bio.isNullOrEmpty()) {
                        binding.tvProfileBio.text = user.bio
                    }

                    // Set subtitle
                    if (!user.branch.isNullOrEmpty()) {
                        binding.tvProfileSubtitle.text = "${user.branch} • Sem 7"
                    }
                    
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivProfileImage.setImageBitmap(bitmap)
                            binding.tvProfileInitial.visibility = View.GONE
                        } catch (e: Exception) {
                            binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } else {
                        binding.tvProfileInitial.text = "CC"
                        binding.tvProfileInitial.visibility = View.VISIBLE
                        binding.ivProfileImage.setImageResource(R.drawable.profile_circle)
                    }

                    updateCounts(userId)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateCounts(userId: String) {
        // Skills count
        database.reference.child("Skills").orderByChild("studentId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    binding.tvSkillCountNum.text = snapshot.childrenCount.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Startups count
        database.reference.child("Ideas").orderByChild("studentId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    binding.tvStartupCountNum.text = snapshot.childrenCount.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            
        // Events count (Registrations)
        database.reference.child("Registrations").orderByChild("studentId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    binding.tvEventCountNum.text = snapshot.childrenCount.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val userId = targetUserId
        if (userId != null && userListener != null) {
            database.reference.child("Users").child(userId).removeEventListener(userListener!!)
        }
        _binding = null
    }
}
