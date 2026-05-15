package com.example.campusconnect

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.databinding.FragmentStartupBinding
import com.example.campusconnect.repository.DataRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StartUpFragment : Fragment() {

    private var _binding: FragmentStartupBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var startupAdapter: StartupAdapter
    private val startupList = mutableListOf<Startup>()
    private val fullStartupList = mutableListOf<Startup>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupRecyclerView()
        fetchApprovedIdeas()
        setupSearch()

        binding.fabAddIdea.setOnClickListener {
            showAddStartupDialog()
        }
    }

    private fun setupRecyclerView() {
        startupAdapter = StartupAdapter(startupList)
        binding.rvStartups.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStartups.adapter = startupAdapter
    }

    private fun fetchApprovedIdeas() {
        val ideasRef = database.reference.child("Ideas")
        ideasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                fullStartupList.clear()
                for (ideaSnapshot in snapshot.children) {
                    val idea = ideaSnapshot.getValue(Startup::class.java)
                    if (idea != null && idea.status == "approved") {
                        fullStartupList.add(idea)
                    }
                }
                filterStartups(binding.etSearchStartups.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to load ideas", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearchStartups.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStartups(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterStartups(query: String) {
        val filtered = if (query.isEmpty()) {
            fullStartupList
        } else {
            fullStartupList.filter {
                it.title?.contains(query, ignoreCase = true) == true ||
                it.studentName?.contains(query, ignoreCase = true) == true ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }
        startupList.clear()
        startupList.addAll(filtered)
        startupAdapter.notifyDataSetChanged()
    }

    private fun showAddStartupDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Post New Idea")
        val layout = LinearLayout(requireContext()).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etTitle = EditText(requireContext()).apply { hint = "Startup Title" }
        val etDesc = EditText(requireContext()).apply { hint = "Description" }
        
        layout.addView(etTitle)
        layout.addView(etDesc)
        
        builder.setView(layout)
        builder.setPositiveButton("Submit") { _, _ ->
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            if (title.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                val user = DataRepository.currentUser
                val ideaId = database.reference.child("Ideas").push().key ?: return@setPositiveButton
                val startup = Startup(
                    id = ideaId,
                    title = title,
                    description = desc,
                    studentName = user?.fullName,
                    studentId = userId,
                    status = "pending"
                )
                database.reference.child("Ideas").child(ideaId).setValue(startup)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Idea submitted for approval", Toast.LENGTH_SHORT).show()
                        
                        // Notify Admin or User themselves for confirmation
                        NotificationHelper.sendNotification(
                            userId = userId,
                            title = "Startup Submitted",
                            body = "Your idea '$title' has been submitted for approval.",
                            type = "startup"
                        )
                    }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
