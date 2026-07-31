package com.onthecourt.app.map.adapter

import android.view.View
import com.onthecourt.app.databinding.ItemCourtBinding
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.SportUtil
import com.onthecourt.app.util.diffCallbackBy

class CourtAdapter(
    private val sport: String,
    private val onClick: (CourtListItem) -> Unit
) : BindingListAdapter<CourtAdapter.CourtListItem, ItemCourtBinding>(
    ItemCourtBinding::inflate,
    diffCallbackBy { a, b -> a.latLng == b.latLng }
) {
    data class CourtListItem(
        val name: String,
        val address: String,
        val latLng: String
    )

    override fun bind(binding: ItemCourtBinding, item: CourtListItem) {
        binding.tvSportEmoji.text = SportUtil.emoji(sport)
        binding.tvCourtName.text = item.name
        binding.tvAddress.text = item.address
        binding.tvAddress.visibility = if (item.address.isEmpty()) View.GONE else View.VISIBLE
        binding.root.setOnClickListener { onClick(item) }
    }
}
