package com.example.campusconnect

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemSkillBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SkillAdapter(
    private val skillList: List<Skill>,
    private val isProfilePage: Boolean = false,
    private val onEditClick: ((Skill) -> Unit)? = null,
    private val onDeleteClick: ((Skill) -> Unit)? = null,
    private val onViewProfileClick: ((String) -> Unit)? = null,
    private val onChatClick: ((Skill) -> Unit)? = null
) : RecyclerView.Adapter<SkillAdapter.SkillViewHolder>() {

    class SkillViewHolder(val binding: ItemSkillBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val skill = skillList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        holder.binding.tvSkillName.text = skill.skillName
        holder.binding.tvSkillDescription.text = skill.description ?: "No description provided."
        holder.binding.tvSkillCategory.text = skill.category ?: "SKILL"
        holder.binding.tvStudentName.text = skill.studentName ?: "Student"

        // Load Student Image safely
        skill.studentId?.let { sid ->
            FirebaseDatabase.getInstance().reference.child("Users").child(sid).child("profileImageUrl").get()
                .addOnSuccessListener { snapshot ->
                    val base64Image = snapshot.value?.toString()
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            holder.binding.ivUserImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            holder.binding.ivUserImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } else {
                        holder.binding.ivUserImage.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
        } ?: run {
            holder.binding.ivUserImage.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Chat logic
        holder.binding.btnChat.setOnClickListener {
            val sid = skill.studentId
            if (sid == null) {
                Toast.makeText(holder.itemView.context, "User ID not found", Toast.LENGTH_SHORT).show()
            } else if (sid == currentUserId) {
                Toast.makeText(holder.itemView.context, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
            } else {
                onChatClick?.invoke(skill)
            }
        }

        // Email logic
        holder.binding.btnEmail.setOnClickListener {
            sendEmail(skill, holder.itemView)
        }

        // View Profile logic
        holder.binding.btnViewProfile.setOnClickListener {
            skill.studentId?.let { sid ->
                onViewProfileClick?.invoke(sid)
            } ?: Toast.makeText(holder.itemView.context, "Profile not available", Toast.LENGTH_SHORT).show()
        }

        // Logic for profile page (edit/delete)
        if (isProfilePage) {
            holder.binding.llActions.visibility = View.GONE
            holder.binding.btnViewProfile.visibility = View.GONE
        }
    }

    private fun sendEmail(skill: Skill, view: View) {
        val sid = skill.studentId ?: return
        
        if (!skill.studentEmail.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${skill.studentEmail}")
                putExtra(Intent.EXTRA_SUBJECT, "Query regarding your skill: ${skill.skillName}")
            }
            try {
                view.context.startActivity(Intent.createChooser(intent, "Send Email..."))
            } catch (e: Exception) {
                Toast.makeText(view.context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        } else {
            FirebaseDatabase.getInstance().reference.child("Users").child(sid).child("email").get()
                .addOnSuccessListener { snapshot ->
                    val email = snapshot.value?.toString()
                    if (!email.isNullOrEmpty()) {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$email")
                            putExtra(Intent.EXTRA_SUBJECT, "Query regarding your skill: ${skill.skillName}")
                        }
                        view.context.startActivity(Intent.createChooser(intent, "Send Email..."))
                    } else {
                        Toast.makeText(view.context, "Email address not available", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun getItemCount(): Int = skillList.size
}
