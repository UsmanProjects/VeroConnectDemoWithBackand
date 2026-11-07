package com.veroflow.veroconnect.backend

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The app's database: one SQLite file (veroconnect.db) stored privately on the phone.
 * Created automatically on first launch - nothing to install or run.
 *
 *  accounts        registered users (password stored as salted PBKDF2 hash, never plain text)
 *  posts           posts created by an account
 *  comments        comments written by an account
 *  messages        chat messages sent / received by an account
 *  groups_created  groups created by an account
 *  flags           everything else an account did: likes, saves, friends, follows, joined groups,
 *                  read notifications, archived chats ... (kind + ref + value)
 */
class VeroDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE accounts(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL DEFAULT '',
                bio TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                website TEXT NOT NULL DEFAULT '',
                avatar_url TEXT,
                followers INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE posts(
                id TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                media_type TEXT NOT NULL,
                media_url TEXT,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE comments(
                id TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL,
                post_id TEXT NOT NULL,
                text TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE messages(
                id TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL,
                conversation_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                text TEXT NOT NULL,
                is_mine INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE groups_created(
                id TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE flags(
                account_id INTEGER NOT NULL,
                kind TEXT NOT NULL,
                ref TEXT NOT NULL,
                value INTEGER NOT NULL,
                PRIMARY KEY(account_id, kind, ref)
            )"""
        )
        db.execSQL("CREATE INDEX idx_posts_account ON posts(account_id)")
        db.execSQL("CREATE INDEX idx_comments_account ON comments(account_id)")
        db.execSQL("CREATE INDEX idx_messages_account ON messages(account_id)")

        seedDemoAccount(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 only - nothing to migrate yet.
    }

    /** A ready-made account so the app can be tried straight away. */
    private fun seedDemoAccount(db: SQLiteDatabase) {
        val salt = PasswordHasher.newSalt()
        val values = ContentValues().apply {
            put("email", DEMO_EMAIL)
            put("username", "alex.morgan")
            put("password_hash", PasswordHasher.hash(DEMO_PASSWORD, salt))
            put("salt", PasswordHasher.encode(salt))
            put("first_name", "Alex")
            put("last_name", "Morgan")
            put("bio", "Exploring the world, one post at a time.")
            put("phone", "+1 555 0100")
            put("avatar_url", "https://randomuser.me/api/portraits/men/32.jpg")
            put("followers", 482)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("accounts", null, values)
    }

    companion object {
        const val DB_NAME = "veroconnect.db"
        const val DB_VERSION = 1
        const val DEMO_EMAIL = "alex.morgan@example.com"
        const val DEMO_PASSWORD = "Password123"
    }
}
