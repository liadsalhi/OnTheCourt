package com.onthecourt.app.util

import android.content.Context
import com.onthecourt.app.R

object SportUtil {

    fun displayName(context: Context, sport: String): String = when (sport) {
        "soccer" -> context.getString(R.string.sport_soccer)
        "basketball" -> context.getString(R.string.sport_basketball)
        "tennis" -> context.getString(R.string.sport_tennis)
        else -> sport
    }

    fun emoji(sport: String): String = when (sport) {
        "soccer" -> "⚽"
        "basketball" -> "🏀"
        "tennis" -> "🎾"
        else -> "🏟"
    }
}
