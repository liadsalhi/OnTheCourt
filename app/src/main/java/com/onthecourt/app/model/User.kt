package com.onthecourt.app.model

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val city: String = "",
    val age: Int = 0,
    val email: String = "",
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList()
)

val User.fullName: String get() = "$firstName $lastName"