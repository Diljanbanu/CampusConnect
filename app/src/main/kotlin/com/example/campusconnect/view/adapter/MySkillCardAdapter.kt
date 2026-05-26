package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemMySkillCardBinding
import com.example.campusconnect.model.Skill

class MySkillCardAdapter(
    private val skillList: List<Skill>,
    private val onEditClick: (Skill) -> Unit,
    private val onDeleteClick: (Skill) -> Unit
) : RecyclerView.Adapter<MySkillCardAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMySkillCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMySkillCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val skill = skillList[position]
        val b = holder.binding

        b.tvSkillName.text = skill.skillName
        b.tvDescription.text = skill.description ?: "No description provided."
        b.tvCategory.text = skill.category ?: "Skill"
        b.tvLevel.text = skill.level ?: "INTERMEDIATE"
        
        b.ratingBar.rating = skill.rating
        b.tvReviewCount.text = "${skill.reviewsCount} Students"
        
        b.tvPopularityPercent.text = "${skill.popularity}%"
        b.pbPopularity.progress = skill.popularity
        
        b.llVerified.visibility = if (skill.isVerified) View.VISIBLE else View.GONE
        
        b.btnEdit.setOnClickListener { onEditClick(skill) }
        b.btnDelete.setOnClickListener { onDeleteClick(skill) }
        
        // Joined Community logic (can be tied to a dynamic node later)
        b.btnCommunity.text = if (position % 2 == 0) "Joined Community" else "Join Community"
    }

    override fun getItemCount(): Int = skillList.size
}
