package com.example.campusconnect.view.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect.R
import com.example.campusconnect.databinding.ItemContactBinding
import com.example.campusconnect.model.User

class ContactAdapter(
    private var contactList: List<User>,
    private val onContactClick: (User) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val user = contactList[position]
        holder.binding.tvContactName.text = user.fullName
        holder.binding.tvContactSubtitle.text = user.branch ?: user.skills ?: "Student"
        holder.binding.tvContactInitial.text = user.fullName?.take(1)?.uppercase() ?: "U"

        if (!user.profileImageUrl.isNullOrEmpty()) {
            try {
                val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                holder.binding.ivContactImage.setImageBitmap(bitmap)
                holder.binding.ivContactImage.visibility = View.VISIBLE
                holder.binding.tvContactInitial.visibility = View.GONE
            } catch (e: Exception) {
                holder.binding.ivContactImage.visibility = View.GONE
                holder.binding.tvContactInitial.visibility = View.VISIBLE
            }
        } else {
            holder.binding.ivContactImage.visibility = View.GONE
            holder.binding.tvContactInitial.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onContactClick(user) }
    }

    override fun getItemCount(): Int = contactList.size
}
