package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.model.Conversation
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Messages Inbox View (Frame 01 of Haven Android Specifications):
 * - Clean screen header with "Messages" (20px, semi-bold) and unread count badge ("3 unread")
 * - 72px rows with 48px avatar, unread dot indicator, name, timestamp, and message preview
 * - Bottom padding ensuring the 8th conversation item scrolls fully above the 80dp bottom navigation bar
 */
@Composable
fun ConversationListView(
  conversations: List<Conversation>,
  onSelectConversation: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val unreadCount = conversations.count { it.isUnread }

  if (conversations.isEmpty()) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(HavenColors.Canvas)
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.ChatBubbleOutline,
          contentDescription = null,
          tint = HavenColors.Primary,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No messages yet",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = HavenColors.TextPrimary
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "When you propose a trade or receive barter inquiries, your conversations will appear here.",
        fontFamily = InterFontFamily,
        fontSize = 13.sp,
        color = HavenColors.TextSecondary
      )
    }
  } else {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(HavenColors.Canvas)
    ) {
      // Header: Messages (20px font-semibold) + unread badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(HavenColors.Canvas)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Messages",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          letterSpacing = (-0.3).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("messages_header_title")
        )

        if (unreadCount > 0) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "$unreadCount unread",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              color = HavenColors.TextSecondary
            )
          }
        }
      }

      // 72px rows LazyColumn
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("conversation_list"),
        contentPadding = PaddingValues(bottom = 88.dp) // Generous scroll space above 80dp bottom navigation bar
      ) {
        items(conversations, key = { it.id }) { conversation ->
          ConversationRowItem(
            conversation = conversation,
            onClick = { onSelectConversation(conversation.id) }
          )
          HorizontalDivider(
            color = HavenColors.BorderSubtle,
            thickness = 1.dp
          )
        }
      }
    }
  }
}

/**
 * 72px list row matching Frame 01 specification
 */
@Composable
private fun ConversationRowItem(
  conversation: Conversation,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(72.dp)
      .clickable { onClick() }
      .padding(horizontal = 16.dp)
      .testTag("conversation_item_${conversation.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 8px Unread indicator dot at left
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(if (conversation.isUnread) HavenColors.TextPrimary else Color.Transparent)
    )

    Spacer(modifier = Modifier.width(10.dp))

    // 48px Avatar Circle (Photo or Letter)
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(HavenColors.SurfaceContainer)
        .border(1.dp, HavenColors.BorderSubtle, CircleShape),
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
              fontWeight = FontWeight.SemiBold,
              fontSize = 17.sp,
              color = HavenColors.TextPrimary
            )
          },
          error = {
            Text(
              text = conversation.participantInitial,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 17.sp,
              color = HavenColors.TextPrimary
            )
          }
        )
      } else {
        Text(
          text = conversation.participantInitial,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 17.sp,
          color = HavenColors.TextPrimary
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Center Text Info
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = conversation.participantName,
          fontFamily = InterFontFamily,
          fontWeight = if (conversation.isUnread) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = conversation.lastMessageTimestamp,
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          color = HavenColors.TextSecondary,
          fontWeight = if (conversation.isUnread) FontWeight.Medium else FontWeight.Normal
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = conversation.lastMessage,
        fontFamily = InterFontFamily,
        fontWeight = if (conversation.isUnread) FontWeight.Medium else FontWeight.Normal,
        fontSize = 14.sp,
        color = if (conversation.isUnread) HavenColors.TextPrimary else HavenColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
