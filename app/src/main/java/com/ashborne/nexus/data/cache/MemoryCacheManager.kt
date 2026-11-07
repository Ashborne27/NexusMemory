package com.ashborne.nexus.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object MemoryCacheManager {
    private val memoryCache = ConcurrentHashMap<String, Any>()

    private val _cacheState = MutableStateFlow>(emptyMap())
    val cacheState: StateFlow> = _cacheState

    fun <T : Any> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return memoryCache[key] as? T
    }

    fun put(key: String, value: Any) {
        memoryCache[key] = value
        _cacheState.value = memoryCache.toMap()
    }

    fun invalidate() {
        memoryCache.clear()
        _cacheState.value = emptyMap()
    }
}
