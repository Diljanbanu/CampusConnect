package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemMySkillCardBinding
import com.example.campusconnect.model.Skill

class MySkillCardAdapter(private val skillList: List<Skill>) : RecyclerView.Adapter<MySkillCardAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMySkillCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMySkillCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val skill = skillList[position]
        holder.binding.tvSkillName.text = skill.skillName
        holder.binding.ratingBar.rating = skill.rating
        holder.binding.tvRatingNum.text = "(${String.format("%.1f", skill.rating)}★ Rating)"
        holder.binding.tvStudentCount.text = "${skill.reviewsCount} Students"
    }

    override fun getItemCount(): Int = skillList.size
}
