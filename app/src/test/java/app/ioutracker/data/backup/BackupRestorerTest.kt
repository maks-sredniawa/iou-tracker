package app.ioutracker.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestorerTest {
    @get:Rule val folder = TemporaryFolder()

    private fun archive(entries: Map<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private val manifest = backupJson.encodeToString(BackupManifest(schemaVersion = 1, createdAt = 1)).toByteArray()

    private fun restorer(databaseFile: File) =
        BackupRestorer(
            databaseFile,
            File(folder.root, "staging"),
            supportedSchema = 1,
            io = Dispatchers.Unconfined,
        )

    @Test
    fun `a restore replaces the database, leaving no part files or sidecars`() =
        runBlocking {
            val databaseFile = File(folder.newFolder("databases"), "app.db").apply { writeText("old") }
            File(databaseFile.path + "-wal").writeText("old log")
            val zip = archive(mapOf(MANIFEST_ENTRY to manifest, DATABASE_ENTRY to "new".toByteArray()))

            val result = restorer(databaseFile).restoreFrom { zip.inputStream() }

            assertEquals(RestoreResult.Restored(1), result)
            assertEquals("new", databaseFile.readText())
            assertFalse(File(databaseFile.path + "-wal").exists())
            val leftovers = databaseFile.parentFile!!.walk().filter { it.name.endsWith(".part") }
            assertTrue(leftovers.toList().toString(), leftovers.none())
        }

    @Test
    fun `a database restore that fails mid-copy leaves the live database and its log alone`() {
        val databaseFile = File(folder.newFolder("databases"), "app.db").apply { writeText("old") }
        File(databaseFile.path + "-wal").writeText("old log")
        File(databaseFile.path + ".part").mkdirs()
        File(databaseFile.path + ".part/blocker").writeText("")
        val zip = archive(mapOf(MANIFEST_ENTRY to manifest, DATABASE_ENTRY to "new".toByteArray()))

        assertThrows(IOException::class.java) {
            runBlocking { restorer(databaseFile).restoreFrom { zip.inputStream() } }
        }

        assertEquals("old", databaseFile.readText())
        assertEquals("old log", File(databaseFile.path + "-wal").readText())
    }

    @Test
    fun `a restore sweeps the part file an interrupted one left behind`() =
        runBlocking {
            val databaseFile = File(folder.newFolder("databases"), "app.db").apply { writeText("old") }
            val staleDatabase = File(databaseFile.path + ".part").apply { writeText("half") }
            val zip = archive(mapOf(MANIFEST_ENTRY to manifest))

            restorer(databaseFile).restoreFrom { zip.inputStream() }

            assertFalse(staleDatabase.exists())
        }

    @Test
    fun `an export carries the manifest and the database, and restores`() =
        runBlocking {
            val exported =
                BackupExporter(
                    databaseFile = File(folder.newFolder("source"), "app.db"),
                    scratchDir = folder.newFolder("scratch"),
                    schemaVersion = 1,
                    io = Dispatchers.Unconfined,
                    checkpoint = { _, staged -> staged.writeText("db") },
                ).exportTo(File(folder.root, "out/backup.zip"))

            val entries =
                ZipInputStream(exported.inputStream()).use { zip ->
                    generateSequence { zip.nextEntry }.map { it.name }.toList()
                }
            assertEquals(listOf(MANIFEST_ENTRY, DATABASE_ENTRY), entries)
            assertFalse(File(exported.path + ".part").exists())

            val databaseFile = File(folder.newFolder("databases"), "app.db")
            assertEquals(RestoreResult.Restored(1), restorer(databaseFile).restoreFrom { exported.inputStream() })
            assertEquals("db", databaseFile.readText())
        }

    @Test
    fun `a newer archive is refused before anything live is touched`() =
        runBlocking {
            val databaseFile = File(folder.newFolder("databases"), "app.db").apply { writeText("old") }
            val newer = backupJson.encodeToString(BackupManifest(schemaVersion = 9, createdAt = 1)).toByteArray()
            val zip = archive(mapOf(MANIFEST_ENTRY to newer, DATABASE_ENTRY to "new".toByteArray()))

            val result =
                BackupRestorer(
                    databaseFile,
                    File(folder.root, "staging"),
                    supportedSchema = 1,
                ).restoreFrom { zip.inputStream() }

            assertEquals(RestoreResult.TooNew(9, 1), result)
            assertEquals("old", databaseFile.readText())
        }
}
