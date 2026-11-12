package app.ioutracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class AutoBackupTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `the marker round-trips, and an over-quota attempt keeps the last one that went through`() {
        val filesDir = folder.newFolder("files")
        assertNull(readAutoBackupMarker(filesDir))

        val accepted = Instant.ofEpochMilli(1_700_000_000_000)
        writeAutoBackupMarker(filesDir, accepted)
        assertEquals(AutoBackupMarker(accepted, overQuota = false), readAutoBackupMarker(filesDir))

        writeAutoBackupMarker(filesDir, Instant.ofEpochMilli(1_800_000_000_000))
        writeOverQuotaMarker(filesDir)
        assertEquals(AutoBackupMarker(accepted, overQuota = true), readAutoBackupMarker(filesDir))

        writeAutoBackupMarker(filesDir, Instant.ofEpochMilli(1_900_000_000_000))
        writeOverQuotaMarker(filesDir)
        assertEquals(AutoBackupMarker(accepted, overQuota = true), readAutoBackupMarker(filesDir))

        clearAutoBackupMarker(filesDir)
        assertNull(readAutoBackupMarker(filesDir))
        assertFalse(File(filesDir, "$AUTO_BACKUP_MARKER_FILE.part").exists())
    }

    @Test
    fun `an over-quota attempt with nothing earlier records no time`() {
        val filesDir = folder.newFolder("files")
        writeAutoBackupMarker(filesDir, Instant.ofEpochMilli(1_700_000_000_000))
        writeOverQuotaMarker(filesDir)
        assertEquals(AutoBackupMarker(null, overQuota = true), readAutoBackupMarker(filesDir))
    }

    @Test
    fun `an unreadable marker reads as none recorded, never as a backup`() {
        val filesDir = folder.newFolder("files")
        File(filesDir, AUTO_BACKUP_MARKER_FILE).writeText("garbage\n")
        assertNull(readAutoBackupMarker(filesDir))
    }
}
