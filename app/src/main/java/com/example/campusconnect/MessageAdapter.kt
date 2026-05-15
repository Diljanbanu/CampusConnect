package com.example.campusconnect

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.databinding.ItemDateHeaderBinding
import com.example.campusconnect.databinding.ItemMessageReceivedBinding
import com.example.campusconnect.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val itemList: List<ChatListItem>,
    private val chatRoomId: String,
    private val onForwardClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val VIEW_TYPE_DATE = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedViewHolder(binding)
            }
            else -> {
                val binding = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position >= itemList.size) return
        val item = itemList[position]

        if (item is ChatListItem.DateHeader) {
            (holder as DateViewHolder).binding.tvDateHeader.text = item.date
            return
        }

        if (item is ChatListItem.MessageItem) {
            val message = item.message
            val time = try {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp ?: 0))
            } catch (e: Exception) {
                "00:00"
            }

            if (holder is SentViewHolder) {
                bindSentMessage(holder, message, time, position == itemList.size - 1)
            } else if (holder is ReceivedViewHolder) {
                bindReceivedMessage(holder, message, time)
            }
        }
    }

    private fun bindSentMessage(holder: SentViewHolder, message: Message, time: String, isLast: Boolean) {
        val binding = holder.binding
        binding.tvForwardedLabel.visibility = if (message.forwarded == true) View.VISIBLE else View.GONE
        
        if (message.deleted == true) {
            binding.tvSentMessage.visibility = View.VISIBLE
            binding.cardSentImage.visibility = View.GONE
            binding.llFileContainer.visibility = View.GONE
            binding.tvSentMessage.text = "This message was deleted"
            binding.tvSentMessage.alpha = 0.5f
            holder.itemView.setOnLongClickListener(null)
        } else {
            binding.tvSentMessage.alpha = 1.0f
            when (message.type) {
                "image" -> {
                    binding.tvSentMessage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.GONE
                    binding.cardSentImage.visibility = View.VISIBLE
                    
                    val imageUrl = message.fileUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(holder.itemView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(binding.ivSentImage)
                    } else if (!message.message.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(message.message, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivSentImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            binding.ivSentImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
                "pdf", "file", "document" -> {
                    binding.tvSentMessage.visibility = View.GONE
                    binding.cardSentImage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.VISIBLE
                    binding.tvFileName.text = message.fileName ?: "File"
                    binding.tvFileSize.text = message.fileSize ?: ""
                    binding.llFileContainer.setOnClickListener { openFile(holder.itemView.context, message.fileUrl) }
                }
                else -> {
                    binding.tvSentMessage.visibility = View.VISIBLE
                    binding.cardSentImage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.GONE
                    binding.tvSentMessage.text = message.message ?: ""
                }
            }

            holder.itemView.setOnLongClickListener {
                showOptionsDialog(holder.itemView.context, message)
                true
            }
        }

        binding.tvSentTime.text = time

        if (isLast && (message.status ?: 1) == 3) {
            binding.tvSeenStatus.visibility = View.VISIBLE
            binding.tvSeenStatus.text = formatSeenStatus(message.seenAt)
        } else {
            binding.tvSeenStatus.visibility = View.GONE
        }

        when (message.status ?: 1) {
            3 -> {
                binding.ivMessageTicks.setImageResource(R.drawable.ic_double_tick)
                binding.ivMessageTicks.clearColorFilter()
            }
            2 -> {
                binding.ivMessageTicks.setImageResource(R.drawable.ic_double_tick)
                binding.ivMessageTicks.setColorFilter(0xFF757575.toInt())
            }
            else -> {
                binding.ivMessageTicks.setImageResource(android.R.drawable.ic_menu_send)
                binding.ivMessageTicks.setColorFilter(0xFF757575.toInt())
            }
        }
    }

    private fun bindReceivedMessage(holder: ReceivedViewHolder, message: Message, time: String) {
        val binding = holder.binding
        binding.tvForwardedLabel.visibility = if (message.forwarded == true) View.VISIBLE else View.GONE

        if (message.deleted == true) {
            binding.tvReceivedMessage.visibility = View.VISIBLE
            binding.cardReceivedImage.visibility = View.GONE
            binding.llFileContainer.visibility = View.GONE
            binding.tvReceivedMessage.text = "This message was deleted"
            binding.tvReceivedMessage.alpha = 0.5f
        } else {
            binding.tvReceivedMessage.alpha = 1.0f
            when (message.type) {
                "image" -> {
                    binding.tvReceivedMessage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.GONE
                    binding.cardReceivedImage.visibility = View.VISIBLE
                    
                    val imageUrl = message.fileUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(holder.itemView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(binding.ivReceivedImage)
                    } else if (!message.message.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(message.message, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivReceivedImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            binding.ivReceivedImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
                "pdf", "file", "document" -> {
                    binding.tvReceivedMessage.visibility = View.GONE
                    binding.cardReceivedImage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.VISIBLE
                    binding.tvFileName.text = message.fileName ?: "File"
                    binding.tvFileSize.text = message.fileSize ?: ""
                    binding.llFileContainer.setOnClickListener { openFile(holder.itemView.context, message.fileUrl) }
                }
                else -> {
                    binding.tvReceivedMessage.visibility = View.VISIBLE
                    binding.cardReceivedImage.visibility = View.GONE
                    binding.llFileContainer.visibility = View.GONE
                    binding.tvReceivedMessage.text = message.message ?: ""
                }
            }
            
            holder.itemView.setOnLongClickListener {
                showOptionsDialog(holder.itemView.context, message)
                true
            }
        }
        binding.tvReceivedTime.text = time
    }

    private fun openFile(context: Context, url: String?) {
        if (url.isNullOrEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatSeenStatus(seenAt: Long?): String {
        if (seenAt == null) return "seen"
        val diff = System.currentTimeMillis() - seenAt
        val mins = diff / (1000 * 60)
        val hours = mins / 60
        
        return when {
            mins < 1 -> "seen just now"
            mins < 60 -> "seen ${mins}m ago"
            hours < 24 -> "seen ${hours}h ago"
            else -> "seen " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(seenAt))
        }
    }

    private fun showOptionsDialog(context: Context, message: Message) {
        val options = arrayOf("Copy", "Forward", "Delete", "Star")
        AlertDialog.Builder(context)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyToClipboard(context, message.message ?: "")
                    1 -> onForwardClick(message)
                    2 -> showDeleteOptions(context, message)
                    3 -> Toast.makeText(context, "Starred", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun copyToClipboard(context: Context, text: String) {
        if (text.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteOptions(context: Context, message: Message) {
        val options = arrayOf("Delete for me", "Delete for everyone", "Cancel")
        AlertDialog.Builder(context)
            .setTitle("Delete Message?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteForMe(message)
                    1 -> deleteForEveryone(message)
                }
            }.show()
    }

    private fun deleteForMe(message: Message) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val mid = message.messageId ?: return
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatRoomId)
            .child(mid).child("deletedFor").setValue(uid)
    }

    private fun deleteForEveryone(message: Message) {
        val mid = message.messageId ?: return
        FirebaseDatabase.getInstance().reference.child("Chats").child(chatRoomId)
            .child(mid).child("deleted").setValue(true)
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= itemList.size) return VIEW_TYPE_DATE
        val item = itemList[position]
        return when (item) {
            is ChatListItem.DateHeader -> VIEW_TYPE_DATE
            is ChatListItem.MessageItem -> {
                if (FirebaseAuth.getInstance().currentUser?.uid == item.message.senderId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
    class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)
    class DateViewHolder(val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
}
