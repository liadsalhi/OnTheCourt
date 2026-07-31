package com.onthecourt.app.friends.adapter

import com.onthecourt.app.databinding.ItemFriendBinding
import com.onthecourt.app.model.User
import com.onthecourt.app.util.AvatarHelper
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.diffCallbackBy

class FriendAdapter(
    private val onInvite: (User) -> Unit,
    private val onClick: (User) -> Unit
) : BindingListAdapter<User, ItemFriendBinding>(
    ItemFriendBinding::inflate,
    diffCallbackBy { a, b -> a.uid == b.uid }
) {
    override fun bind(binding: ItemFriendBinding, item: User) {
        AvatarHelper.bind(binding.tvAvatar, binding.tvName, item.firstName, item.lastName, item.uid)
        binding.btnInvite.setOnClickListener { onInvite(item) }
        binding.btnChat.setOnClickListener { onClick(item) }
        binding.root.setOnClickListener { onClick(item) }
    }
}
