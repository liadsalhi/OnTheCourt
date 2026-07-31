package com.onthecourt.app.model

data class Game(
    val gameId: String = "",
    val creatorUid: String = "",
    val sport: String = "",
    val city: String = "",
    val courtName: String = "",
    val date: String = "",
    val time: String = "",
    val maxPlayers: Int = 0,
    val numTeams: Int = 0,
    val matchDurationMinutes: Int = 0,
    val description: String = "",
    val players: List<String> = emptyList(),
    val playerNames: Map<String, String> = emptyMap(),
    val isOpen: Boolean = true,
    val startTimestamp: Long = 0L,
    val createdAt: Long = 0L
)

// The single definition of "has this game already started" — a game with no
// startTimestamp (old data) is treated as never passed. Used by every screen that
// splits games into active/past or decides if joining is still possible.
fun Game.hasPassed(now: Long = System.currentTimeMillis()): Boolean =
    startTimestamp > 0L && now >= startTimestamp