package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.ui.components.AttachedOfferCard
import com.example.ui.components.HavenInsets
import com.example.ui.model.Conversation
import com.example.ui.model.Message
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Thread Page for an active conversation (Frame 02 & Frame 03 of Haven Android Specifications):
 * - Status bar and cutout safe top bar with back navigation chevron
 * - 40px participant avatar, name, and handle
 * - Chat history with distinct monastic bubbles and embedded attached offer cards
 * - Pinned bottom composer lifting smoothly above the on-screen keyboard (IME)
 *   while cleanly padding above the system navigation bar when closed.
 * - Strict preservation of message text input, offer attachment, and send mechanics.
 */
@Composable
fun ConversationThreadView(
  conversation: Conversation,
  messageInputText: String,
  pendingAttachedOffer: Offer?,
  onBack: () -> Unit,
  onMessageInputChange: (String) -> Unit,
  onOpenAttachSheet: () -> Unit,
  onRemoveAttachment: () -> Unit,
  onSendMessage: () -> Unit,
  onInspectOffer: (Offer) -> Unit,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  BackHandler {
    onBack()
  }

  // Auto-scroll to latest message
  LaunchedEffect(conversation.messages.size) {
    if (conversation.messages.isNotEmpty()) {
      listState.animateScrollToItem(conversation.messages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("conversation_thread_${conversation.id}")
  ) {
    // Child Screen Top Bar with edge-to-edge status bar & cutout integration
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(HavenColors.Canvas)
    ) {
      // Dynamic height for status bar + camera punch holes / notches
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
              .size(44.dp)
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

          // 40px Avatar Circle
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(HavenColors.SurfaceContainer),
            contentAlignment = Alignment.Center
          ) {
            if (!conversation.avatarUrl.isNullOrBlank()) {
              SubcomposeAsyncImage(
                model = conversation.avatarUrl,
                contentDescription = conversation.participantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                  Text(
                    text = conversation.participantInitial,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  )
                },
                error = {
                  Text(
                    text = conversation.participantInitial,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              )
            } else {
              Text(
                text = conversation.participantInitial,
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
              text = conversation.participantName,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              lineHeight = 20.sp,
              color = HavenColors.TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = conversation.participantHandle,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 13.sp,
              lineHeight = 16.sp,
              color = HavenColors.TextSecondary
            )
          }
        }

        // Context Action
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { /* Context details */ }
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

    // Conversation Messages Scroll Area
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(6.dp))
        // Earlier messages pill
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(HavenColors.Canvas)
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Earlier messages",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextSecondary
            )
          }
        }
      }

      items(conversation.messages, key = { it.id }) { message ->
        MessageBubbleItem(
          message = message,
          onInspectOffer = onInspectOffer
        )
      }

      item {
        Spacer(modifier = Modifier.height(10.dp))
      }
    }

    // Pinned Bottom Composer Bar (lifts with soft keyboard, respects navigation bar when closed)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(HavenColors.Canvas)
        .windowInsetsPadding(HavenInsets.bottomWithIme.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag("conversation_composer_bar")
    ) {
      // Pending Attachment Preview Chip
      if (pendingAttachedOffer != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.SurfaceContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(6.dp))
          ) {
            Image(
              painter = painterResource(id = pendingAttachedOffer.imageResId),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Attached: ${pendingAttachedOffer.title}",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = pendingAttachedOffer.priceDisplay,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              color = HavenColors.TextPrimary
            )
          }

          IconButton(
            onClick = onRemoveAttachment,
            modifier = Modifier
              .size(24.dp)
              .testTag("remove_attachment_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remove attachment",
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Composer Input Row (Preserving Exact Working Mechanics)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // "Attach offer" button
        IconButton(
          onClick = onOpenAttachSheet,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pendingAttachedOffer != null) HavenColors.SurfaceContainer else HavenColors.Canvas)
            .testTag("attach_offer_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.LocalOffer,
            contentDescription = "Attach offer to message",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(20.dp)
          )
        }

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
          if (messageInputText.isEmpty()) {
            Text(
              text = "Write a message...",
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextMuted
            )
          }
          BasicTextField(
            value = messageInputText,
            onValueChange = onMessageInputChange,
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
        val canSend = messageInputText.isNotBlank() || pendingAttachedOffer != null
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
 * Message Bubble Item with Monastic Greyscale Palette and Attached Offer Card Support
 */
@Composable
private fun MessageBubbleItem(
  message: Message,
  onInspectOffer: (Offer) -> Unit
) {
  val isMe = message.isFromMe

  Column(
    modifier = Modifier.fillMaxWidth(),
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
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Embedded attached offer if present
        message.attachedOffer?.let { offer ->
          AttachedOfferCard(
            offer = offer,
            onOfferClick = { onInspectOffer(offer) },
            isFromMe = isMe
          )
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

    Text(
      text = message.timestamp,
      fontFamily = InterFontFamily,
      fontSize = 11.sp,
      color = HavenColors.TextSecondary,
      modifier = Modifier.padding(horizontal = 4.dp)
    )
  }
}
