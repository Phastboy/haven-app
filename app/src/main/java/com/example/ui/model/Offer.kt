package com.example.ui.model

import com.example.R

data class Offer(
  val id: String,
  val title: String,
  val fullTitle: String,
  val creatorName: String,
  val creatorInitial: String,
  val priceDisplay: String,
  val currency: String,
  val priceNumeric: Double,
  val imageResId: Int,
  val imageUrl: String,
  val isFavorite: Boolean = false,
  val category: String = "Crafts",
  val description: String = "",
  /** Additional bundled images for the detail gallery. The primary image is [imageResId]. */
  val galleryResIds: List<Int> = emptyList(),
  /** Ordered label/value rows shown under "Offer location". */
  val location: List<Pair<String, String>> = emptyList(),
  val galleryUrls: List<String> = emptyList()
)

object SampleOffers {
  val defaultOffers = listOf(
    Offer(
      id = "offer-1",
      title = "Hand-woven Aso Oke fabric, 6 yards",
      fullTitle = "Hand-woven ceremonial Aso Oke fabric, 6 yards",
      creatorName = "Adaeze Obi",
      creatorInitial = "A",
      priceDisplay = "45,000 NGN",
      currency = "NGN",
      priceNumeric = 45000.0,
      imageResId = R.drawable.fabric_craft,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1XPeKhs-jdQJzrx4DgSpBY5d2U7viS72LSoBderGt3o3yQ4kNfJlUSu1sF5HpcX3VkfKpj2YzZmIA9mPyU6xPv8K2Lc3NaZGOiNxwpKUw-AMr8LiAL7h98SG0f_UTJ9BgHthDunh92Q_tGYi1v6tJb4mtZz0L8tK23Fb7opxbKsfUSw_iBkPZNZHv6ecFwG9wW5PLfWzYU_Cd0JMw-g9mCIGBQkS0aM7wSs2s-J9u219OEL1ukWYWw2mvij",
      isFavorite = true,
      category = "Textiles",
      description = "Authentic ceremonial hand-loomed Aso Oke textile woven in Oyo State. Double-weft cotton with metallic accent threads. Ideal for traditional attire, upholstery, or artistic draping.",
      location = listOf(
        "Neighbourhood" to "Yaba Art District",
        "City" to "Lagos",
        "Country" to "Nigeria"
      )
    ),
    Offer(
      id = "offer-2",
      title = "Sculptural Table Lamp",
      fullTitle = "Sculptural Ceramic & Brass Table Lamp",
      creatorName = "Tunde Bakare",
      creatorInitial = "T",
      priceDisplay = "120,000 NGN",
      currency = "NGN",
      priceNumeric = 120000.0,
      imageResId = R.drawable.lamp_craft,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1UEVbbaU_DTh5Yxl-gsrirVw_g4SP5G75PLgLCCtSwb9iFxoc7qLTngW3g4IBr1TWy_tifez__x7vpI05ppEkiQGN45YN183UP1PYQ6MWxmthCVAMMqEWlUmM_zrdVrM8aCm9rKx5ekapZLQkaE9fMa8xHBpffoV6jyMNIh15WA_7KLzf1wzsFocyXtKqJzpzOIcAIyHXAvOaqV69hwvbvvhOCopuSD8r8yXkxRWC43n33sG9I7jlePmXCz",
      isFavorite = false,
      category = "Home & Decor",
      description = "Handmade turned reclaimed iroko wood and brushed brass pedestal lamp with sand-glazed earthenware base. Warm ambient glow with dimmable braided cord switch.",
      location = listOf(
        "Neighbourhood" to "Surulere",
        "City" to "Lagos",
        "Country" to "Nigeria"
      )
    ),
    Offer(
      id = "offer-3",
      title = "Weekly vegetable box",
      fullTitle = "Weekly organic harvest box from my farm",
      creatorName = "Musa Danjuma",
      creatorInitial = "M",
      priceDisplay = "12,000 NGN",
      currency = "NGN",
      priceNumeric = 120000.0 / 10.0,
      imageResId = R.drawable.veg_box,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1WsTEroDCJvKwpyC1Q2rOipT83ueZAPxkcTjLNubMajFh3hNXWbMPcf3bMyL4RRJ72lxtLsP0Zfp7sCwbvoqskWFHuhOokHNpOspfIM12_Ue9qkiSYAuEV_Z1Iy2wEdjHkWBL6rRNOArh50ac35cDMIAg7RnY_aRu1AXwXYy_VgFfxJemsP_3EPNYWhjcJ09Jug16Oioa9UYgjKfejgu0tItnsJSCn7TjV4MeUogetNGe8AmN-8amOugXw",
      isFavorite = false,
      category = "Produce",
      description = "Freshly harvested heirloom greens, ugwu, tatase, habaneros, sweet plantains, and garden roots from our regenerative farm in Epe. Delivered weekly.",
      location = listOf(
        "Neighbourhood" to "Epe",
        "City" to "Lagos",
        "Country" to "Nigeria"
      )
    ),
    Offer(
      id = "offer-4",
      title = "Handmade ceramic mugs, 4x",
      fullTitle = "Handmade ceramic mugs, set of 4",
      creatorName = "Ife Adeyemi",
      creatorInitial = "I",
      priceDisplay = "18 USD",
      currency = "USD",
      priceNumeric = 18.0,
      imageResId = R.drawable.pottery_craft,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1Xmqt3LsLGxkpd5OHPN9TBWizqZclRQw1J56vroPbhKpNpxyW1DrDwL6r2j3dVVsBkI-qRPjZyguHFJ8jMp_U5xzFmHPQzcWQqOcJxwnqtmdcOFIVUzxZCKILxhLEoHuXVLGMm_k6mI9AMl9l-73aliMfErKlCAxZ3ivZaRy9nijOFX9CBbr9a5dYJ6Ehw-Sw73FvyE4OacWGIceV1TySiTLFVESC63OIqNL-eWT3pM3HlHFASQyojDQU-a",
      isFavorite = false,
      category = "Ceramics",
      description = "Set of four wheel-thrown stoneware mugs with speckled oatmeal glaze and exposed terracotta clay bases. Dishwasher and microwave safe.",
      location = listOf(
        "Neighbourhood" to "Independence Layout",
        "City" to "Enugu",
        "Country" to "Nigeria"
      )
    ),
    Offer(
      id = "offer-5",
      title = "Guitar lessons, online",
      fullTitle = "Beginner guitar lessons, online 1-on-1",
      creatorName = "Kofi Mensah",
      creatorInitial = "K",
      priceDisplay = "25 USD",
      currency = "USD",
      priceNumeric = 25.0,
      imageResId = R.drawable.guitar_lessons,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1Vg6TJnGDR-0m5duNtGkav-uG80oOGL3NNKqVcCzfQ7fJyJYM_x7IuQiexzoX5GP6cz1lE2H336rCBk5co7eSoxkp7flKImrWE-qogaZANhKalLQo_ZP1pA3Y9Fucy0hMf2-IyrfK-PJNK2d2qzKMv0o7q3Wb2Ot_aPNbo0BrCjEdoK-yGUOi9uBZPQzjV1-tHV9ILXvK3zp9RbDyanSXOJL4HZkUMkZqJmovm_GuLR435U7dL2ZtNEtuzW",
      isFavorite = false,
      category = "Services & Skills",
      description = "60-minute interactive live acoustic guitar masterclass. Learn foundational fingerpicking, chord transitions, rhythm strumming, and your favorite songs.",
      location = listOf(
        "Neighbourhood" to "East Legon",
        "City" to "Accra",
        "Country" to "Ghana"
      )
    ),
    Offer(
      id = "offer-6",
      title = "Used textbooks, year 1",
      fullTitle = "Used textbooks, engineering year 1",
      creatorName = "Chidi Eze",
      creatorInitial = "C",
      priceDisplay = "Free",
      currency = "USD",
      priceNumeric = 0.0,
      imageResId = R.drawable.textbooks,
      imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1WQbeECMZzTV3BxWp6k4qHqo4JA3PpgCC2qfN6QUcZrPoj820GEpZi6n_2hyaKIFtdaCYzq5g3ld-wmHAdo5RLt--MUBOTntkvZpFFFIpzENaXEFFWTIsHU8xopF7FRR8Uz9ZloTVk3-RLvLYV-0ElxLzgLyYXOEl3ESQ5pUuF-KQBzS1ySygO130pLYhK_PloEcnoVFExSAIxEqvKRxiI5J1QUTPL0vqHzTyHzk-VOma-iMVHM9c4tM8Rb",
      isFavorite = false,
      category = "Books",
      description = "Complete first-year engineering textbooks including Advanced Calculus, Physics Principles, and Materials Science. Clean condition with study annotations.",
      location = listOf(
        "Neighbourhood" to "Bodija",
        "City" to "Ibadan",
        "Country" to "Nigeria"
      )
    )
  )
}
