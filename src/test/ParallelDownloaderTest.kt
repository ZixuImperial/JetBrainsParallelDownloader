package test

import main.ParallelDownloader
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ParallelDownloaderTest {
    // my-local-file.txt has the phrase "hello THIS IS MY NEW WEB SERVER"
    @Test
    fun `errorOnFileThatDoesNotExist`() {
        val parallelDownloader = ParallelDownloader(1)
        val url = "http://localhost:8080/file-not-exist.txt"
        try {
            parallelDownloader.content(url)
            fail()
        } catch (e: RuntimeException) {
            assertEquals("Cannot locate $url", e.message)
        }
    }

    @Test
    fun `gettingTheFileOneByteAtATime`() {
        val parallelDownloader = ParallelDownloader(1)
        assertEquals(
            parallelDownloader.content("http://localhost:8080/my-local-file.txt"),
            "hello THIS IS MY NEW WEB SERVER".map { ch ->
                ch.toString()
            },
        )
    }

    @Test
    fun `gettingTheFileTwoByteAtATime`() {
        val parallelDownloader = ParallelDownloader(2)
        assertEquals(
            parallelDownloader.content("http://localhost:8080/my-local-file.txt"),
            listOf("he", "ll", "o ", "TH", "IS", " I", "S ", "MY", " N", "EW", " W", "EB", " S", "ER", "VE", "R"),
        )
    }

    @Test
    fun `gettingTheEntireFile`() {
        val parallelDownloader = ParallelDownloader(1024)
        assertEquals(
            parallelDownloader.content("http://localhost:8080/my-local-file.txt"),
            listOf("hello THIS IS MY NEW WEB SERVER"),
        )
    }

    @Test
    fun `downloadingTheFileOneByteAtATime`() {
        val parallelDownloader = ParallelDownloader(1)
        val url = "http://localhost:8080/my-local-file.txt"
        parallelDownloader.download(url)
        val file = File(url.substringAfterLast('/'))

        assertEquals(
            file.readText(Charsets.UTF_8),
            "hello THIS IS MY NEW WEB SERVER",
        )
    }

    @Test
    fun `downloadingTheEntireFile`() {
        val parallelDownloader = ParallelDownloader(1024)
        val url = "http://localhost:8080/my-local-file.txt"
        parallelDownloader.download(url)
        val file = File(url.substringAfterLast('/'))

        assertEquals(
            file.readText(Charsets.UTF_8),
            "hello THIS IS MY NEW WEB SERVER",
        )
    }
}
