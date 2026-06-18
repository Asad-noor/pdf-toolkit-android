package com.offlinepdf.toolkit.core.data.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.exceptions.BadPasswordException
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.AffineTransform
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.offlinepdf.toolkit.core.domain.error.PdfError
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import com.offlinepdf.toolkit.core.domain.model.WatermarkPosition
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

class PdfProcessingException(val error: PdfError, cause: Throwable? = null) :
    Exception(error.toString(), cause)

@Singleton
class ITextPdfProcessor @Inject constructor() {

    // ── Document Info ─────────────────────────────────────────────────────────

    fun getPageCount(input: ByteArray, password: String?): Int {
        val reader = openReader(input, password)
        return PdfDocument(reader).use { it.numberOfPages }
    }

    fun getPageDimensions(input: ByteArray, password: String?, pageNumber: Int): Pair<Float, Float> {
        val reader = openReader(input, password)
        return PdfDocument(reader).use { doc ->
            val page = doc.getPage(pageNumber)
            Pair(page.pageSize.width, page.pageSize.height)
        }
    }

    fun getRotation(input: ByteArray, password: String?, pageNumber: Int): Int {
        val reader = openReader(input, password)
        return PdfDocument(reader).use { doc ->
            doc.getPage(pageNumber).rotation
        }
    }

    fun isPasswordProtected(input: ByteArray): Boolean {
        return try {
            PdfDocument(PdfReader(ByteArrayInputStream(input))).use { false }
        } catch (e: BadPasswordException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMetadata(input: ByteArray, password: String?): Map<String, String?> {
        val reader = openReader(input, password)
        return PdfDocument(reader).use { doc ->
            val info = doc.documentInfo
            mapOf(
                "author" to info.author,
                "title" to info.title,
                "creator" to info.creator,
                "creationDate" to info.getMoreInfo("CreationDate")
            )
        }
    }

    // ── Merge ─────────────────────────────────────────────────────────────────

    fun merge(
        inputs: List<ByteArray>,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val writer = PdfWriter(outputStream)
        val mergedDoc = PdfDocument(writer)
        val merger = PdfMerger(mergedDoc)
        try {
            inputs.forEachIndexed { index, bytes ->
                val reader = openReader(bytes, null)
                PdfDocument(reader).use { sourceDoc ->
                    merger.merge(sourceDoc, 1, sourceDoc.numberOfPages)
                }
                onProgress(index + 1, inputs.size)
            }
        } finally {
            mergedDoc.close()
        }
    }

    // ── Split ─────────────────────────────────────────────────────────────────

    fun split(
        input: ByteArray,
        mode: SplitMode,
        password: String?,
        onChunk: (Int, ByteArray) -> Unit,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        PdfDocument(reader).use { sourceDoc ->
            val totalPages = sourceDoc.numberOfPages
            val chunks: List<IntRange> = when (mode) {
                is SplitMode.ByRange -> mode.ranges
                is SplitMode.EveryN -> {
                    (1..totalPages step mode.n).map { start ->
                        start..minOf(start + mode.n - 1, totalPages)
                    }
                }
                is SplitMode.AtPages -> {
                    val splits = mutableListOf<Int>()
                    splits.add(1)
                    splits.addAll(mode.pageNumbers.filter { it in 2..totalPages })
                    splits.add(totalPages + 1)
                    splits.zipWithNext { a, b -> a until b }
                }
            }

            chunks.forEachIndexed { index, range ->
                val baos = ByteArrayOutputStream()
                val chunkWriter = PdfWriter(baos)
                val chunkDoc = PdfDocument(chunkWriter)
                val merger = PdfMerger(chunkDoc)
                val chunkReader = openReader(input, password)
                PdfDocument(chunkReader).use { src ->
                    val from = range.first.coerceIn(1, totalPages)
                    val to = range.last.coerceIn(1, totalPages)
                    if (from <= to) merger.merge(src, from, to)
                }
                chunkDoc.close()
                onChunk(index, baos.toByteArray())
                onProgress(index + 1, chunks.size)
            }
        }
    }

    // ── Compress ──────────────────────────────────────────────────────────────

    fun compress(
        input: ByteArray,
        level: CompressionLevel,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val jpegQuality = when (level) {
            CompressionLevel.LOW -> 80
            CompressionLevel.MEDIUM -> 60
            CompressionLevel.HIGH -> 35
        }

        val reader = openReader(input, password)
        val writer = PdfWriter(outputStream, WriterProperties().useSmartMode())
        val pdfDoc = PdfDocument(reader, writer)
        try {
            val totalPages = pdfDoc.numberOfPages
            for (pageNum in 1..totalPages) {
                val page = pdfDoc.getPage(pageNum)
                val resources = page.resources
                val xObjects = resources.getResource(PdfName.XObject)
                xObjects?.keySet()?.forEach { key ->
                    val xObj = xObjects.getAsStream(key)
                    if (xObj != null && PdfName.Image == xObj.getAsName(PdfName.Subtype)) {
                        recompressImage(xObj, jpegQuality)
                    }
                }
                if (pageNum == 1) {
                    pdfDoc.trailer.getAsDictionary(PdfName.Info)?.apply {
                        remove(PdfName.Author)
                        remove(PdfName.Creator)
                        remove(PdfName.Producer)
                    }
                    pdfDoc.catalog.remove(PdfName.Metadata)
                }
                onProgress(pageNum, totalPages)
            }
        } finally {
            pdfDoc.close()
        }
    }

    private fun recompressImage(xObj: PdfStream, quality: Int) {
        runCatching {
            val imageBytes = xObj.getBytes(true) ?: return
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            bitmap.recycle()
            xObj.setData(baos.toByteArray(), false)
            xObj.put(PdfName.Filter, PdfName.DCTDecode)
        }
    }

    // ── Rotate ────────────────────────────────────────────────────────────────

    fun rotatePages(
        input: ByteArray,
        pageNumbers: List<Int>?,
        rotation: RotationDegree,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        val writer = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(reader, writer)
        try {
            val totalPages = pdfDoc.numberOfPages
            val targets = pageNumbers ?: (1..totalPages).toList()
            targets.forEachIndexed { idx, pageNum ->
                if (pageNum in 1..totalPages) {
                    val page = pdfDoc.getPage(pageNum)
                    val current = page.rotation
                    page.rotation = (current + rotation.degrees) % 360
                }
                onProgress(idx + 1, targets.size)
            }
        } finally {
            pdfDoc.close()
        }
    }

    // ── Extract Pages ─────────────────────────────────────────────────────────

    fun extractPages(
        input: ByteArray,
        pageNumbers: List<Int>,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        val writer = PdfWriter(outputStream)
        val outputDoc = PdfDocument(writer)
        val merger = PdfMerger(outputDoc)
        try {
            PdfDocument(reader).use { sourceDoc ->
                val total = sourceDoc.numberOfPages
                val sorted = pageNumbers.filter { it in 1..total }.distinct().sorted()
                sorted.forEachIndexed { idx, pageNum ->
                    val tmpReader = openReader(input, password)
                    PdfDocument(tmpReader).use { src ->
                        merger.merge(src, pageNum, pageNum)
                    }
                    onProgress(idx + 1, sorted.size)
                }
            }
        } finally {
            outputDoc.close()
        }
    }

    // ── Watermark ─────────────────────────────────────────────────────────────

    fun addTextWatermark(
        input: ByteArray,
        config: WatermarkConfig.TextWatermark,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        val writer = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(reader, writer)
        try {
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val totalPages = pdfDoc.numberOfPages
            for (i in 1..totalPages) {
                val page = pdfDoc.getPage(i)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page)
                val gs = PdfExtGState().setFillOpacity(config.opacity)
                canvas.saveState()
                canvas.setExtGState(gs)

                val r = ((config.color shr 16) and 0xFFL).toFloat() / 255f
                val g = ((config.color shr 8) and 0xFFL).toFloat() / 255f
                val b = (config.color and 0xFFL).toFloat() / 255f
                canvas.setFillColor(DeviceRgb(r, g, b))

                val (cx, cy) = watermarkCenter(config.position, pageSize)
                canvas.concatMatrix(
                    AffineTransform.getRotateInstance(
                        Math.toRadians(config.rotation.toDouble()),
                        cx.toDouble(),
                        cy.toDouble()
                    )
                )
                canvas.beginText()
                    .setFontAndSize(font, config.fontSizePt)
                    .moveText(cx.toDouble(), cy.toDouble())
                    .showText(config.text)
                    .endText()
                canvas.restoreState()
                canvas.release()
                onProgress(i, totalPages)
            }
        } finally {
            pdfDoc.close()
        }
    }

    fun addImageWatermark(
        input: ByteArray,
        config: WatermarkConfig.ImageWatermark,
        imageBytes: ByteArray,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        val writer = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(reader, writer)
        try {
            val imageData = ImageDataFactory.create(imageBytes)
            val totalPages = pdfDoc.numberOfPages
            for (i in 1..totalPages) {
                val page = pdfDoc.getPage(i)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page)
                val gs = PdfExtGState().setFillOpacity(config.opacity)
                canvas.saveState()
                canvas.setExtGState(gs)

                val imgW = imageData.width * config.scaleFactor
                val imgH = imageData.height * config.scaleFactor
                val (cx, cy) = watermarkCenter(config.position, pageSize)
                val x = cx - imgW / 2f
                val y = cy - imgH / 2f

                canvas.addImageFittedIntoRectangle(
                    imageData,
                    Rectangle(x, y, imgW, imgH),
                    false
                )
                canvas.restoreState()
                canvas.release()
                onProgress(i, totalPages)
            }
        } finally {
            pdfDoc.close()
        }
    }

    private fun watermarkCenter(position: WatermarkPosition, page: Rectangle): Pair<Float, Float> {
        val marginX = page.width * 0.1f
        val marginY = page.height * 0.1f
        return when (position) {
            WatermarkPosition.TOP_LEFT -> Pair(marginX, page.height - marginY)
            WatermarkPosition.TOP_CENTER -> Pair(page.width / 2f, page.height - marginY)
            WatermarkPosition.TOP_RIGHT -> Pair(page.width - marginX, page.height - marginY)
            WatermarkPosition.CENTER -> Pair(page.width / 2f, page.height / 2f)
            WatermarkPosition.BOTTOM_LEFT -> Pair(marginX, marginY)
            WatermarkPosition.BOTTOM_CENTER -> Pair(page.width / 2f, marginY)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(page.width - marginX, marginY)
        }
    }

    // ── Password Protect ──────────────────────────────────────────────────────

    fun encrypt(
        input: ByteArray,
        config: PasswordConfig,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val permissions = buildList {
            if (config.allowPrinting) add(EncryptionConstants.ALLOW_PRINTING)
            if (config.allowCopying) add(EncryptionConstants.ALLOW_COPY)
        }.fold(0) { acc, perm -> acc or perm }

        val writerProps = WriterProperties().setStandardEncryption(
            config.userPassword.toByteArray(),
            config.ownerPassword.toByteArray(),
            permissions,
            EncryptionConstants.ENCRYPTION_AES_128
        )
        val reader = openReader(input, null)
        val writer = PdfWriter(outputStream, writerProps)
        PdfDocument(reader, writer).use { }
        onProgress(1, 1)
    }

    // ── Unlock ────────────────────────────────────────────────────────────────

    fun unlock(
        input: ByteArray,
        currentPassword: String,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, currentPassword)
        val writer = PdfWriter(outputStream)
        PdfDocument(reader, writer).use { }
        onProgress(1, 1)
    }

    // ── Images to PDF ─────────────────────────────────────────────────────────

    fun imagesToPdf(
        imageBytesList: List<ByteArray>,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val writer = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)
        try {
            imageBytesList.forEachIndexed { index, bytes ->
                val imageData = ImageDataFactory.create(bytes)
                val image = Image(imageData)
                val pageWidth = pdfDoc.defaultPageSize.width - document.leftMargin - document.rightMargin
                val pageHeight = pdfDoc.defaultPageSize.height - document.topMargin - document.bottomMargin
                image.scaleToFit(pageWidth, pageHeight)
                if (index > 0) document.add(com.itextpdf.layout.element.AreaBreak())
                document.add(image)
                onProgress(index + 1, imageBytesList.size)
            }
        } finally {
            document.close()
        }
    }

    // ── Extract Text ──────────────────────────────────────────────────────────

    fun extractText(
        input: ByteArray,
        password: String?,
        onProgress: (Int, Int) -> Unit
    ): String {
        val reader = openReader(input, password)
        return PdfDocument(reader).use { pdfDoc ->
            val totalPages = pdfDoc.numberOfPages
            val sb = StringBuilder()
            for (i in 1..totalPages) {
                val strategy = com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy()
                val text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(
                    pdfDoc.getPage(i), strategy
                )
                sb.appendLine(text)
                onProgress(i, totalPages)
            }
            sb.toString()
        }
    }

    // ── Reorder Pages ─────────────────────────────────────────────────────────

    fun reorderPages(
        input: ByteArray,
        newOrder: List<Int>,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val writer = PdfWriter(outputStream)
        val outputDoc = PdfDocument(writer)
        val merger = PdfMerger(outputDoc)
        try {
            newOrder.forEachIndexed { idx, pageNum ->
                val reader = openReader(input, password)
                PdfDocument(reader).use { src ->
                    merger.merge(src, pageNum, pageNum)
                }
                onProgress(idx + 1, newOrder.size)
            }
        } finally {
            outputDoc.close()
        }
    }

    // ── Delete Pages ──────────────────────────────────────────────────────────

    fun deletePages(
        input: ByteArray,
        pageNumbers: List<Int>,
        password: String?,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val reader = openReader(input, password)
        PdfDocument(reader).use { sourceDoc ->
            val totalPages = sourceDoc.numberOfPages
            val keepPages = (1..totalPages).filter { it !in pageNumbers }.distinct().sorted()

            val writer = PdfWriter(outputStream)
            val outputDoc = PdfDocument(writer)
            val merger = PdfMerger(outputDoc)
            try {
                keepPages.forEachIndexed { idx, pageNum ->
                    val r = openReader(input, password)
                    PdfDocument(r).use { src -> merger.merge(src, pageNum, pageNum) }
                    onProgress(idx + 1, keepPages.size)
                }
            } finally {
                outputDoc.close()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun openReader(bytes: ByteArray, password: String?): PdfReader {
        val props = ReaderProperties()
        if (password != null) props.setPassword(password.toByteArray())
        return PdfReader(ByteArrayInputStream(bytes), props)
    }
}

private fun <T> PdfDocument.use(block: (PdfDocument) -> T): T =
    try { block(this) } finally { close() }
