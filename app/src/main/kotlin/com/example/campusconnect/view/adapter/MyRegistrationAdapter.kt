package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemMyRegistrationBinding
import com.example.campusconnect.model.Event

class MyRegistrationAdapter(
    private val eventList: List<Event>,
    private val onActionClick: (Event) -> Unit
) : RecyclerView.Adapter<MyRegistrationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMyRegistrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRegistrationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = eventList[position]
        val b = holder.binding

        b.tvCategory.text = event.category ?: "Business"
        b.tvEventType.text = (event.type ?: "INCUBATION EVENT").uppercase()
        b.tvTitle.text = event.title
        b.tvVenue.text = "Venue: ${event.location ?: "RKU Campus"}"
        b.tvDate.text = "Date: ${event.date ?: "TBA"}"

        // Action button changes based on status
        when (event.eventStatus) {
            "UPCOMING" -> b.btnAction.text = "Entry Pass"
            "LIVE" -> b.btnAction.text = "Join Session"
            "COMPLETED" -> b.btnAction.text = "Download Certificate"
            else -> b.btnAction.text = "View Details"
        }

        b.btnAction.setOnClickListener { onActionClick(event) }
    }

    override fun getItemCount(): Int = eventList.size
}
