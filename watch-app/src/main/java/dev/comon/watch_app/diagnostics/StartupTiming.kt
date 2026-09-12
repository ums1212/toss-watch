package dev.comon.watch_app.diagnostics

import android.os.SystemClock
import android.util.Log
import dev.comon.watch_app.BuildConfig

/** Debug timing only: never log tokens, QR payloads, credentials or HTTP bodies. */
object StartupTiming {
    fun now(): Long = SystemClock.elapsedRealtime()

    fun mark(stage: String, startedAt: Long = now()) {
        if (BuildConfig.DEBUG) Log.i("WatchStartup", "stage=$stage elapsedMs=${now() - startedAt} uptimeMs=${now()}")
    }

    inline fun <T> measure(stage: String, block: () -> T): T {
        val start = now()
        mark("$stage.start")
        try { return block() } finally { mark("$stage.end", start) }
    }
}
