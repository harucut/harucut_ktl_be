package com.harucut.util.mail.service

interface MailService {

    fun sendEmail(to: String, subject: String, text: String, isHtml: Boolean = false)
}