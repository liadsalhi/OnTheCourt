package com.onthecourt.app.game.adapter

import androidx.core.content.ContextCompat
import com.onthecourt.app.R
import com.onthecourt.app.databinding.ItemGameBinding
import com.onthecourt.app.model.Game
import com.onthecourt.app.util.BindingListAdapter
import com.onthecourt.app.util.SportUtil
import com.onthecourt.app.util.diffCallbackBy

class GameAdapter(
    private val onClick: (Game) -> Unit
) : BindingListAdapter<Game, ItemGameBinding>(
    ItemGameBinding::inflate,
    diffCallbackBy { a, b -> a.gameId == b.gameId }
) {
    override fun bind(binding: ItemGameBinding, item: Game) {
        val context = binding.root.context
        binding.tvSportEmoji.text = SportUtil.emoji(item.sport)
        binding.tvCourtName.text = item.courtName
        binding.tvTime.text = context.getString(R.string.date_time_format, item.date, item.time)
        if (item.isOpen) {
            binding.tvPlayerCount.text = context.getString(
                R.string.players_count, item.players.size, item.maxPlayers
            )
            binding.tvPlayerCount.setTextColor(ContextCompat.getColor(context, R.color.secondary))
        } else {
            binding.tvPlayerCount.text = context.getString(R.string.label_registration_closed)
            binding.tvPlayerCount.setTextColor(ContextCompat.getColor(context, R.color.error))
        }
        binding.root.setOnClickListener { onClick(item) }
    }
}
