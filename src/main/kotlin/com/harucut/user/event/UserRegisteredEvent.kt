package com.harucut.user.event

import com.harucut.user.entity.User

data class UserRegisteredEvent(
    val user: User
)
