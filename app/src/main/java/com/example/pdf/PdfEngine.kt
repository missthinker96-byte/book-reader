package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object PdfEngine {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates a small sample PDF file for testing upload and reading.
     */
    fun createSamplePdf(context: Context, title: String, author: String): File {
        val cacheDir = File(context.cacheDir, "sample_pdfs").apply { mkdirs() }
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val file = File(cacheDir, "${sanitizedTitle}_sample.pdf")

        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(33, 33, 33)
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = AndroidColor.rgb(100, 100, 100)
            textSize = 18f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = AndroidColor.rgb(50, 50, 50)
            textSize = 14f
            isAntiAlias = true
        }
        val pageNumberPaint = Paint().apply {
            color = AndroidColor.rgb(160, 160, 160)
            textSize = 12f
            isAntiAlias = true
        }

        val pageSampleTexts = listOf(
            listOf(
                "Chapter I: An Introduction",
                "",
                "It is a truth universally acknowledged, that a reader in possession",
                "of a good collection of books must be in want of a peaceful library.",
                "",
                "Books have the unique power to transport our thoughts across oceans,",
                "centuries, and minds. As you open these pages, you enter a world",
                "crafted purely from ideas, stories, and imagination.",
                "",
                "\"A reader lives a thousand lives before he dies. The man who never",
                "reads lives only one.\"",
                "",
                "This small sample PDF serves as an example document uploaded through",
                "the Admin Portal to demonstrate Firebase Storage and Firestore sync."
            ),
            listOf(
                "Chapter II: The Craft of Reading",
                "",
                "Reading is not merely deciphering characters upon a page; it is",
                "communing with thinkers who lived across eras and cultures.",
                "",
                "Whether set in Paper Light, Slate Dark, or Midnight Black, every",
                "word retains its clarity and warmth.",
                "",
                "\"There is no friend as loyal as a book.\"",
                "— Ernest Hemingway",
                "",
                "Notice how cleanly the typography and margins preserve visual",
                "comfort across orientations and reading themes."
            ),
            listOf(
                "Chapter III: Epilogue",
                "",
                "Thank you for exploring this digital library application.",
                "",
                "With Firebase Storage for document binaries, Cloud Firestore for metadata,",
                "and Firebase Authentication for administrator access, your library",
                "is scalable, secure, and ready for readers worldwide.",
                "",
                "Happy reading and exploring!"
            )
        )

        for (i in pageSampleTexts.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i + 1).create() // A4 at 72dpi
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Background
            canvas.drawColor(AndroidColor.rgb(252, 250, 245))

            // Header decoration line
            val decorPaint = Paint().apply {
                color = AndroidColor.rgb(210, 195, 175)
                strokeWidth = 2f
            }
            canvas.drawLine(50f, 60f, 545f, 60f, decorPaint)

            // Title & Author on first page, or chapter on later pages
            if (i == 0) {
                canvas.drawText(title, 50f, 100f, titlePaint)
                canvas.drawText("By $author", 50f, 130f, subtitlePaint)
            } else {
                canvas.drawText(title, 50f, 90f, subtitlePaint)
            }

            var currentY = if (i == 0) 180f else 130f
            for (line in pageSampleTexts[i]) {
                if (line.startsWith("Chapter")) {
                    canvas.drawText(line, 50f, currentY, subtitlePaint)
                    currentY += 26f
                } else {
                    canvas.drawText(line, 50f, currentY, bodyPaint)
                    currentY += 22f
                }
            }

            // Footer
            canvas.drawText("Page ${i + 1} of ${pageSampleTexts.size}", 270f, 800f, pageNumberPaint)

            pdfDocument.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Resolves a PDF file from a URI, web URL, or local path.
     */
    suspend fun resolvePdfFile(context: Context, pdfUrl: String): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "downloaded_pdfs").apply { mkdirs() }

        // If it's a content URI or file URI
        if (pdfUrl.startsWith("content://") || pdfUrl.startsWith("file://")) {
            val uri = Uri.parse(pdfUrl)
            val localFile = File(cacheDir, "imported_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext localFile
        }

        // If it's a local file path
        val asDirectFile = File(pdfUrl)
        if (asDirectFile.exists() && asDirectFile.isFile) {
            return@withContext asDirectFile
        }

        // If it starts with http/https, download or return cached copy
        if (pdfUrl.startsWith("http://") || pdfUrl.startsWith("https://")) {
            val hashName = "pdf_${pdfUrl.hashCode().toString().replace("-", "n")}.pdf"
            val targetFile = File(cacheDir, hashName)
            if (targetFile.exists() && targetFile.length() > 0) {
                return@withContext targetFile
            }

            try {
                val request = Request.Builder().url(pdfUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { stream ->
                            FileOutputStream(targetFile).use { out ->
                                stream.copyTo(out)
                            }
                        }
                        if (targetFile.exists() && targetFile.length() > 0) {
                            return@withContext targetFile
                        }
                    }
                }
            } catch (e: Exception) {
                // If download fails, create a placeholder sample
            }
        }

        // Fallback: Generate a small document describing the book
        return@withContext createSamplePdf(context, "Sample Book", "Reader Archive")
    }

    /**
     * Copy an input stream from URI to a temp file in cache.
     */
    suspend fun copyUriToTempFile(context: Context, uri: Uri, fileName: String): File = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }

    /**
     * Render a page of a PDF file to a Bitmap.
     */
    suspend fun renderPage(file: File, pageIndex: Int, targetWidth: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val finalWidth = targetWidth.coerceIn(600, 2000)
            val finalHeight = (finalWidth * aspectRatio).toInt()

            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Get total page count of a PDF file.
     */
    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            return@withContext renderer.pageCount
        } catch (e: Exception) {
            return@withContext 0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }
}
