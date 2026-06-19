package com.harucut.auth.exit.handler

interface UserDeletionHandler {
    fun handleUserDeletion(userId: Long)
}