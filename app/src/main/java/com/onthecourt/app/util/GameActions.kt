package com.onthecourt.app.util

import com.google.firebase.firestore.FirebaseFirestore
import com.onthecourt.app.model.Game

object GameActions {

    // Adds a player to a game: updates the players list and playerNames map,
    // and closes registration once the game is full. Used both when a user
    // joins a game themselves and when a friend invites them into one.
    fun addPlayer(
        db: FirebaseFirestore,
        game: Game,
        uid: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val newPlayers = game.players.toMutableList().also { it.add(uid) }
        val newPlayerNames = game.playerNames.toMutableMap().also { it[uid] = name }
        val updates = hashMapOf<String, Any>(
            "players" to newPlayers,
            "playerNames" to newPlayerNames,
            "open" to (newPlayers.size < game.maxPlayers)
        )
        db.collection("games").document(game.gameId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }
}