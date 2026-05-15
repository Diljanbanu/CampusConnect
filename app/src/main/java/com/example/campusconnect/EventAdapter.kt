package com.example.campusconnect

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemEventBinding

class EventAdapter(
    private val eventList: List<Event>,
    private val onRegisterClick: ((Event) -> Unit)? = null,
    private val onDetailsClick: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        
        holder.binding.tvEventTitle.text = event.title
        holder.binding.tvEventDesc.text = event.description
        holder.binding.tvEventDate.text = "${event.date}, ${event.time}"
        holder.binding.tvEventLocation.text = event.location
        
        // Seats info
        val seatsLeft = event.capacity - event.registeredCount
        holder.binding.tvSeatsLeft.text = "$seatsLeft Seats Left"
        holder.binding.tvRegisteredCount.text = "${event.registeredCount} Registered"
        
        // Image logic
        if (!event.imageUrl.isNullOrEmpty()) {
            try {
                val decodedByte = Base64.decode(event.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                holder.binding.ivEventBanner.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.ivEventBanner.setImageResource(R.drawable.bg_featured_event)
            }
        }

        // Status Badge & Action Button
        when (event.eventStatus?.uppercase()) {
            "LIVE" -> {
                holder.binding.tvStatusBadge.text = "LIVE"
                holder.binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_live)
                holder.binding.btnAction.text = "Join Live"
                holder.binding.tvSeatsLeft.visibility = View.GONE
            }
            "COMPLETED" -> {
                holder.binding.tvStatusBadge.text = "COMPLETED"
                holder.binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed)
                holder.binding.btnAction.text = "Certificate"
                holder.binding.btnDetails.text = "Photos"
                holder.binding.tvSeatsLeft.text = "Completed"
                holder.binding.tvSeatsLeft.setBackgroundTintList(null) // Reset tint
                holder.binding.tvRegisteredCount.text = "${event.registeredCount} Attended"
            }
            else -> { // UPCOMING
                holder.binding.tvStatusBadge.text = "UPCOMING"
                holder.binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_upcoming)
                holder.binding.btnAction.text = "Register"
                holder.binding.btnDetails.text = "Details"
            }
        }

        holder.binding.btnAction.setOnClickListener { 
            if (holder.binding.btnAction.text == "Register") {
                val bundle = android.os.Bundle().apply {
                    putSerializable("event", event)
                }
                androidx.navigation.Navigation.findNavController(it).navigate(R.id.action_eventsFragment_to_eventRegistrationFragment, bundle)
            } else {
                onRegisterClick?.invoke(event) 
            }
        }
        holder.binding.btnDetails.setOnClickListener { 
            val bundle = android.os.Bundle().apply {
                putSerializable("event", event)
            }
            androidx.navigation.Navigation.findNavController(it).navigate(R.id.action_eventsFragment_to_eventDetailsFragment, bundle)
        }
    }

    override fun getItemCount(): Int = eventList.size
}
