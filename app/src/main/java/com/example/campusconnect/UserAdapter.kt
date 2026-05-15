package com.example.campusconnect

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class UserAdapter(
    private var userList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.tvUserName.text = user.fullName
        holder.binding.tvUserInitial.text = user.fullName?.take(1)?.uppercase() ?: "U"
        
        // Group logic (dummy check for community items)
        if (user.fullName?.contains("Community") == true) {
            holder.binding.tvUserInitial.visibility = View.GONE
            holder.binding.ivGroupIcon.visibility = View.VISIBLE
            holder.binding.cvAvatar.setCardBackgroundColor(holder.itemView.context.getColor(R.color.primaryRedLight)) // or some blue
        } else {
            holder.binding.tvUserInitial.visibility = View.VISIBLE
            holder.binding.ivGroupIcon.visibility = View.GONE
            holder.binding.cvAvatar.setCardBackgroundColor(0xFFFFEAEA.toInt())
        }

        // Load Base64 Profile Image
        if (!user.profileImageUrl.isNullOrEmpty()) {
            try {
                val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                holder.binding.ivUserImage.setImageBitmap(bitmap)
                holder.binding.ivUserImage.visibility = View.VISIBLE
                holder.binding.tvUserInitial.visibility = View.GONE
                holder.binding.ivGroupIcon.visibility = View.GONE
            } catch (e: Exception) {
                holder.binding.ivUserImage.visibility = View.GONE
            }
        } else {
            holder.binding.ivUserImage.visibility = View.GONE
        }

        // Online status (dummy for now)
        holder.binding.viewOnlineStatus.visibility = if (position % 2 == 0) View.VISIBLE else View.GONE

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val otherUserId = user.uid
        if (currentUserId != null && otherUserId != null) {
            val chatRoom = if (currentUserId < otherUserId) currentUserId + otherUserId else otherUserId + currentUserId
            
            FirebaseDatabase.getInstance().reference.child("Chats").child(chatRoom)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var unreadCount = 0
                        var lastMsg = ""
                        var timestamp = 0L
                        
                        for (msgSnapshot in snapshot.children) {
                            val msg = msgSnapshot.getValue(Message::class.java)
                            if (msg != null) {
                                lastMsg = msg.message ?: ""
                                timestamp = msg.timestamp ?: 0L
                                if (msg.receiverId == currentUserId && (msg.status ?: 1) < 3) {
                                    unreadCount++
                                }
                            }
                        }
                        
                        holder.binding.tvLastMessage.text = if (lastMsg.isNotEmpty()) lastMsg else "No messages yet"
                        holder.binding.tvMessageTime.text = if (timestamp > 0) formatTime(timestamp) else ""
                        
                        if (unreadCount > 0) {
                            holder.binding.tvUnreadCount.text = unreadCount.toString()
                            holder.binding.tvUnreadCount.visibility = View.VISIBLE
                            holder.binding.tvMessageTime.setTextColor(0xFF22C55E.toInt())
                        } else {
                            holder.binding.tvUnreadCount.visibility = View.GONE
                            holder.binding.tvMessageTime.setTextColor(0xFF64748B.toInt())
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val msgDate = Calendar.getInstance().apply { time = date }
        
        return if (now.get(Calendar.DATE) == msgDate.get(Calendar.DATE)) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        } else if (now.get(Calendar.DATE) - msgDate.get(Calendar.DATE) == 1) {
            "Yesterday"
        } else {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
    }

    override fun getItemCount(): Int = userList.size
}
