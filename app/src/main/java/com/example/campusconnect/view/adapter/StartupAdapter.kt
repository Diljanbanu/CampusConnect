package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemStartupBinding
import com.example.campusconnect.model.Startup

class StartupAdapter(private val startupList: List<Startup>) : RecyclerView.Adapter<StartupAdapter.StartupViewHolder>() {

    class StartupViewHolder(val binding: ItemStartupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartupViewHolder {
        val binding = ItemStartupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StartupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StartupViewHolder, position: Int) {
        val startup = startupList[position]
        holder.binding.tvTitle.text = startup.title
        holder.binding.tvCategory.text = startup.category
        holder.binding.tvStudentName.text = startup.studentName
        holder.binding.tvDescription.text = startup.description
    }

    override fun getItemCount(): Int = startupList.size
}
