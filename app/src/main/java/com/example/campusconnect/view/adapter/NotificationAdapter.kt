package com.example.campusconnect.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.R
import com.example.campusconnect.databinding.ItemNotificationBinding
import com.example.campusconnect.model.Notification

class NotificationAdapter(
    private val notificationList: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationList[position]
        
        holder.binding.tvNotificationTitle.text = notification.title
        holder.binding.tvNotificationBody.text = notification.body
        holder.binding.tvNotificationTime.text = formatTimeAgo(notification.timestamp ?: 0L)
        
        val iconRes = when (notification.type) {
            "skill" -> R.drawable.skills
            "event" -> R.drawable.event
            "startup" -> R.drawable.rocket
            "chat" -> R.drawable.chat
            else -> R.drawable.ic_notification
        }
        holder.binding.ivNotificationIcon.setImageResource(iconRes)
        holder.binding.viewUnreadDot.visibility = if (notification.isRead) View.GONE else View.VISIBLE
        
        holder.itemView.setOnClickListener { onNotificationClick(notification) }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    override fun getItemCount(): Int = notificationList.size
}
