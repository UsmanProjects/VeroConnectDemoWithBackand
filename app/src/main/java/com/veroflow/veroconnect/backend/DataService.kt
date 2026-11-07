package com.veroflow.veroconnect.backend

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.graphics.Color
import com.veroflow.veroconnect.data.ChatMessage
import com.veroflow.veroconnect.data.Comment
import com.veroflow.veroconnect.data.Conversation
import com.veroflow.veroconnect.data.Group
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.Post

/** "5m", "3h", "2d" style label for a stored timestamp. */
fun agoLabel(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0)) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> "${minutes / 1440}d"
    }
}

/**
 * Saves everything the signed-in account does and puts it back on top of the demo world
 * when that account signs in again. Guests are not saved (calls are ignored).
 */
class DataService(private val db: VeroDatabase) {

    companion object {
        const val LIKE = "like"
        const val SAVE = "save"
        const val COMMENT_LIKE = "comment_like"
        const val FRIEND = "friend"            // value true = friend, false = removed
        const val REQUEST_DONE = "request_done"
        const val FOLLOW = "follow"
        const val GROUP = "group"              // value true = joined, false = left
        const val NOTIF_READ = "notif_read"
        const val NOTIF_CLEARED = "notif_cleared"
        const val MEDIA_DELETED = "media_deleted"
        const val CONV_ARCHIVED = "conv_archived"
        const val CONV_MUTED = "conv_muted"
        const val CONV_DELETED = "conv_deleted"
        const val CONV_READ = "conv_read"
    }

    private fun owner(): Long? = Backend.accountId

    // ------------------------------------------------------------------ saving

    fun setFlag(kind: String, ref: String, value: Boolean) {
        val o = owner() ?: return
        putFlag(db.writableDatabase, o, kind, ref, value)
    }

    fun setFlags(kind: String, refs: Collection<String>, value: Boolean) {
        val o = owner() ?: return
        val d = db.writableDatabase
        d.beginTransaction()
        try {
            refs.forEach { putFlag(d, o, kind, it, value) }
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    private fun putFlag(d: SQLiteDatabase, o: Long, kind: String, ref: String, value: Boolean) {
        val cv = ContentValues().apply {
            put("account_id", o)
            put("kind", kind)
            put("ref", ref)
            put("value", if (value) 1 else 0)
        }
        d.insertWithOnConflict("flags", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun savePost(post: Post) {
        val o = owner() ?: return
        val cv = ContentValues().apply {
            put("id", post.id)
            put("account_id", o)
            put("text", post.text)
            put("media_type", post.mediaType.name)
            put("media_url", post.mediaUrl)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict("posts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveComment(postId: String, comment: Comment) {
        val o = owner() ?: return
        val cv = ContentValues().apply {
            put("id", comment.id)
            put("account_id", o)
            put("post_id", postId)
            put("text", comment.text)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict("comments", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateComment(id: String, text: String) {
        if (owner() == null) return
        val cv = ContentValues().apply { put("text", text) }
        db.writableDatabase.update("comments", cv, "id = ?", arrayOf(id))
    }

    fun deleteComment(id: String) {
        if (owner() == null) return
        db.writableDatabase.delete("comments", "id = ?", arrayOf(id))
    }

    fun saveMessage(conversationId: String, message: ChatMessage) {
        val o = owner() ?: return
        val cv = ContentValues().apply {
            put("id", message.id)
            put("account_id", o)
            put("conversation_id", conversationId)
            put("sender_id", message.senderId)
            put("text", message.text)
            put("is_mine", if (message.isMine) 1 else 0)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict("messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteMessage(id: String) {
        if (owner() == null) return
        db.writableDatabase.delete("messages", "id = ?", arrayOf(id))
    }

    /** Deleting a chat removes its saved messages and hides the demo history for it. */
    fun deleteConversation(conversationId: String) {
        val o = owner() ?: return
        db.writableDatabase.delete(
            "messages", "account_id = ? AND conversation_id = ?", arrayOf(o.toString(), conversationId)
        )
        setFlag(CONV_DELETED, conversationId, true)
    }

    fun saveGroup(group: Group) {
        val o = owner() ?: return
        val cv = ContentValues().apply {
            put("id", group.id)
            put("account_id", o)
            put("name", group.name)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict("groups_created", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------------------------------------------------------------- restoring

    private data class MsgRow(
        val conversationId: String,
        val id: String,
        val senderId: String,
        val text: String,
        val isMine: Boolean,
        val createdAt: Long
    )

    private fun loadFlags(o: Long): Map<String, Map<String, Boolean>> {
        val out = HashMap<String, MutableMap<String, Boolean>>()
        db.readableDatabase.rawQuery(
            "SELECT kind, ref, value FROM flags WHERE account_id = ?", arrayOf(o.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.getOrPut(c.getString(0)) { HashMap() }[c.getString(1)] = c.getInt(2) == 1
            }
        }
        return out
    }

    /** Applies the signed-in account's saved data on top of the freshly seeded demo world. */
    fun restore() {
        val o = owner() ?: return
        val flags = loadFlags(o)
        val d = db.readableDatabase
        val repo = MockRepository

        // Own posts (oldest first, each added to the top -> newest ends up first)
        d.rawQuery(
            "SELECT id, text, media_type, media_url, created_at FROM posts WHERE account_id = ? ORDER BY created_at ASC",
            arrayOf(o.toString())
        ).use { c ->
            while (c.moveToNext()) {
                repo.posts.add(
                    0,
                    Post(
                        id = c.getString(0),
                        authorId = "me",
                        text = c.getString(1),
                        mediaType = runCatching { MediaType.valueOf(c.getString(2)) }.getOrDefault(MediaType.NONE),
                        mediaColor = Color(0xFF6750A4),
                        likeCount = 0,
                        comments = emptyList(),
                        shareCount = 0,
                        timeAgo = agoLabel(c.getLong(4)),
                        mediaUrl = c.getString(3)
                    )
                )
            }
        }

        // Own comments
        d.rawQuery(
            "SELECT id, post_id, text, created_at FROM comments WHERE account_id = ? ORDER BY created_at ASC",
            arrayOf(o.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val post = repo.posts.find { it.id == c.getString(1) } ?: continue
                post.comments.add(
                    0,
                    Comment(
                        id = c.getString(0),
                        authorId = "me",
                        text = c.getString(2),
                        timeAgo = agoLabel(c.getLong(3))
                    )
                )
            }
        }

        // Likes, saves, comment likes
        val likes = flags[LIKE].orEmpty()
        val saves = flags[SAVE].orEmpty()
        val commentLikes = flags[COMMENT_LIKE].orEmpty()
        repo.posts.forEach { post ->
            if (likes[post.id] == true) {
                post.isLiked = true
                post.likeCount += 1
            }
            if (saves[post.id] == true) post.isSaved = true
            post.comments.forEach { comment ->
                if (commentLikes[comment.id] == true) {
                    comment.isLiked = true
                    comment.likes += 1
                }
            }
        }

        // Friends, requests, follows
        flags[FRIEND].orEmpty().forEach { (userId, isFriend) ->
            val user = repo.users.find { it.id == userId } ?: return@forEach
            user.isFriend = isFriend
            if (isFriend) {
                if (repo.friends.none { it.id == userId }) repo.friends.add(user)
            } else {
                repo.friends.removeAll { it.id == userId }
            }
        }
        flags[REQUEST_DONE].orEmpty().forEach { (userId, done) ->
            if (done) repo.friendRequests.removeAll { it.userId == userId }
        }
        flags[FOLLOW].orEmpty().forEach { (userId, following) ->
            repo.users.find { it.id == userId }?.isFollowing = following
        }

        // Groups
        flags[GROUP].orEmpty().forEach { (groupId, joined) ->
            repo.groups.find { it.id == groupId }?.isJoined = joined
        }
        d.rawQuery(
            "SELECT id, name FROM groups_created WHERE account_id = ? ORDER BY created_at ASC",
            arrayOf(o.toString())
        ).use { c ->
            while (c.moveToNext()) {
                repo.groups.add(
                    0,
                    Group(
                        id = c.getString(0),
                        name = c.getString(1),
                        memberCount = 1,
                        color = Color(0xFF6750A4),
                        isJoined = flags[GROUP]?.get(c.getString(0)) ?: true
                    )
                )
            }
        }

        // Notifications, media
        val cleared = flags[NOTIF_CLEARED].orEmpty()
        val read = flags[NOTIF_READ].orEmpty()
        repo.notifications.removeAll { cleared[it.id] == true }
        repo.notifications.forEach { if (read[it.id] == true) it.isRead = true }
        val deletedMedia = flags[MEDIA_DELETED].orEmpty()
        repo.mediaGallery.removeAll { deletedMedia[it.id] == true }

        // Chats
        flags[CONV_DELETED].orEmpty().forEach { (id, deleted) ->
            if (deleted) repo.conversations.removeAll { it.id == id }
        }
        val rows = ArrayList<MsgRow>()
        d.rawQuery(
            "SELECT conversation_id, id, sender_id, text, is_mine, created_at FROM messages WHERE account_id = ? ORDER BY created_at ASC",
            arrayOf(o.toString())
        ).use { c ->
            while (c.moveToNext()) {
                rows.add(MsgRow(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4) == 1, c.getLong(5)))
            }
        }
        val byConversation = rows.groupBy { it.conversationId }
        byConversation.forEach { (conversationId, list) ->
            val convo = repo.conversations.find { it.id == conversationId }
                ?: Conversation(
                    id = conversationId,
                    userId = conversationId.removePrefix("conv_"),
                    lastMessage = "",
                    time = "now",
                    unreadCount = 0,
                    messages = emptyList()
                ).also { repo.conversations.add(it) }
            list.forEach {
                convo.messages.add(
                    ChatMessage(
                        id = it.id,
                        senderId = it.senderId,
                        text = it.text,
                        isMine = it.isMine,
                        timeAgo = agoLabel(it.createdAt)
                    )
                )
            }
            convo.lastMessage = list.last().text
            convo.time = agoLabel(list.last().createdAt)
        }
        // Chats you wrote in go to the top, newest first.
        val touched = byConversation.keys
            .mapNotNull { id -> repo.conversations.find { it.id == id } }
            .sortedByDescending { byConversation[it.id]!!.last().createdAt }
        repo.conversations.removeAll(touched)
        repo.conversations.addAll(0, touched)

        val archived = flags[CONV_ARCHIVED].orEmpty()
        val muted = flags[CONV_MUTED].orEmpty()
        val convRead = flags[CONV_READ].orEmpty()
        repo.conversations.forEach { convo ->
            if (archived[convo.id] != null) convo.isArchived = archived[convo.id] == true
            if (muted[convo.id] != null) convo.isMuted = muted[convo.id] == true
            if (convRead[convo.id] == true) convo.unreadCount = 0
        }
    }
}
