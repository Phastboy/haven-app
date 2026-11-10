package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.MessageDeliveryStatus
import com.example.data.messaging.db.MessageEntity
import com.example.ui.components.HavenEmptyState
import com.example.ui.components.HavenInsets
import com.example.ui.components.MessageBubbleSkeleton
import com.example.ui.model.ConversationThreadUiState
import com.example.ui.model.ThreadContentState
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Production Native Conversation Thread Screen for Haven.
 * Delivers local-first persisted reads, instant optimistic sends,
 * stable scroll anchoring, and delivery state tracking.
 *
 * The message area renders by [ThreadContentState]: cached content is shown
 * immediately and ordinary refreshes stay quiet; bubble-shaped skeletons appear
 * only while the initial history is genuinely missing, and a confirmed-empty or
 * unloadable thread gets its own honest state instead of a blank list.
 */
@Composable
fun NativeConversationThreadScreen(
  state: ConversationThreadUiState,
  onBack: () -> Unit,
  onInputChanged: (String) -> Unit,
  onSendMessage: () -> Unit,
  onRetryMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
  onLoadOlder: () -> Unit = {},
  onOpenProfile: ((String) -> Unit)? = null,
  onRefresh: () -> Unit = {}
) {
  val listState = rememberLazyListState()
  val isDragged by listState.interactionSource.collectIsDraggedAsState()

  BackHandler {
    onBack()
  }

  var previousLastId by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(state.messages.lastOrNull()?.clientMessageId) {
    val last = state.messages.lastOrNull()
    if (last != null && (previousLastId == null ||
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1 ||
        (last.isFromMe && last.status == MessageDeliveryStatus.SENDING))) {
      listState.scrollToItem(state.messages.size)
    }
    previousLastId = last?.clientMessageId
  }
  LaunchedEffect(listState, isDragged) {
    if (!isDragged) return@LaunchedEffect
    snapshotFlow { listState.firstVisibleItemIndex <= 1 }
      .distinctUntilChanged().collect { nearTop -> if (nearTop) onLoadOlder() }
  }

  val conv = state.conversation
  val displayName = conv?.displayName ?: "Conversation"
  val handle = conv?.handle ?: ""
  val avatarUrl = conv?.avatarUrl
  val initials = displayName.trim().split(Regex("\\s+")).take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("native_conversation_thread_screen")
  ) {
    // Edge-to-edge Top Bar
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(HavenColors.Canvas)
    ) {
      Spacer(modifier = Modifier.windowInsetsTopHeight(HavenInsets.statusBarsAndCutouts))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("thread_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back to messages",
              tint = HavenColors.TextPrimary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // 40dp Avatar Circle
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(HavenColors.SurfaceContainer),
            contentAlignment = Alignment.Center
          ) {
            if (!avatarUrl.isNullOrBlank()) {
              SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                  Text(
                    text = initials,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  )
                },
                error = {
                  Text(
                    text = initials,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              )
            } else {
              Text(
                text = initials,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = HavenColors.TextPrimary
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = displayName,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              lineHeight = 20.sp,
              color = HavenColors.TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (handle.isNotBlank()) {
              Text(
                text = handle,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = HavenColors.TextSecondary
              )
            }
          }
        }

        // The profile action stays available during ordinary refreshes; sync
        // progress is scoped to the message area, never to header actions.
        if (onOpenProfile != null && handle.isNotBlank()) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onOpenProfile(handle.removePrefix("@")) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Profile",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary
            )
          }
        }
      }
    }

    // Offline / queue-failure banner: shown only while content is retained or
    // a send failed to queue. A thread that could not be loaded at all reports
    // through its dedicated unavailable state below.
    val bannerMessage = state.sendError ?: state.syncError
    if (bannerMessage != null && state.contentState != ThreadContentState.UNAVAILABLE) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(HavenColors.SurfaceContainer)
          .padding(vertical = 4.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = bannerMessage,
          fontFamily = InterFontFamily,
          fontSize = 12.sp,
          color = HavenColors.TextSecondary
        )
      }
    }

    // Messages Scroll Area
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("thread_messages_list"),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      when (state.contentState) {
        ThreadContentState.CONTENT -> {
          item(key = "history_status") {
            if (state.isLoadingOlder) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else if (state.hasOlder) {
              Text(
                text = state.olderError ?: "Earlier messages",
                color = HavenColors.TextSecondary,
                modifier = Modifier.clickable(onClick = onLoadOlder).padding(12.dp)
              )
            }
          }

          items(state.messages, key = { it.clientMessageId }) { message ->
            NativeMessageBubble(
              message = message,
              onRetry = { onRetryMessage(message.clientMessageId) }
            )
          }
        }

        ThreadContentState.EMPTY -> item(key = "thread_empty_state") {
          Box(
            modifier = Modifier.fillParentMaxHeight(0.8f),
            contentAlignment = Alignment.Center
          ) {
            HavenEmptyState(
              icon = Icons.Outlined.ChatBubbleOutline,
              title = "No messages yet",
              body = "Send a message below to start this conversation.",
              testTag = "thread_empty_state"
            )
          }
        }

        ThreadContentState.UNAVAILABLE -> item(key = "thread_unavailable_state") {
          Box(
            modifier = Modifier.fillParentMaxHeight(0.8f),
            contentAlignment = Alignment.Center
          ) {
            HavenEmptyState(
              icon = Icons.Outlined.CloudOff,
              title = "Couldn't load messages",
              body = "Check your connection and try again.",
              primaryActionLabel = "Try again",
              onPrimaryAction = onRefresh,
              testTag = "thread_unavailable_state"
            )
          }
        }

        ThreadContentState.RESOLVING_CACHE,
        ThreadContentState.LOADING_HISTORY -> items(
          count = threadSkeletonSpecs.size,
          key = { index -> "message_skeleton_$index" }
        ) { index ->
          val spec = threadSkeletonSpecs[index]
          MessageBubbleSkeleton(
            fromMe = spec.fromMe,
            bubbleWidth = spec.width,
            bubbleHeight = spec.height
          )
        }
      }

      item(key = "thread_bottom_space") {
        Spacer(modifier = Modifier.height(8.dp))
      }

    }

    // Pinned Bottom Composer Bar with IME padding
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(HavenColors.Canvas)
        .windowInsetsPadding(HavenInsets.bottomWithIme.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag("thread_composer_bar")
    ) {
      // Pending Attachment Preview Chip
      if (state.pendingAttachedOfferTitle != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.SurfaceContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Attached: ${state.pendingAttachedOfferTitle}",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (state.pendingAttachedOfferPrice != null) {
              Text(
                text = state.pendingAttachedOfferPrice,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = HavenColors.TextPrimary
              )
            }
          }
        }
      }

      // Input Field and Send Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Text Input Field
        Box(
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          if (state.inputText.isEmpty()) {
            Text(
              text = "Write a message...",
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextMuted
            )
          }
          BasicTextField(
            value = state.inputText,
            onValueChange = onInputChanged,
            singleLine = true,
            textStyle = TextStyle(
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary
            ),
            cursorBrush = SolidColor(HavenColors.Primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("message_input_field")
          )
        }

        // Send Button
        val canSend = state.inputText.isNotBlank() || state.pendingAttachedOfferTitle != null
        IconButton(
          onClick = onSendMessage,
          enabled = canSend,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (canSend) HavenColors.Primary else HavenColors.ButtonDisabled)
            .testTag("send_message_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send message",
            tint = HavenColors.TextOnDark,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

/**
 * Message Bubble Composable displaying delivery status (SENDING, SENT, FAILED)
 * and retry affordances.
 */
@Composable
private fun NativeMessageBubble(
  message: MessageEntity,
  onRetry: () -> Unit
) {
  val isMe = message.isFromMe

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("message_bubble_${message.clientMessageId}"),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 295.dp)
        .clip(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isMe) 16.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 16.dp
          )
        )
        .background(if (isMe) HavenColors.Primary else HavenColors.SurfaceContainer)
        .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!message.attachedOfferTitle.isNullOrBlank()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isMe) HavenColors.SurfaceCard.copy(alpha = 0.15f) else HavenColors.Canvas)
              .padding(8.dp)
          ) {
            Column {
              Text(
                text = "Attached: ${message.attachedOfferTitle}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (isMe) HavenColors.TextOnDark else HavenColors.TextPrimary
              )
              if (!message.attachedOfferPrice.isNullOrBlank()) {
                Text(
                  text = message.attachedOfferPrice,
                  fontFamily = InterFontFamily,
                  fontSize = 11.sp,
                  color = if (isMe) HavenColors.TextOnDark.copy(alpha = 0.8f) else HavenColors.TextSecondary
                )
              }
            }
          }
        }

        if (message.text.isNotBlank()) {
          Text(
            text = message.text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = if (isMe) HavenColors.TextOnDark else HavenColors.TextPrimary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Timestamp & Delivery status row
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 4.dp)
    ) {
      if (message.formattedTime.isNotBlank()) {
        Text(
          text = message.formattedTime,
          fontFamily = InterFontFamily,
          fontSize = 11.sp,
          color = HavenColors.TextSecondary
        )
      }

      if (isMe) {
        when (message.status) {
          MessageDeliveryStatus.SENDING -> {
            Icon(
              imageVector = Icons.Outlined.AccessTime,
              contentDescription = "Sending",
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(12.dp)
            )
          }
          MessageDeliveryStatus.SENT -> {
            Icon(
              imageVector = Icons.Outlined.Check,
              contentDescription = "Sent",
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(12.dp)
            )
          }
          MessageDeliveryStatus.FAILED -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clickable { onRetry() }
                .testTag("retry_message_${message.clientMessageId}")
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Failed to send. Tap to retry",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Tap to retry",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }
    }
  }
}

/** One bubble-shaped placeholder row: alignment and bubble size. */
private data class ThreadSkeletonSpec(
  val fromMe: Boolean,
  val width: Dp,
  val height: Dp
)

/**
 * Conversation-shaped layout for the initial-history skeletons: varied widths
 * and one/two-line bubbles, mostly incoming, like a freshly opened thread.
 */
private val threadSkeletonSpecs = listOf(
  ThreadSkeletonSpec(fromMe = false, width = 190.dp, height = 44.dp),
  ThreadSkeletonSpec(fromMe = true, width = 120.dp, height = 44.dp),
  ThreadSkeletonSpec(fromMe = false, width = 240.dp, height = 78.dp),
  ThreadSkeletonSpec(fromMe = false, width = 160.dp, height = 61.dp),
  ThreadSkeletonSpec(fromMe = true, width = 205.dp, height = 61.dp),
  ThreadSkeletonSpec(fromMe = false, width = 135.dp, height = 44.dp)
)
