package com.example.campusconnect

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusconnect.databinding.FragmentChatBinding
import com.example.campusconnect.repository.DataRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var receiverId: String? = null
    private var senderId: String? = null
    private var chatRoom: String? = null

    private var messageAdapter: MessageAdapter? = null
    private val chatListItems = mutableListOf<ChatListItem>()
    private var messagesListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        senderId = FirebaseAuth.getInstance().currentUser?.uid
        receiverId = arguments?.getString("receiverId")
        val receiverName = arguments?.getString("receiverName")

        if (receiverId == null || senderId == null) {
            Toast.makeText(requireContext(), "Error: Invalid session", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        binding.tvChatUserName.text = receiverName ?: "User"
        binding.tvUserInitial.text = receiverName?.take(1)?.uppercase() ?: "U"

        chatRoom = if (senderId!! < receiverId!!) senderId + receiverId else receiverId + senderId
        
        setupRecyclerView()
        fetchMessages()
        fetchReceiverImage()
        markMessagesAsSeen()

        binding.btnSendMessage.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(text = messageText, type = "text")
            }
        }

        binding.btnImageAttachment.setOnClickListener { openGallery() }
        binding.btnCameraAttachment.setOnClickListener { openCamera() }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        
        binding.btnVideoCall.setOnClickListener { makeCall(isVideo = true) }
        binding.btnVoiceCall.setOnClickListener { makeCall(isVideo = false) }
        
        binding.btnAddAttachment.setOnClickListener { openFilePicker() }
    }

    private fun makeCall(isVideo: Boolean) {
        val bundle = Bundle().apply {
            putString("userId", receiverId)
            putBoolean("isIncoming", false)
            putBoolean("isVideo", isVideo)
        }
        findNavController().navigate(R.id.action_chatFragment_to_callFragment, bundle)
    }

    private fun setupRecyclerView() {
        val room = chatRoom ?: return
        messageAdapter = MessageAdapter(chatListItems, room) { message ->
            showForwardDialog(message)
        }
        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatMessages.adapter = messageAdapter
    }

    private fun showForwardDialog(message: Message) {
        database.reference.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val users = mutableListOf<User>()
                for (ds in snapshot.children) {
                    val user = ds.getValue(User::class.java)
                    if (user != null && user.uid != senderId) users.add(user)
                }
                
                val names = users.map { it.fullName ?: "Unknown" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Forward to...")
                    .setItems(names) { _, which ->
                        val targetUser = users[which]
                        targetUser.uid?.let { forwardMessage(message, it) }
                    }.show()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun forwardMessage(original: Message, targetId: String) {
        val sid = senderId ?: return
        val targetRoom = if (sid < targetId) sid + targetId else targetId + sid
        val messageId = database.reference.child("Chats").child(targetRoom).push().key ?: return
        
        val forwardedMsg = original.copy(
            messageId = messageId,
            senderId = sid,
            receiverId = targetId,
            timestamp = System.currentTimeMillis(),
            status = 1,
            forwarded = true
        )
        
        database.reference.child("Chats").child(targetRoom).child(messageId).setValue(forwardedMsg)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Message forwarded", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchMessages() {
        val room = chatRoom ?: return
        messagesListener = database.reference.child("Chats").child(room)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    chatListItems.clear()
                    var lastDate = ""
                    
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            if (message.deletedFor == senderId) continue
                            
                            val currentDate = formatDateHeader(message.timestamp ?: 0L)
                            if (currentDate != lastDate) {
                                chatListItems.add(ChatListItem.DateHeader(currentDate))
                                lastDate = currentDate
                            }
                            chatListItems.add(ChatListItem.MessageItem(message))
                        }
                    }
                    messageAdapter?.notifyDataSetChanged()
                    if (chatListItems.isNotEmpty()) {
                        binding.rvChatMessages.scrollToPosition(chatListItems.size - 1)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage(text: String? = null, type: String, fileUrl: String? = null, fileName: String? = null, fileSize: String? = null) {
        val room = chatRoom ?: return
        val messageId = database.reference.child("Chats").child(room).push().key ?: return
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            message = text,
            fileUrl = fileUrl,
            fileName = fileName,
            fileSize = fileSize,
            timestamp = System.currentTimeMillis(),
            status = 1,
            type = type
        )

        database.reference.child("Chats").child(room).child(messageId).setValue(message)
            .addOnSuccessListener {
                if (type == "text") binding.etMessage.setText("")
                
                receiverId?.let { uid ->
                    val senderName = DataRepository.currentUser?.fullName ?: "Someone"
                    NotificationHelper.sendNotification(
                        userId = uid,
                        title = "New Message from $senderName",
                        body = when(type) {
                            "text" -> text ?: ""
                            "image" -> "Sent an image"
                            else -> "Sent a file: $fileName"
                        },
                        type = "chat"
                    )
                }
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        fileLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            uploadFileToFirebase(uri, "image")
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap ?: return@registerForActivityResult
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            
            val ref = storage.reference.child("chat_files/${UUID.randomUUID()}.jpg")
            ref.putBytes(data).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    sendMessage(type = "image", fileUrl = uri.toString())
                }
            }
        }
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            uploadFileToFirebase(uri, "file")
        }
    }

    private fun uploadFileToFirebase(uri: Uri, type: String) {
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)
        val extension = fileName.substringAfterLast('.', "")
        val finalType = if (extension.equals("pdf", true)) "pdf" else type
        
        val ref = storage.reference.child("chat_files/${UUID.randomUUID()}_$fileName")
        
        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()
        
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                sendMessage(type = finalType, fileUrl = downloadUri.toString(), fileName = fileName, fileSize = fileSize)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileSize(uri: Uri): String {
        var size: Long = 0
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) size = it.getLong(index)
                }
            }
        }
        if (size <= 0) return ""
        val kb = size / 1024
        return if (kb >= 1024) String.format("%.1f MB", kb / 1024f) else "$kb KB"
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "file"
    }

    private fun fetchReceiverImage() {
        receiverId?.let { uid ->
            database.reference.child("Users").child(uid).child("profileImageUrl").get()
                .addOnSuccessListener { snapshot ->
                    if (_binding == null) return@addOnSuccessListener
                    val base64Image = snapshot.value?.toString()
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            val decodedByte = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                            binding.ivUserImage.setImageBitmap(bitmap)
                            binding.tvUserInitial.visibility = View.GONE
                        } catch (e: Exception) {}
                    }
                }
        }
    }

    private fun formatDateHeader(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val now = Calendar.getInstance()
        return if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) "Today"
        else if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1) "Yesterday"
        else SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun markMessagesAsSeen() {
        val room = chatRoom ?: return
        database.reference.child("Chats").child(room).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (msgSnapshot in snapshot.children) {
                    val msg = msgSnapshot.getValue(Message::class.java)
                    if (msg != null && msg.receiverId == senderId && (msg.status ?: 1) < 3) {
                        msgSnapshot.ref.child("status").setValue(3)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatRoom?.let { room ->
            messagesListener?.let { database.reference.child("Chats").child(room).removeEventListener(it) }
        }
        _binding = null
    }
}
