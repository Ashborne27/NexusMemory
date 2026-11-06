package com.ashborne.nexus.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestionnaire de Cache Réactif optimisé pour NexusMemory v12.0.
 * Assure une mise en cache thread-safe basée sur StateFlow.
 */
object MemoryCacheManager {
    private val memoryCache = ConcurrentHashMap<String, Any>()
    
    private val _cacheState = MutableStateFlow>(emptyMap())
    val cacheState: StateFlow> = _cacheState.asStateFlow()

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
