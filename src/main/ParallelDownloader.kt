package main

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

const val MAXTHREADS = 10

class ParallelDownloader(
    private val chunkSize: Int,
) {
    init {
        if (chunkSize <= 0) {
            throw IllegalArgumentException("maxRange must be greater than zero.")
        }
    }

    private val client = HttpClient.newHttpClient()
    private val executor = Executors.newFixedThreadPool(MAXTHREADS)

    fun content(url: String): List<String> {
        val futures = getFutures(url)
        val strings = mutableListOf<String>()
        futures.forEach { future -> strings.add(future.get().toString(Charsets.UTF_8)) }
        client.close()
        return strings
    }

    fun download(url: String) {
        val futures = getFutures(url)
        val file = File(url.substringAfterLast('/'))
        file.delete()
        futures.forEach { future -> file.appendBytes(future.get()) }
        client.close()
    }

    private fun getFutures(url: String): List<Future<ByteArray>> {
        val uri = URI.create(url)
        val headRequest = HttpRequest.newBuilder(uri).HEAD().build()
        val headResponse = client.send(headRequest, HttpResponse.BodyHandlers.ofString())

        if (headResponse.statusCode() != 200) {
            throw RuntimeException("Cannot locate $url")
        }

        val contentLength =
            headResponse
                .headers()
                .firstValue("Content-Length")
                .orElseThrow {
                    RuntimeException("File does not have a Content-Length")
                }.toInt()

        val acceptRanges = headResponse.headers().firstValue("Accept-Ranges").orElse("none")

        if (acceptRanges == "none") {
            throw RuntimeException("Cannot use ranges in the download")
        }

        val futures = mutableListOf<Future<ByteArray>>()
        for (start in 0..<contentLength step chunkSize) {
            futures.add(executor.submit(RangeDownloader(client, uri, start, start + chunkSize - 1)))
        }
        return futures
    }
}

class RangeDownloader(
    private val client: HttpClient,
    private val uri: URI,
    private val start: Int,
    private val end: Int,
) : Callable<ByteArray> {
    override fun call(): ByteArray {
        val getRequest =
            HttpRequest
                .newBuilder(uri)
                .GET()
                .header("Range", "bytes=$start-$end")
                .build()
        val getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofByteArray())
        return getResponse.body()
    }
}
