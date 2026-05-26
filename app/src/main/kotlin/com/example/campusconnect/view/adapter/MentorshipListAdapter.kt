package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemMentorshipListBinding
import com.example.campusconnect.model.MentorshipRecord
import java.text.SimpleDateFormat
import java.util.*

class MentorshipListAdapter(private val mentorshipList: List<MentorshipRecord>) : RecyclerView.Adapter<MentorshipListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMentorshipListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMentorshipListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = mentorshipList[position]
        val b = holder.binding

        b.tvStudentInitial.text = record.studentName?.take(2)?.uppercase() ?: "S"
        b.tvStudentName.text = record.studentName
        b.tvTaughtSkill.text = "Mastered: ${record.skillName}"
        
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        b.tvDate.text = record.date?.let { sdf.format(Date(it)) } ?: ""
        
        b.tvRating.text = String.format("%.1f", record.rating)
    }

    override fun getItemCount(): Int = mentorshipList.size
}
