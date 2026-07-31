package com.onthecourt.app.chat.adapter

import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.onthecourt.app.R
import com.onthecourt.app.databinding.ItemChatMessageBinding
import com.onthecourt.app.model.ChatMessage
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.diffCallbackBy

class ChatMessageAdapter(
    private val currentUid: String
) : BindingListAdapter<ChatMessage, ItemChatMessageBinding>(
    ItemChatMessageBinding::inflate,
    diffCallbackBy { a, b -> a.messageId == b.messageId }
) {
    override fun bind(binding: ItemChatMessageBinding, item: ChatMessage) {
        val isMine = item.senderId == currentUid
        val context = binding.root.context

        val params = binding.cardBubble.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (isMine) Gravity.END else Gravity.START
        binding.cardBubble.layoutParams = params

        binding.cardBubble.setCardBackgroundColor(
            ContextCompat.getColor(context, if (isMine) R.color.primary else R.color.surface)
        )
        binding.tvMessage.setTextColor(
            ContextCompat.getColor(context, if (isMine) R.color.on_primary else R.color.on_surface)
        )
        binding.tvMessage.text = item.text
    }
}
