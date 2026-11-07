package com.veroflow.veroconnect.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.veroflow.veroconnect.backend.Account
import com.veroflow.veroconnect.backend.Backend
import kotlin.random.Random

/**
 * In-memory "world" of the app (other people, their posts, chats ...) plus the signed-in user.
 *
 * The world is generated from a FIXED seed, so ids and content are identical on every launch.
 * That is what lets the backend (see the `backend` package) save only what the account did
 * (likes, comments, messages, ...) and put it back after the next login.
 */
object MockRepository {

    private const val SEED = 20260101L
    private var rng = Random(SEED)

    private val palette = listOf(
        Color(0xFF6750A4), Color(0xFF00668B), Color(0xFF7D5260),
        Color(0xFF386A20), Color(0xFFB3261E), Color(0xFF9C4146),
        Color(0xFF006874), Color(0xFF8C4A2F), Color(0xFF4B5563),
        Color(0xFF0F766E), Color(0xFFA21CAF), Color(0xFF1D4ED8)
    )

    private val firstNames = listOf(
        "Ava", "Liam", "Noor", "Hassan", "Emma", "Omar", "Sara", "Bilal",
        "Zara", "Ali", "Maya", "Usman", "Layla", "Ahmed", "Nina", "Farhan",
        "Hana", "Imran", "Leila", "Yusuf", "Amber", "Kamran", "Sofia", "Tariq"
    )
    private val lastNames = listOf(
        "Khan", "Malik", "Sheikh", "Ahmed", "Raza", "Iqbal", "Butt", "Chaudhry",
        "Hussain", "Javed", "Qureshi", "Farooq", "Baig", "Siddiqui", "Aziz", "Naveed"
    )

    private val postCaptions = listOf(
        "Beautiful sunset today \uD83C\uDF05", "Coffee first, thoughts later ☕",
        "Weekend adventures begin!", "Can't stop smiling today \uD83D\uDE0A",
        "New project launched — excited to share more soon.",
        "Throwback to last summer's trip.", "Grateful for good friends and good food.",
        "Working on something new in the studio.", "Rainy days call for good books.",
        "Just finished a 10k run — feeling great!", "City lights never get old.",
        "Homemade pasta night \uD83C\uDF5D", "Exploring the mountains this weekend.",
        "Big news coming soon, stay tuned!", "Nothing beats a good cup of tea.",
        "New gear, same passion.", "Late night coding session.",
        "Family time is the best time.", "Trying out a new recipe today.",
        "Concert night was unforgettable!"
    )

    private fun guestUser() = User(
        id = "me",
        firstName = "Guest",
        lastName = "User",
        username = "guest",
        email = "",
        bio = "You are browsing as a guest. Register to save your activity.",
        avatarColor = Color(0xFF6750A4)
    )

    /** The user who is using the app right now (a registered account, or a guest). */
    var currentUser by mutableStateOf(guestUser())

    var users: List<User> = emptyList()
        private set

    fun userById(id: String): User =
        if (id == "me") currentUser else users.find { it.id == id } ?: currentUser

    val posts: MutableList<Post> = mutableStateListOf()
    val stories: MutableList<Story> = mutableStateListOf()
    val groups: MutableList<Group> = mutableStateListOf()
    val friendRequests: MutableList<FriendRequest> = mutableStateListOf()
    val friends: MutableList<User> = mutableStateListOf()
    val conversations: MutableList<Conversation> = mutableStateListOf()
    val notifications: MutableList<AppNotification> = mutableStateListOf()
    val mediaGallery: MutableList<MediaItem> = mutableStateListOf()

    // ---- Session / settings state ----
    var isLoggedIn = mutableStateOf(false)
    var isGuest = mutableStateOf(false)
    var rememberMe = mutableStateOf(false)
    var darkMode = mutableStateOf(false)
    var notificationsEnabled = mutableStateOf(true)
    var locationEnabled = mutableStateOf(false)
    var autoPlayVideos = mutableStateOf(true)
    var language = mutableStateOf("English")

    val recentSearches = mutableStateListOf("android testing", "veroflow", "material design")

    fun unreadNotificationCount(): Int = notifications.count { !it.isRead }
    fun totalUnreadChats(): Int = conversations.filter { !it.isArchived }.sumOf { it.unreadCount }

    // ---- Profile picture + one-shot toast shown by the next screen ----
    val myAvatarUrl = mutableStateOf(ImageUrls.myAvatarOptions.first())
    val toast = mutableStateOf<String?>(null)

    // ------------------------------------------------------------------ world

    private fun buildUsers(): List<User> = (1..40).map { i ->
        val f = firstNames[i % firstNames.size]
        val l = lastNames[(i * 3) % lastNames.size]
        User(
            id = "u$i",
            firstName = f,
            lastName = l,
            username = "${f.lowercase()}.${l.lowercase()}$i",
            email = "${f.lowercase()}.${l.lowercase()}$i@example.com",
            phone = "+1 555 01${i.toString().padStart(2, '0')}",
            bio = "Just another day in the life of $f.",
            avatarColor = palette[i % palette.size],
            followers = (20..3000).random(rng),
            following = (10..800).random(rng),
            postsCount = (1..120).random(rng),
            isFollowing = i % 3 == 0,
            isFriend = i % 4 == 0
        )
    }

    private fun buildPosts(): List<Post> {
        val list = mutableListOf<Post>()
        for (i in 1..120) {
            val author = users[i % users.size]
            val hasMedia = i % 3 != 0
            val mediaType = if (!hasMedia) MediaType.NONE else if (i % 2 == 0) MediaType.IMAGE else MediaType.VIDEO
            val commentCount = (0..4).random(rng)
            val comments = (1..commentCount).map { c ->
                Comment(
                    id = "p${i}c$c",
                    authorId = users[(i + c) % users.size].id,
                    text = "This is great! #$c",
                    likes = (0..20).random(rng),
                    timeAgo = "${c}h"
                )
            }
            list.add(
                Post(
                    id = "post$i",
                    authorId = author.id,
                    text = postCaptions[i % postCaptions.size],
                    mediaType = mediaType,
                    mediaColor = palette[(i * 7) % palette.size],
                    likeCount = (0..500).random(rng),
                    isLiked = false,
                    comments = comments,
                    shareCount = (0..80).random(rng),
                    timeAgo = "${(1..23).random(rng)}h"
                )
            )
        }
        return list
    }

    private fun buildGroups(): List<Group> = (1..14).map { i ->
        Group(
            id = "g$i",
            name = listOf(
                "Android Developers", "Photography Lovers", "Travel Enthusiasts",
                "Book Club", "Fitness Freaks", "Home Cooking", "Startup Founders",
                "Music Makers", "Gamers United", "Art & Design", "Pet Owners",
                "Movie Buffs", "Hiking Club", "Tech Talk"
            )[i - 1],
            memberCount = (50..12000).random(rng),
            color = palette[i % palette.size],
            isJoined = i % 3 == 0
        )
    }

    private fun buildConversations(): List<Conversation> = users.take(12).mapIndexed { idx, u ->
        val msgs = mutableListOf<ChatMessage>()
        val lines = listOf(
            "Hey! How are you?", "Are we still on for tomorrow?",
            "Sounds good \uD83D\uDC4D", "Sent you the file.", "Haha that's hilarious",
            "See you soon!", "Can you call me later?", "Thanks a lot!"
        )
        for (m in 0..4) {
            msgs.add(
                ChatMessage(
                    id = "${u.id}_m$m",
                    senderId = if (m % 2 == 0) u.id else "me",
                    text = lines[(m + idx) % lines.size],
                    isMine = m % 2 != 0,
                    timeAgo = "${m + 1}m"
                )
            )
        }
        Conversation(
            id = "conv_${u.id}",
            userId = u.id,
            lastMessage = msgs.last().text,
            time = "${(1..59).random(rng)}m",
            unreadCount = if (idx % 3 == 0) (1..5).random(rng) else 0,
            messages = msgs
        )
    }

    private fun buildNotifications(): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        val types = NotificationType.values()
        for (i in 1..25) {
            val user = users[i % users.size]
            val type = types[i % types.size]
            val text = when (type) {
                NotificationType.LIKE -> "liked your post"
                NotificationType.COMMENT -> "commented on your post"
                NotificationType.FRIEND_REQUEST -> "sent you a friend request"
                NotificationType.MENTION -> "mentioned you in a comment"
            }
            list.add(
                AppNotification(
                    id = "n$i",
                    type = type,
                    userId = user.id,
                    text = text,
                    timeAgo = "${i}h",
                    isRead = i > 8
                )
            )
        }
        return list
    }

    private fun buildMedia(): List<MediaItem> = (1..30).map { i ->
        MediaItem(
            id = "media$i",
            type = if (i % 4 == 0) MediaType.VIDEO else MediaType.IMAGE,
            color = palette[i % palette.size]
        )
    }

    /** Rebuilds the whole demo world exactly as it is on a fresh install. */
    fun resetWorld() {
        rng = Random(SEED)
        users = buildUsers()
        posts.clear(); posts.addAll(buildPosts())
        stories.clear(); stories.addAll(users.take(15).map { Story(id = "story_${it.id}", userId = it.id, color = it.avatarColor) })
        groups.clear(); groups.addAll(buildGroups())
        friendRequests.clear(); friendRequests.addAll(users.drop(20).take(8).map { FriendRequest(id = "fr_${it.id}", userId = it.id) })
        friends.clear(); friends.addAll(users.filter { it.isFriend })
        conversations.clear(); conversations.addAll(buildConversations())
        notifications.clear(); notifications.addAll(buildNotifications())
        mediaGallery.clear(); mediaGallery.addAll(buildMedia())
    }

    fun getOrCreateConversation(userId: String): Conversation {
        conversations.find { it.userId == userId }?.let {
            it.isArchived = false
            return it
        }
        val created = Conversation(
            id = "conv_$userId",
            userId = userId,
            lastMessage = "Say hi \uD83D\uDC4B",
            time = "now",
            unreadCount = 0,
            messages = emptyList()
        )
        conversations.add(0, created)
        return created
    }

    // ------------------------------------------------------------------ sessions

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    /** Call once from MainActivity (after Backend.init). Restores settings and, with "Remember me", the login. */
    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        val p = context.applicationContext.getSharedPreferences("veroconnect_prefs", Context.MODE_PRIVATE)
        prefs = p
        rememberMe.value = p.getBoolean("remember_me", false)
        darkMode.value = p.getBoolean("dark_mode", false)
        notificationsEnabled.value = p.getBoolean("notifications", true)
        locationEnabled.value = p.getBoolean("location", false)
        autoPlayVideos.value = p.getBoolean("autoplay", true)
        language.value = p.getString("language", "English") ?: "English"

        val savedId = p.getLong("account_id", -1L)
        if (rememberMe.value && p.getBoolean("logged_in", false) && savedId != -1L) {
            Backend.auth.accountById(savedId)?.let { startSession(it) }
        }
    }

    /** Signs an account in: rebuilds the demo world and puts the account's saved activity back. */
    fun startSession(account: Account) {
        resetWorld()
        Backend.accountId = account.id
        currentUser = User(
            id = "me",
            firstName = account.firstName,
            lastName = account.lastName,
            username = account.username,
            email = account.email,
            phone = account.phone,
            address = account.address,
            website = account.website,
            bio = account.bio,
            avatarColor = palette[(account.id % palette.size).toInt()],
            followers = account.followers
        )
        myAvatarUrl.value = account.avatarUrl ?: ImageUrls.myAvatarOptions.first()
        Backend.data.restore()
        isLoggedIn.value = true
        isGuest.value = false
        saveSession()
    }

    fun startGuest() {
        resetWorld()
        Backend.accountId = null
        currentUser = guestUser()
        myAvatarUrl.value = ImageUrls.myAvatarOptions.first()
        isGuest.value = true
        isLoggedIn.value = false
        saveSession()
    }

    fun endSession() {
        Backend.accountId = null
        isLoggedIn.value = false
        isGuest.value = false
        resetWorld()
        currentUser = guestUser()
        myAvatarUrl.value = ImageUrls.myAvatarOptions.first()
        saveSession()
    }

    /** Settings > Clear all data: removes every account and saved activity, then signs out. */
    fun clearAllData() {
        Backend.wipeAll()
        prefs?.edit()?.clear()?.apply()
        rememberMe.value = false
        darkMode.value = false
        notificationsEnabled.value = true
        locationEnabled.value = false
        autoPlayVideos.value = true
        language.value = "English"
        endSession()
    }

    fun saveSession() {
        prefs?.edit()
            ?.putBoolean("remember_me", rememberMe.value)
            ?.putBoolean("logged_in", isLoggedIn.value)
            ?.putLong("account_id", Backend.accountId ?: -1L)
            ?.apply()
    }

    fun saveSettings() {
        prefs?.edit()
            ?.putBoolean("dark_mode", darkMode.value)
            ?.putBoolean("notifications", notificationsEnabled.value)
            ?.putBoolean("location", locationEnabled.value)
            ?.putBoolean("autoplay", autoPlayVideos.value)
            ?.putString("language", language.value)
            ?.apply()
    }

    /** Saves the edited profile of the signed-in account to the database. */
    fun saveProfile() {
        val id = Backend.accountId ?: return
        val u = currentUser
        Backend.auth.updateProfile(
            id = id,
            firstName = u.firstName,
            lastName = u.lastName,
            bio = u.bio,
            phone = u.phone,
            address = u.address,
            website = u.website,
            avatarUrl = myAvatarUrl.value
        )
    }

    init {
        resetWorld()
    }
}
