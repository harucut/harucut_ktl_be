package com.harucut.auth.exit.service

interface UserExitService {
    fun requestExit(userId: Long)
    fun exit(userId: Long)
    fun reActivate(userId: Long)
}