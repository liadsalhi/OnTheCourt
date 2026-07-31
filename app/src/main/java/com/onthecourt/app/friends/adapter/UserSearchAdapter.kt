package com.onthecourt.app.friends.adapter

import com.onthecourt.app.R
import com.onthecourt.app.databinding.ItemUserSearchBinding
import com.onthecourt.app.model.User
import com.onthecourt.app.util.AvatarHelper
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.diffCallbackBy

class UserSearchAdapter(
    private val onClick: (User) -> Unit
) : BindingListAdapter<User, ItemUserSearchBinding>(
    ItemUserSearchBinding::inflate,
    diffCallbackBy { a, b -> a.uid == b.uid }
) {
    override fun bind(binding: ItemUserSearchBinding, item: User) {
        AvatarHelper.bind(binding.tvAvatar, binding.tvName, item.firstName, item.lastName, item.uid)
        binding.tvDetails.text = binding.root.context.getString(
            R.string.result_age_city_format, item.age, item.city
        )
        binding.root.setOnClickListener { onClick(item) }
    }
}
