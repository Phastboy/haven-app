package com.example.camera

import androidx.core.content.FileProvider

/** Separate provider identity keeps camera paths independent from APK installation paths. */
class OfferCaptureProvider : FileProvider()
