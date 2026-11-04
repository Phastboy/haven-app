package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Custom Application class configuring high-performance image memory and disk caching via Coil.
 */
class HavenApplication : Application(), ImageLoaderFactory {

  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(0.25) // Allocate up to 25% of application memory for bitmap caching
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("haven_image_cache"))
          .maxSizeBytes(100L * 1024 * 1024) // 100 MB persistent disk cache
          .build()
      }
      .crossfade(150)
      .respectCacheHeaders(false) // Cache images persistently to allow offline rendering
      .memoryCachePolicy(CachePolicy.ENABLED)
      .diskCachePolicy(CachePolicy.ENABLED)
      .build()
  }
}
