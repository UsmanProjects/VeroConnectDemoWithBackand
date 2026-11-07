package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.ChatMessage
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val cannedReplies = listOf(
    "Haha nice!", "Got it \uD83D\uDC4D", "Sounds good to me", "Let me check and get back to you.",
    "Awesome, thanks!", "Can we talk later?", "On my way!", "Love that \u2764\uFE0F"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(conversationId: String, onBack: () -> Unit) {
    val convo = remember { MockRepository.conversations.find { it.id == conversationId } }
    val user = convo?.let { MockRepository.userById(it.userId) }
    // Messages are Compose-state backed (SnapshotStateList): the list updates itself.
    val messages = convo?.messages
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
    var optionsFor by remember { mutableStateOf<ChatMessage?>(null) }
    var showEmoji by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun sendMessage(text: String, simulateReply: Boolean = true) {
        if (convo == null || text.isBlank()) return
        val mine = ChatMessage(
            id = "m_new_${System.currentTimeMillis()}",
            senderId = "me",
            text = text,
            isMine = true,
            timeAgo = "now"
        )
        convo.messages.add(mine)
        Backend.data.saveMessage(convo.id, mine)
        convo.lastMessage = text
        convo.time = "now"
        // Move this chat to the top of the chat list.
        MockRepository.conversations.remove(convo)
        MockRepository.conversations.add(0, convo)
        scope.launch {
            listState.animateScrollToItem(0)
            if (simulateReply) {
                delay(600)
                isTyping = true
                listState.animateScrollToItem(0)
                delay(1500)
                val reply = cannedReplies[convo.messages.size % cannedReplies.size]
                val replyMessage = ChatMessage(
                    id = "m_reply_${System.currentTimeMillis()}",
                    senderId = convo.userId,
                    text = reply,
                    isMine = false,
                    timeAgo = "now"
                )
                convo.messages.add(replyMessage)
                Backend.data.saveMessage(convo.id, replyMessage)
                convo.lastMessage = reply
                isTyping = false
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user != null) {
                            Avatar(user, 36)
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(user?.fullName ?: "Chat", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                if (isTyping) "typing…" else "Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Sits above the system navigation bar AND the keyboard, so it is never half hidden.
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                ) {
                    replyTo?.let { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Replying to: ${r.text}", modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { replyTo = null }, modifier = Modifier.testTag("btn_cancel_reply")) { Text("Cancel") }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmoji = !showEmoji }, modifier = Modifier.testTag("btn_emoji")) {
                            Icon(Icons.Filled.SentimentSatisfied, contentDescription = "Emoji")
                        }
                        IconButton(
                            onClick = {
                                sendMessage("\uD83D\uDCCE document_${(100..999).random()}.pdf", simulateReply = false)
                                scope.launch { snackbarHostState.showSnackbar("File attached") }
                            },
                            modifier = Modifier.testTag("btn_attach")
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Attach file")
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Message...") },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f).testTag("field_message"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                val prefix = replyTo?.let { "↪ ${it.text}\n" } ?: ""
                                sendMessage(prefix + input)
                                input = ""
                                replyTo = null
                            })
                        )
                        if (input.isBlank()) {
                            IconButton(onClick = {
                                sendMessage("\uD83C\uDFA4 Voice message 0:0${(3..9).random()}", simulateReply = false)
                                scope.launch { snackbarHostState.showSnackbar("Voice note recorded") }
                            }, modifier = Modifier.testTag("btn_voice_note")) {
                                Icon(Icons.Filled.Mic, contentDescription = "Record voice note")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val prefix = replyTo?.let { "↪ ${it.text}\n" } ?: ""
                                    sendMessage(prefix + input)
                                    input = ""
                                    replyTo = null
                                },
                                modifier = Modifier.testTag("btn_send_message")
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (showEmoji) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("😀", "😂", "❤️", "👍", "🎉", "😮").forEach { emoji ->
                                Text(
                                    emoji,
                                    modifier = Modifier
                                        .testTag("emoji_$emoji")
                                        .clickable { input += emoji },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (messages != null && messages.isEmpty() && !isTyping) {
                Text(
                    "No messages yet. Say hi \uD83D\uDC4B",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("list_messages"),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(12.dp)
            ) {
                // reverseLayout: the first item is drawn at the bottom (newest side).
                if (isTyping && user != null) {
                    item(key = "typing") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Avatar(user, 28)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "typing…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("typing_indicator")
                            )
                        }
                    }
                }
                items(messages?.reversed() ?: emptyList(), key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!msg.isMine && user != null) {
                            Avatar(user, 28)
                            Spacer(Modifier.width(6.dp))
                        }
                        Column(
                            modifier = Modifier
                                .widthIn(max = 270.dp)
                                .clip(
                                    if (msg.isMine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                                    else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                )
                                .background(
                                    if (msg.isMine) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { optionsFor = msg }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("message_${msg.id}"),
                            horizontalAlignment = Alignment.End
                        ) {
                            val textColor =
                                if (msg.isMine) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            Text(msg.text, color = textColor, modifier = Modifier.align(Alignment.Start))
                            Text(
                                if (msg.timeAgo == "now") "now" else "${msg.timeAgo} ago",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    optionsFor?.let { msg ->
        ModalBottomSheet(onDismissRequest = { optionsFor = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = { Text("Reply") },
                    modifier = Modifier
                        .testTag("btn_reply_message")
                        .clickable { replyTo = msg; optionsFor = null }
                )
                ListItem(
                    headlineContent = { Text("Copy") },
                    modifier = Modifier
                        .testTag("btn_copy_message")
                        .clickable {
                            clipboard.setText(AnnotatedString(msg.text))
                            optionsFor = null
                            scope.launch { snackbarHostState.showSnackbar("Message copied") }
                        }
                )
                if (msg.isMine) {
                    ListItem(
                        headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier
                            .testTag("btn_delete_message")
                            .clickable {
                                convo?.messages?.remove(msg)
                                Backend.data.deleteMessage(msg.id)
                                optionsFor = null
                            }
                    )
                }
            }
        }
    }
}
