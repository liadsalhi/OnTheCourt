package com.onthecourt.app.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.onthecourt.app.R
import com.onthecourt.app.chat.adapter.ChatMessageAdapter
import com.onthecourt.app.databinding.FragmentChatBinding
import com.onthecourt.app.model.ChatMessage

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var messagesListener: ListenerRegistration? = null
    private lateinit var messageAdapter: ChatMessageAdapter

    private val chatId: String by lazy {
        val myUid = auth.currentUser?.uid ?: ""
        listOf(myUid, args.friendUid).sorted().joinToString("_")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvFriendName.text = args.friendName

        messageAdapter = ChatMessageAdapter(auth.currentUser?.uid ?: "")
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = messageAdapter

        binding.btnSend.setOnClickListener { sendMessage() }

        listenToMessages()
    }

    private fun listenToMessages() {
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                messageAdapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return
        val myUid = auth.currentUser?.uid ?: return

        val messageRef = db.collection("chats").document(chatId)
            .collection("messages").document()
        val message = ChatMessage(
            messageId = messageRef.id,
            senderId = myUid,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        binding.etMessage.setText("")
        messageRef.set(message)
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.msg_send_failed, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.remove()
        _binding = null
    }
}
