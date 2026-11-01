package com.example.ui.model

import com.example.R

data class Message(
  val id: String,
  val isFromMe: Boolean,
  val text: String,
  val timestamp: String,
  val attachedOffer: Offer? = null
)

data class Conversation(
  val id: String,
  val participantName: String,
  val participantInitial: String,
  val participantRole: String = "Verified Artisan",
  val participantHandle: String = "@${participantName.lowercase().replace(" ", "-")}",
  val avatarUrl: String? = null,
  val lastMessage: String,
  val lastMessageTimestamp: String,
  val isUnread: Boolean = false,
  val messages: List<Message>,
  val relatedOffer: Offer? = null
)

object SampleConversations {
  private const val ADAEZE_AVATAR =
    "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"

  val defaultConversations = listOf(
    // Row 1: Adaeze Obi (Unread) matching Frame 01 & Frame 02
    Conversation(
      id = "conv-1",
      participantName = "Adaeze Obi",
      participantInitial = "A",
      participantRole = "Textile Weaver · Oyo State",
      participantHandle = "@adaeze-obi",
      avatarUrl = ADAEZE_AVATAR,
      lastMessage = "Yes, the 6 yards are still available. I can ship on Friday.",
      lastMessageTimestamp = "Oct 25, 14:21",
      isUnread = true,
      relatedOffer = SampleOffers.defaultOffers[0], // Aso Oke
      messages = listOf(
        Message(
          id = "msg-1-1",
          isFromMe = true,
          text = "Hello, is this still available? I need it for a wedding in December.",
          timestamp = "13:45",
          attachedOffer = SampleOffers.defaultOffers[0]
        ),
        Message(
          id = "msg-1-2",
          isFromMe = false,
          text = "Yes, the 6 yards are still available. Which colour do you prefer?",
          timestamp = "13:52"
        ),
        Message(
          id = "msg-1-3",
          isFromMe = true,
          text = "Indigo with the gold thread, please.",
          timestamp = "13:58"
        ),
        Message(
          id = "msg-1-4",
          isFromMe = false,
          text = "You may also like this matching gele. I can have both ready in two weeks.",
          timestamp = "14:05",
          attachedOffer = SampleOffers.defaultOffers[0]
        ),
        Message(
          id = "msg-1-5",
          isFromMe = true,
          text = "Could you do 40,000 NGN for the fabric if I also take the gele?",
          timestamp = "14:15"
        ),
        Message(
          id = "msg-1-6",
          isFromMe = false,
          text = "Yes, the 6 yards are still available. I can ship on Friday.",
          timestamp = "14:21"
        )
      )
    ),
    // Row 2: Kofi Mensah (Unread)
    Conversation(
      id = "conv-2",
      participantName = "Kofi Mensah",
      participantInitial = "K",
      participantRole = "Woodcraft Artisan · Kumasi",
      participantHandle = "@kofi-mensah",
      lastMessage = "Could you do 40,000 NGN if I take two?",
      lastMessageTimestamp = "Oct 25, 11:05",
      isUnread = true,
      relatedOffer = SampleOffers.defaultOffers[1],
      messages = listOf(
        Message(
          id = "msg-2-1",
          isFromMe = false,
          text = "Could you do 40,000 NGN if I take two?",
          timestamp = "11:05",
          attachedOffer = SampleOffers.defaultOffers[1]
        )
      )
    ),
    // Row 3: Tolu Adebayo (Unread)
    Conversation(
      id = "conv-3",
      participantName = "Tolu Adebayo",
      participantInitial = "T",
      participantRole = "Leathercrafter · Lagos",
      participantHandle = "@tolu-adebayo",
      lastMessage = "Thanks! I will send the measurements tonight.",
      lastMessageTimestamp = "Oct 24, 18:40",
      isUnread = true,
      messages = listOf(
        Message(
          id = "msg-3-1",
          isFromMe = false,
          text = "Thanks! I will send the measurements tonight.",
          timestamp = "18:40"
        )
      )
    ),
    // Row 4: Ngozi Eze (Read)
    Conversation(
      id = "conv-4",
      participantName = "Ngozi Eze",
      participantInitial = "N",
      participantRole = "Ceramicist · Enugu",
      participantHandle = "@ngozi-eze",
      lastMessage = "The leather tote arrived safely! Absolutely love the finish.",
      lastMessageTimestamp = "Oct 23, 09:15",
      isUnread = false,
      relatedOffer = SampleOffers.defaultOffers[2],
      messages = listOf(
        Message(
          id = "msg-4-1",
          isFromMe = false,
          text = "The leather tote arrived safely! Absolutely love the finish.",
          timestamp = "09:15"
        )
      )
    ),
    // Row 5: Samuel Boateng (Read)
    Conversation(
      id = "conv-5",
      participantName = "Samuel Boateng",
      participantInitial = "S",
      participantRole = "Instrument Maker · Accra",
      participantHandle = "@samuel-boateng",
      lastMessage = "Let me check if the spare guitar strings are still in the case.",
      lastMessageTimestamp = "Oct 21, 16:30",
      isUnread = false,
      relatedOffer = SampleOffers.defaultOffers[4],
      messages = listOf(
        Message(
          id = "msg-5-1",
          isFromMe = false,
          text = "Let me check if the spare guitar strings are still in the case.",
          timestamp = "16:30"
        )
      )
    ),
    // Row 6: Amara Nwosu (Read)
    Conversation(
      id = "conv-6",
      participantName = "Amara Nwosu",
      participantInitial = "A",
      participantRole = "Botanical Dyer · Calabar",
      participantHandle = "@amara-nwosu",
      lastMessage = "We can prepare the party tray for pick up by 2 PM on Sunday.",
      lastMessageTimestamp = "Oct 19, 13:02",
      isUnread = false,
      messages = listOf(
        Message(
          id = "msg-6-1",
          isFromMe = false,
          text = "We can prepare the party tray for pick up by 2 PM on Sunday.",
          timestamp = "13:02"
        )
      )
    ),
    // Row 7: Chidi Okafor (Read)
    Conversation(
      id = "conv-7",
      participantName = "Chidi Okafor",
      participantInitial = "C",
      participantRole = "Printmaker · Ibadan",
      participantHandle = "@chidi-okafor",
      lastMessage = "Great! The box of children's books is on the front porch.",
      lastMessageTimestamp = "Oct 17, 10:44",
      isUnread = false,
      relatedOffer = SampleOffers.defaultOffers[5],
      messages = listOf(
        Message(
          id = "msg-7-1",
          isFromMe = false,
          text = "Great! The box of children's books is on the front porch.",
          timestamp = "10:44"
        )
      )
    ),
    // Row 8: Fatima Bello (Read)
    Conversation(
      id = "conv-8",
      participantName = "Fatima Bello",
      participantInitial = "F",
      participantRole = "Jeweler · Abuja",
      participantHandle = "@fatima-bello",
      lastMessage = "Sent the draft for the invitations. Please review when free.",
      lastMessageTimestamp = "Oct 15, 17:50",
      isUnread = false,
      messages = listOf(
        Message(
          id = "msg-8-1",
          isFromMe = false,
          text = "Sent the draft for the invitations. Please review when free.",
          timestamp = "17:50"
        )
      )
    )
  )
}
