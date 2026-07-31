package com.onthecourt.app.util

import android.graphics.Color
import android.widget.TextView
import kotlin.math.absoluteValue

object AvatarHelper {

    private val colors = listOf(
        "#0D2B55", "#1A3A6B", "#F47C20", "#2E7D32", "#6A1B9A", "#00838F"
    )

    fun getInitials(firstName: String, lastName: String): String {
        val first = firstName.firstOrNull()?.uppercaseChar() ?: ' '
        val last = lastName.firstOrNull()?.uppercaseChar() ?: ' '
        return "$first$last".trim()
    }

    fun getColorInt(uid: String): Int {
        val hex = colors[uid.hashCode().absoluteValue % colors.size]
        return Color.parseColor(hex)
    }

    // Fills in an initials avatar + name label. Every list row that shows a
    // user (friend, friend request, search result, player) needs this same
    // pair of views set up, so it lives here once instead of in each adapter.
    fun bind(avatarView: TextView, nameView: TextView, firstName: String, lastName: String, uid: String) {
        avatarView.text = getInitials(firstName, lastName)
        avatarView.background.mutate().setTint(getColorInt(uid))
        nameView.text = "$firstName $lastName"
    }
}