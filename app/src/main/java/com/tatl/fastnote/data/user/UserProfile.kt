package com.tatl.fastnote.data.user

enum class AccountType {
    GUEST,
    GOOGLE
}

data class UserProfile(
    val userId: String,
    val userName: String,
    val email: String,
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val accountType: AccountType = AccountType.GUEST
)
