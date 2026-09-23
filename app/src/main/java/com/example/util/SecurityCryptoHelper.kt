package com.example.util

import java.security.MessageDigest

object SecurityCryptoHelper {
    private const val SALT = "NCCK_CHURCH_ADMIN_P1_SALT_2026_SECURE"

    /**
     * Hashes password using SHA-256 with static application salt.
     */
    fun hashPassword(password: String): String {
        val clean = password.trim()
        if (clean.isEmpty()) return ""
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((SALT + clean).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Standard SHA-256 hash
     */
    fun sha256(input: String): String {
        val clean = input.trim()
        if (clean.isEmpty()) return ""
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(clean.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies plain text password against stored hash or plain text legacy PIN.
     */
    fun verifyPassword(inputPassword: String, storedHashOrPlain: String): Boolean {
        val cleanInput = inputPassword.trim()
        val cleanStored = storedHashOrPlain.trim()
        if (cleanInput.isEmpty() || cleanStored.isEmpty()) return false
        val hashedInput = hashPassword(cleanInput)
        return cleanStored == hashedInput || cleanStored == cleanInput
    }

    /**
     * Generates a 6-digit confirmation OTP for P2.
     */
    fun generateConfirmationOtp(): String {
        return (100000..999999).random().toString()
    }
}
