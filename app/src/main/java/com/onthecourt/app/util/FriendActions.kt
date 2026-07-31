package com.onthecourt.app.util

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FriendActions {

    // Sends a friend request from fromUid to targetUid by appending to the
    // target's friendRequests list. Used by both the friend search screen and
    // the "add friend" button on a game's player roster.
    //
    // Reads the target's current friends list fresh from the server (instead of
    // trusting a possibly-stale local list) so a request can never be sent to
    // someone who is already a friend, no matter which screen triggers it.
    fun sendRequest(
        db: FirebaseFirestore,
        fromUid: String,
        targetUid: String,
        onAlreadyFriends: () -> Unit,
        onAlreadySent: () -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(targetUid).get()
            .addOnSuccessListener { doc ->
                val friends = doc.get("friends") as? List<String> ?: emptyList()
                if (friends.contains(fromUid)) {
                    onAlreadyFriends()
                    return@addOnSuccessListener
                }
                val requests = (doc.get("friendRequests") as? List<String> ?: emptyList()).toMutableList()
                if (requests.contains(fromUid)) {
                    onAlreadySent()
                } else {
                    requests.add(fromUid)
                    db.collection("users").document(targetUid)
                        .update("friendRequests", requests)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener(onFailure)
                }
            }
            .addOnFailureListener(onFailure)
    }

    // Accepts a pending friend request: adds each user to the other's friends list and
    // removes the request. Uses arrayUnion/arrayRemove (not read-modify-write on a cached
    // list) so a request arriving at the same moment isn't silently lost.
    fun acceptRequest(
        db: FirebaseFirestore,
        myUid: String,
        requesterUid: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val myRef = db.collection("users").document(myUid)
        val theirRef = db.collection("users").document(requesterUid)
        val batch = db.batch()
        batch.update(myRef, "friends", FieldValue.arrayUnion(requesterUid))
        batch.update(myRef, "friendRequests", FieldValue.arrayRemove(requesterUid))
        batch.update(theirRef, "friends", FieldValue.arrayUnion(myUid))
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    fun declineRequest(
        db: FirebaseFirestore,
        myUid: String,
        requesterUid: String,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(myUid)
            .update("friendRequests", FieldValue.arrayRemove(requesterUid))
            .addOnFailureListener(onFailure)
    }
}