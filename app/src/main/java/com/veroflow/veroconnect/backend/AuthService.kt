package com.veroflow.veroconnect.backend

import android.content.ContentValues
import android.database.Cursor
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 password hashing. Passwords are never stored in plain text. */
object PasswordHasher {
    private const val ITERATIONS = 12_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return encode(factory.generateSecret(spec).encoded)
    }

    fun verify(password: String, saltText: String, expectedHash: String): Boolean {
        val actual = hash(password, decode(saltText))
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }
}

data class Account(
    val id: Long,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val bio: String,
    val phone: String,
    val address: String,
    val website: String,
    val avatarUrl: String?,
    val followers: Int
)

sealed class AuthResult {
    data class Success(val account: Account) : AuthResult()
    /** [field] tells the screen which input to mark ("email", "username", "password"), or null. */
    data class Failure(val field: String?, val message: String) : AuthResult()
}

/** Registration, login and profile updates, backed by the accounts table. */
class AuthService(private val db: VeroDatabase) {

    fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        phone: String,
        password: String
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim()
        if (exists("email", cleanEmail)) {
            return AuthResult.Failure("email", "An account with this email already exists")
        }
        if (exists("username", cleanUsername)) {
            return AuthResult.Failure("username", "This username is already taken")
        }
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("email", cleanEmail)
            put("username", cleanUsername)
            put("password_hash", PasswordHasher.hash(password, salt))
            put("salt", PasswordHasher.encode(salt))
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("phone", phone.trim())
            put("followers", 0)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.writableDatabase.insert("accounts", null, values)
        if (id == -1L) return AuthResult.Failure(null, "Could not create the account. Please try again.")
        // Give every new account a profile picture.
        val avatar = com.veroflow.veroconnect.data.ImageUrls.myAvatarOptions[(id % 8).toInt()]
        db.writableDatabase.execSQL("UPDATE accounts SET avatar_url = ? WHERE id = ?", arrayOf(avatar, id))
        return AuthResult.Success(accountById(id)!!)
    }

    fun login(email: String, password: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        db.readableDatabase.rawQuery(
            "SELECT id, password_hash, salt FROM accounts WHERE email = ?",
            arrayOf(cleanEmail)
        ).use { c ->
            if (!c.moveToFirst()) {
                return AuthResult.Failure("email", "No account found with this email. Please register first.")
            }
            val id = c.getLong(0)
            if (!PasswordHasher.verify(password, c.getString(2), c.getString(1))) {
                return AuthResult.Failure("password", "Incorrect password. Please try again.")
            }
            return AuthResult.Success(accountById(id)!!)
        }
    }

    fun accountById(id: Long): Account? {
        db.readableDatabase.rawQuery(
            """SELECT id, email, username, first_name, last_name, bio, phone, address, website,
                      avatar_url, followers FROM accounts WHERE id = ?""",
            arrayOf(id.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.toAccount() else null
        }
    }

    fun updateProfile(
        id: Long,
        firstName: String,
        lastName: String,
        bio: String,
        phone: String,
        address: String,
        website: String,
        avatarUrl: String?
    ) {
        val values = ContentValues().apply {
            put("first_name", firstName)
            put("last_name", lastName)
            put("bio", bio)
            put("phone", phone)
            put("address", address)
            put("website", website)
            put("avatar_url", avatarUrl)
        }
        db.writableDatabase.update("accounts", values, "id = ?", arrayOf(id.toString()))
    }

    /** Returns null on success, or an error message. */
    fun changePassword(id: Long, oldPassword: String, newPassword: String): String? {
        db.readableDatabase.rawQuery(
            "SELECT password_hash, salt FROM accounts WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (!c.moveToFirst()) return "Account not found"
            if (!PasswordHasher.verify(oldPassword, c.getString(1), c.getString(0))) {
                return "Current password is incorrect"
            }
        }
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("password_hash", PasswordHasher.hash(newPassword, salt))
            put("salt", PasswordHasher.encode(salt))
        }
        db.writableDatabase.update("accounts", values, "id = ?", arrayOf(id.toString()))
        return null
    }

    private fun exists(column: String, value: String): Boolean {
        db.readableDatabase.rawQuery(
            "SELECT 1 FROM accounts WHERE $column = ? LIMIT 1",
            arrayOf(value)
        ).use { return it.moveToFirst() }
    }

    private fun Cursor.toAccount() = Account(
        id = getLong(0),
        email = getString(1),
        username = getString(2),
        firstName = getString(3),
        lastName = getString(4),
        bio = getString(5),
        phone = getString(6),
        address = getString(7),
        website = getString(8),
        avatarUrl = getString(9),
        followers = getInt(10)
    )
}
