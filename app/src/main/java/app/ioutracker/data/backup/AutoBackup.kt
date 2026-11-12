package app.ioutracker.data.backup

import java.io.File
import java.io.IOException
import java.time.Instant

/*
 * Auto Backup as functions over `File` (ADR-0005): the agent runs in a process bound to the base
 * `Application`, with no container to reach for.
 */

/** Relative to `filesDir`, and never itself backed up. */
const val AUTO_BACKUP_MARKER_FILE = "auto-backup-marker.txt"

private const val MARKER_LAST_BACKUP_KEY = "lastBackupAtEpochMilli"
private const val MARKER_PREVIOUS_BACKUP_KEY = "previousBackupAtEpochMilli"
private const val MARKER_OVER_QUOTA_KEY = "overQuota"

/**
 * What the last automatic backup attempt left behind. Android reports a quota rejection through
 * `onQuotaExceeded` and no other transport failure at all, so [lastBackupAt] is the last attempt that
 * was not refused, not a confirmed upload. When [overQuota], it is the one before the refusal, or
 * null if none is recorded.
 */
data class AutoBackupMarker(
    val lastBackupAt: Instant?,
    val overQuota: Boolean = false,
)

/** Carries the previous [AutoBackupMarker.lastBackupAt] along, for [writeOverQuotaMarker] to fall back to. */
fun writeAutoBackupMarker(
    filesDir: File,
    at: Instant,
) {
    val previous = readMarkerValues(filesDir)?.get(MARKER_LAST_BACKUP_KEY)
    writeMarker(
        filesDir,
        buildString {
            append("$MARKER_LAST_BACKUP_KEY=${at.toEpochMilli()}\n")
            if (previous != null) append("$MARKER_PREVIOUS_BACKUP_KEY=$previous\n")
        },
    )
}

/**
 * Written over the marker [writeAutoBackupMarker] left moments before, from the same attempt's
 * measuring pass. The refused set never reached the account, so the time shown becomes the one
 * before it.
 */
fun writeOverQuotaMarker(filesDir: File) {
    val values = readMarkerValues(filesDir).orEmpty()
    val lastGood =
        if (values[MARKER_OVER_QUOTA_KEY] ==
            "true"
        ) {
            values[MARKER_LAST_BACKUP_KEY]
        } else {
            values[MARKER_PREVIOUS_BACKUP_KEY]
        }
    writeMarker(
        filesDir,
        buildString {
            append("$MARKER_OVER_QUOTA_KEY=true\n")
            if (lastGood != null) append("$MARKER_LAST_BACKUP_KEY=$lastGood\n")
        },
    )
}

fun clearAutoBackupMarker(filesDir: File) {
    File(filesDir, AUTO_BACKUP_MARKER_FILE).delete()
}

/** Null when absent or unreadable, which renders as "none recorded", never as a working backup. */
fun readAutoBackupMarker(filesDir: File): AutoBackupMarker? {
    val values = readMarkerValues(filesDir) ?: return null
    val overQuota = values[MARKER_OVER_QUOTA_KEY] == "true"
    val lastBackupAt = values[MARKER_LAST_BACKUP_KEY]?.toLongOrNull()?.let(Instant::ofEpochMilli)
    if (lastBackupAt == null && !overQuota) return null
    return AutoBackupMarker(
        lastBackupAt = lastBackupAt,
        overQuota = overQuota,
    )
}

private fun readMarkerValues(filesDir: File): Map<String, String>? {
    val marker = File(filesDir, AUTO_BACKUP_MARKER_FILE)
    if (!marker.isFile) return null
    return try {
        marker
            .readLines()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
            }.toMap()
    } catch (e: IOException) {
        null
    }
}

/** Temp-then-rename, so a kill mid-write leaves the previous marker rather than half a line. */
private fun writeMarker(
    filesDir: File,
    text: String,
) {
    val marker = File(filesDir, AUTO_BACKUP_MARKER_FILE)
    val part = File(filesDir, "$AUTO_BACKUP_MARKER_FILE.part")
    try {
        filesDir.mkdirs()
        part.writeText(text)
        if (!part.renameTo(marker)) {
            part.copyTo(marker, overwrite = true)
            part.delete()
        }
    } catch (e: IOException) {
        part.delete()
    }
}
