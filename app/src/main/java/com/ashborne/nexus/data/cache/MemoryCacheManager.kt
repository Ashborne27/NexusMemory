package com.ashborne.nexus.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object MemoryCacheManager {
    private val memoryCache = ConcurrentHashMap<String, Any>()

    private val _cacheState = MutableStateFlow>(emptyMap())
    val cacheState = _cacheState.asStateFlow()

    fun <T : Any> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return memoryCache[key] as? T
    }

    fun put(key: String, value: Any) {
        memoryCache[key] = value
        _cacheState.value = HashMap(memoryCache)
    }

    fun invalidate() {
        memoryCache.clear()
        _cacheState.value = emptyMap()
    }
}
