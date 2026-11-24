package app.olauncher.data

import androidx.core.util.Consumer

/** Reuses recent fixes and coalesces screen refreshes on the main thread. */
internal class HomeLocationCache<T : Any>(
    private val elapsedMillis: () -> Long,
    private val fixElapsedMillis: (T) -> Long,
) {
    private class Pending<T : Any>(var onResult: (T?) -> Unit)

    private var cached: T? = null
    private var pending: Pending<T>? = null

    fun recent(): T? = cached?.takeIf { elapsedMillis() - fixElapsedMillis(it) in 0L until MAX_AGE_MS }

    fun get(
        request: (Consumer<T?>) -> Unit,
        lastKnown: () -> T?,
        onResult: (T?) -> Unit,
    ) {
        recent()?.let {
            onResult(it)
            return
        }
        pending?.let {
            it.onResult = onResult
            return
        }

        val next = Pending<T>(onResult)
        pending = next
        request(Consumer<T?> { current ->
            if (pending !== next) return@Consumer
            pending = null
            val location = current ?: lastKnown()
            cached = location
            next.onResult(location)
        })
    }

    fun clear() {
        cached = null
        pending = null
    }

    private companion object {
        const val MAX_AGE_MS = 2 * 60 * 1000L
    }
}
