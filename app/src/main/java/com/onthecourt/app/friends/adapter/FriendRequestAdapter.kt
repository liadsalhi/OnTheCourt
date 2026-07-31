package com.onthecourt.app.friends.adapter

import com.onthecourt.app.databinding.ItemFriendRequestBinding
import com.onthecourt.app.model.User
import com.onthecourt.app.util.AvatarHelper
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.diffCallbackBy

class FriendRequestAdapter(
    private val onAccept: (User) -> Unit,
    private val onDecline: (User) -> Unit
) : BindingListAdapter<User, ItemFriendRequestBinding>(
    ItemFriendRequestBinding::inflate,
    diffCallbackBy { a, b -> a.uid == b.uid }
) {
    override fun bind(binding: ItemFriendRequestBinding, item: User) {
        AvatarHelper.bind(binding.tvAvatar, binding.tvName, item.firstName, item.lastName, item.uid)
        binding.btnAccept.setOnClickListener {
            onAccept(item)
            binding.btnAccept.isEnabled = false
            binding.btnDecline.isEnabled = false
        }
        binding.btnDecline.setOnClickListener {
            onDecline(item)
            binding.btnAccept.isEnabled = false
            binding.btnDecline.isEnabled = false
        }
    }
}
