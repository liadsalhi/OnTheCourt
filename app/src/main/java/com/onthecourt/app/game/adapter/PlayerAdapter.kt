package com.onthecourt.app.game.adapter

import android.view.View
import com.onthecourt.app.R
import com.onthecourt.app.databinding.ItemPlayerBinding
import com.onthecourt.app.util.AvatarHelper
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.diffCallbackBy

class PlayerAdapter(
    private val currentUid: String,
    private val onAddFriend: (String) -> Unit
) : BindingListAdapter<PlayerAdapter.PlayerItem, ItemPlayerBinding>(
    ItemPlayerBinding::inflate,
    diffCallbackBy { a, b -> a.uid == b.uid }
) {
    data class PlayerItem(
        val uid: String,
        val firstName: String,
        val lastName: String,
        val isCurrentUser: Boolean,
        val isFriend: Boolean
    )

    override fun bind(binding: ItemPlayerBinding, item: PlayerItem) {
        AvatarHelper.bind(binding.tvAvatar, binding.tvName, item.firstName, item.lastName, item.uid)

        val showAddFriend = !item.isCurrentUser && !item.isFriend
        binding.btnAddFriend.visibility = if (showAddFriend) View.VISIBLE else View.GONE
        binding.btnAddFriend.setOnClickListener {
            onAddFriend(item.uid)
            binding.btnAddFriend.isEnabled = false
            binding.btnAddFriend.text = binding.root.context.getString(R.string.status_sent)
        }
    }
}
