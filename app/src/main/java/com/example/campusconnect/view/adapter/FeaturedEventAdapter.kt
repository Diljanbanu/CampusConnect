package com.example.campusconnect.view.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.R
import com.example.campusconnect.databinding.ItemFeaturedEventBinding
import com.example.campusconnect.model.Event

class FeaturedEventAdapter(
    private val featuredEvents: List<Event>
) : RecyclerView.Adapter<FeaturedEventAdapter.FeaturedViewHolder>() {

    class FeaturedViewHolder(val binding: ItemFeaturedEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        val event = featuredEvents[position]
        holder.binding.tvFeaturedTitle.text = event.title
        holder.binding.tvFeaturedType.text = event.type ?: "Campus Event"
        holder.binding.tvFeaturedDate.text = event.date
        holder.binding.tvFeaturedLocation.text = event.location

        if (!event.imageUrl.isNullOrEmpty()) {
            try {
                val decodedByte = Base64.decode(event.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                holder.binding.ivFeaturedBanner.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.binding.ivFeaturedBanner.setImageResource(R.drawable.bg_featured_event)
            }
        }

        holder.itemView.setOnClickListener {
            val bundle = android.os.Bundle().apply {
                putSerializable("event", event)
            }
            androidx.navigation.Navigation.findNavController(it).navigate(R.id.action_eventsFragment_to_eventDetailsFragment, bundle)
        }
    }

    override fun getItemCount(): Int = featuredEvents.size
}
