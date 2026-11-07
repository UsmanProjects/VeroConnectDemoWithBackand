package com.veroflow.veroconnect.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val website: String = "",
    val bio: String = "",
    val avatarColor: Color,
    val followers: Int = 0,
    val following: Int = 0,
    val postsCount: Int = 0,
    var isFollowing: Boolean = false,
    var isFriend: Boolean = false
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val initials: String get() = "${firstName.firstOrNull() ?: ' '}${lastName.firstOrNull() ?: ' '}".trim().uppercase()
    val avatarUrl: String
        get() = if (id == "me") MockRepository.myAvatarUrl.value else ImageUrls.avatar(id, firstName)
    val coverUrl: String get() = ImageUrls.cover(id)
}

enum class MediaType { NONE, IMAGE, VIDEO }

// The classes below keep their mutable fields in Compose state, so any screen that reads
// them recomposes automatically when they change (like, save, join, unread count, ...).

class Comment(
    val id: String,
    val authorId: String,
    text: String,
    likes: Int = 0,
    isLiked: Boolean = false,
    val timeAgo: String
) {
    var text by mutableStateOf(text)
    var likes by mutableIntStateOf(likes)
    var isLiked by mutableStateOf(isLiked)
}

class Post(
    val id: String,
    val authorId: String,
    text: String,
    val mediaType: MediaType,
    val mediaColor: Color,
    likeCount: Int,
    isLiked: Boolean = false,
    isSaved: Boolean = false,
    comments: List<Comment>,
    shareCount: Int,
    val timeAgo: String,
    val mediaUrl: String? = null
) {
    var text by mutableStateOf(text)
    var likeCount by mutableIntStateOf(likeCount)
    var isLiked by mutableStateOf(isLiked)
    var isSaved by mutableStateOf(isSaved)
    var shareCount by mutableIntStateOf(shareCount)
    val comments: SnapshotStateList<Comment> = comments.toMutableStateList()

    val imageUrl: String
        get() = mediaUrl ?: ImageUrls.post(id, mediaType == MediaType.VIDEO)
    val duration: String get() = ImageUrls.videoDuration(id)
}

class Story(
    val id: String,
    val userId: String,
    val color: Color,
    viewed: Boolean = false
) {
    var viewed by mutableStateOf(viewed)
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    var text: String,
    val isMine: Boolean,
    val timeAgo: String,
    val type: MediaType = MediaType.NONE
)

class Conversation(
    val id: String,
    val userId: String,
    lastMessage: String,
    time: String,
    unreadCount: Int,
    isArchived: Boolean = false,
    messages: List<ChatMessage>,
    isMuted: Boolean = false
) {
    var lastMessage by mutableStateOf(lastMessage)
    var time by mutableStateOf(time)
    var unreadCount by mutableIntStateOf(unreadCount)
    var isArchived by mutableStateOf(isArchived)
    var isMuted by mutableStateOf(isMuted)
    val messages: SnapshotStateList<ChatMessage> = messages.toMutableStateList()
}

class Group(
    val id: String,
    val name: String,
    val memberCount: Int,
    val color: Color,
    isJoined: Boolean = false
) {
    var isJoined by mutableStateOf(isJoined)
    val imageUrl: String get() = ImageUrls.group(id)
}

enum class NotificationType { LIKE, COMMENT, FRIEND_REQUEST, MENTION }

class AppNotification(
    val id: String,
    val type: NotificationType,
    val userId: String,
    val text: String,
    val timeAgo: String,
    isRead: Boolean = false
) {
    var isRead by mutableStateOf(isRead)
}

data class FriendRequest(
    val id: String,
    val userId: String
)

data class MediaItem(
    val id: String,
    val type: MediaType,
    val color: Color
) {
    val imageUrl: String get() = ImageUrls.media(id)
}
