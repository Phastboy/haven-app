package com.example

import coil3.request.crossfade
import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import okio.Path.Companion.toOkioPath
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.data.network.HavenCioClient
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy

/**
 * Custom Application class configuring high-performance image memory and disk caching via Coil.
 */
class HavenApplication : Application(), SingletonImageLoader.Factory {

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
      .components { add(KtorNetworkFetcherFactory(httpClient = HavenCioClient.imageClient)) }
      .memoryCache {
        MemoryCache.Builder()
          .maxSizePercent(context, 0.25) // Allocate up to 25% of application memory for bitmap caching
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("haven_image_cache").toOkioPath())
          .maxSizeBytes(100L * 1024 * 1024) // 100 MB persistent disk cache
          .build()
      }
      .crossfade(150)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .diskCachePolicy(CachePolicy.ENABLED)
      .build()
  }
}
