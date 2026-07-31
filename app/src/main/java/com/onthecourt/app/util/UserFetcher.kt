package com.onthecourt.app.util

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.onthecourt.app.model.User

object UserFetcher {

    // Firestore's whereIn() only accepts up to 10 values per query. Ids are
    // split into batches of 10 and the results of all batches are merged, so
    // this still returns everyone even for a user with more than 10 friends.
    private const val BATCH_SIZE = 10

    fun fetchByIds(db: FirebaseFirestore, uids: List<String>, onResult: (List<User>) -> Unit) {
        if (uids.isEmpty()) {
            onResult(emptyList())
            return
        }
        // Matching by document id (not a separate "uid" field) means this can't miss a
        // user whose "uid" field was ever left blank or out of sync with its doc id.
        val queries = uids.chunked(BATCH_SIZE).map { batch ->
            db.collection("users").whereIn(FieldPath.documentId(), batch).get()
        }
        Tasks.whenAllSuccess<QuerySnapshot>(queries)
            .addOnSuccessListener { snapshots ->
                val users = snapshots.flatMap { it.documents }.mapNotNull { it.toObject(User::class.java) }
                onResult(users)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}