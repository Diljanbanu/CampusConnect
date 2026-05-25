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
        holder.binding.tvStudentInitial.text = record.studentName?.take(2)?.uppercase() ?: "S"
        holder.binding.tvStudentName.text = record.studentName
        holder.binding.tvTaughtSkill.text = "Taught: ${record.skillName}"
        
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.binding.tvDate.text = record.date?.let { sdf.format(Date(it)) } ?: ""
        
        holder.binding.tvRating.text = "★ ${String.format("%.1f", record.rating)} Rating"
    }

    override fun getItemCount(): Int = mentorshipList.size
}
