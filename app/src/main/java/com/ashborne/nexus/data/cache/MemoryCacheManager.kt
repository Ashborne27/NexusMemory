package com.ashborne.nexus.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

object MemoryCacheManager {
    private val memoryCache = ConcurrentHashMap<String, Any>()

    val cacheState = MutableStateFlow>(emptyMap())

    fun <T : Any> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return memoryCache[key] as? T
    }

    fun put(key: String, value: Any) {
        memoryCache[key] = value
        cacheState.value = HashMap(memoryCache)
    }

    fun invalidate() {
        memoryCache.clear()
        cacheState.value = emptyMap()
    }
}
