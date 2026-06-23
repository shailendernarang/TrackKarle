package com.example.wealthtracker.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.graphics.Color as GColor
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.wealthtracker.data.local.InvestmentEntity
import com.ss.wealthtracker.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatExportDate(ts: Long): String = try {
    SimpleDateFormat("dd MMM'yy", Locale.ENGLISH).format(Date(ts))
} catch (_: Exception) { "" }

// ── CSV ──────────────────────────────────────────────────────────────────────

fun createCsvExport(context: Context, items: List<InvestmentEntity>): File {
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(cacheDir, "portfolio_export.csv")
    val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()
    val total = items.sumOf { it.amount }
    val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())

    fun q(s: Any): String {
        val str = s.toString()
        return if (str.contains(",") || str.contains("\"") || str.contains("\n"))
            '"' + str.replace("\"", "\"\"") + '"' else str
    }

    // Metadata block (2 rows so Excel shows it cleanly above the table)
    val meta = "TrackKaro Portfolio Export,,,\n" +
               "Generated:,$dateFmt,,\n" +
               "Total Value:,${FormatUtils.formatINR(total)},,\n" +
               "Holdings:,${items.size},,\n\n"

    val (header, rows) = when (singleType) {
        "FD" -> {
            "#,Bank / Issuer,Interest Rate,Maturity Date,Invested Amount\n" to
            items.mapIndexed { i, e ->
                listOf(i + 1, e.bankName ?: "FD",
                    e.fdRate?.let { "%.2f%%".format(it) } ?: "",
                    e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt),
                    FormatUtils.formatINR(e.amount)
                ).joinToString(",") { q(it) }
            }
        }
        "Health Insurance" -> {
            "#,Policy Name,Renewal Date,Premium Amount\n" to
            items.mapIndexed { i, e ->
                listOf(i + 1, e.hiPolicyName ?: "",
                    e.hiRenewalDate ?: formatExportDate(e.createdAt),
                    FormatUtils.formatINR(e.amount)
                ).joinToString(",") { q(it) }
            }
        }
        "Stocks" -> {
            "#,Stock / Scrip,Purchase Date,Invested Amount\n" to
            items.mapIndexed { i, e ->
                listOf(i + 1,
                    (e.stockName ?: e.type).ifBlank { "Stocks" },
                    e.stockDate ?: formatExportDate(e.createdAt),
                    FormatUtils.formatINR(e.amount)
                ).joinToString(",") { q(it) }
            }
        }
        else -> {
            "#,Asset Type,Name / Identifier,Date,Invested Amount\n" to
            items.mapIndexed { i, e ->
                val (name, date) = when (e.investmentType) {
                    "FD"               -> (e.bankName ?: "Fixed Deposit") to (e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "Health Insurance" -> (e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Insurance") to (e.hiRenewalDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "Mutual Fund"      -> e.type.ifBlank { "Mutual Fund" } to (e.mfDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "Gold"             -> (e.goldType?.takeIf { it.isNotBlank() } ?: "Gold") to (e.goldDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "PPF"              -> (e.ppfFy?.takeIf { it.isNotBlank() } ?: "PPF") to (e.ppfDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "NPS"              -> (e.npsTier?.takeIf { it.isNotBlank() } ?: "NPS") to (e.npsDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    "Equity", "Stocks" -> ((e.stockName?.takeIf { it.isNotBlank() } ?: e.type).ifBlank { "Stocks" }) to (e.stockDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
                    else               -> e.type.ifBlank { e.investmentType } to formatExportDate(e.createdAt)
                }
                listOf(i + 1, e.investmentType, name, date, FormatUtils.formatINR(e.amount))
                    .joinToString(",") { q(it) }
            }
        }
    }

    val cols = header.substringBefore('\n').split(',').size
    val totalRow = buildList {
        repeat(cols - 2) { add("") }
        add("TOTAL"); add(FormatUtils.formatINR(total))
    }.joinToString(",") { q(it) }

    // UTF-8 BOM (﻿) ensures Excel auto-detects UTF-8 and renders Indian ₹ correctly
    file.writeText("﻿" + meta + header + rows.joinToString("\n") + "\n" + totalRow)
    return file
}

// ── PDF ──────────────────────────────────────────────────────────────────────

fun createPdfExport(context: Context, items: List<InvestmentEntity>): File {
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(cacheDir, "portfolio_report.pdf")
    val doc = PdfDocument()
    val pageW = 595; val pageH = 842

    val boldTf = ResourcesCompat.getFont(context, R.font.montserrat_bold)  ?: Typeface.DEFAULT_BOLD
    val regTf  = ResourcesCompat.getFont(context, R.font.montserrat_regular) ?: Typeface.DEFAULT

    // ── Palette ──
    val cHeader  = GColor.parseColor("#0F172A")   // dark slate — header band + total row
    val cTableHdr= GColor.parseColor("#1E293B")   // table column header bg
    val cStatBg  = GColor.parseColor("#F1F5F9")   // stat strip bg
    val cAltRow  = GColor.parseColor("#F8FAFC")   // alternating table row
    val cBorder  = GColor.parseColor("#E2E8F0")   // subtle borders
    val cMuted   = GColor.parseColor("#64748B")   // muted text
    val cBody    = GColor.parseColor("#334155")   // body cell text
    val cAmt     = GColor.parseColor("#1D4ED8")   // amount blue
    val cSlate   = GColor.parseColor("#94A3B8")   // header subtitle
    val cAccent  = GColor.parseColor("#3B82F6")   // group label accent
    val cRowLine = GColor.parseColor("#EEF2F7")   // intra-row divider

    // ── Paint helpers ──
    fun bp(color: Int, size: Float) = Paint().apply { this.color = color; textSize = size; typeface = boldTf; isAntiAlias = true }
    fun rp(color: Int, size: Float) = Paint().apply { this.color = color; textSize = size; typeface = regTf; isAntiAlias = true }
    fun fp(color: Int) = Paint().apply { this.color = color }
    fun lp(color: Int, w: Float = 1f) = Paint().apply { this.color = color; strokeWidth = w }

    // ── Column layout ──
    val margin = 40f; val rightEdge = pageW - margin  // 555f
    val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()

    data class Col(val hdr: String, val x: Float, val right: Boolean = false)
    val cols: List<Col> = when (singleType) {
        "FD"               -> listOf(Col("#", margin), Col("Bank / Issuer", 64f), Col("Rate", 285f), Col("Maturity", 370f), Col("Amount", rightEdge, true))
        "Health Insurance" -> listOf(Col("#", margin), Col("Policy", 64f), Col("Renewal Date", 340f), Col("Amount", rightEdge, true))
        "Stocks"           -> listOf(Col("#", margin), Col("Stock / Scrip", 64f), Col("Purchase Date", 340f), Col("Amount", rightEdge, true))
        else               -> listOf(Col("#", margin), Col("Asset Type", 64f), Col("Name", 185f), Col("Date", 360f), Col("Amount", rightEdge, true))
    }

    // ── Row data builder ──
    fun buildRow(idx: Int, e: InvestmentEntity): List<String> {
        val (name, date) = when (e.investmentType) {
            "FD"               -> (e.bankName ?: "FD") to (e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "Health Insurance" -> (e.hiPolicyName ?: "Insurance") to (e.hiRenewalDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "Mutual Fund"      -> e.type.ifBlank { "MF" } to (e.mfDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "Gold"             -> (e.goldType ?: "Gold") to (e.goldDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "PPF"              -> (e.ppfFy ?: "PPF") to (e.ppfDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "NPS"              -> (e.npsTier ?: "NPS") to (e.npsDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            "Equity", "Stocks" -> ((e.stockName ?: e.type).ifBlank { "Stocks" }) to (e.stockDate?.takeIf { it.isNotBlank() } ?: formatExportDate(e.createdAt))
            else               -> e.type.ifBlank { e.investmentType } to formatExportDate(e.createdAt)
        }
        return when (singleType) {
            "FD"               -> listOf("${idx+1}", e.bankName ?: "FD", e.fdRate?.let { "%.1f%%".format(it) } ?: "—", date, FormatUtils.formatINR(e.amount))
            "Health Insurance" -> listOf("${idx+1}", e.hiPolicyName ?: "Insurance", date, FormatUtils.formatINR(e.amount))
            "Stocks"           -> listOf("${idx+1}", (e.stockName ?: e.type).ifBlank { "Stocks" }, date, FormatUtils.formatINR(e.amount))
            else               -> listOf("${idx+1}", e.investmentType, name, date, FormatUtils.formatINR(e.amount))
        }
    }

    // ── Page state ──
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var canvas = page.canvas
    var y = 0f

    fun newPage() {
        doc.finishPage(page); pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        canvas = page.canvas; y = 50f
    }
    fun maybeNewPage(h: Float = 30f) { if (y + h > pageH - 50f) newPage() }
    fun drawRight(txt: String, rx: Float, cy: Float, paint: Paint) =
        canvas.drawText(txt, rx - paint.measureText(txt), cy, paint)

    // ── HEADER BAND ──────────────────────────────────────────────────────────
    val headerH = 90f
    canvas.drawRect(0f, 0f, pageW.toFloat(), headerH, fp(cHeader))

    // App icon
    val icon: Bitmap? = try {
        val d = context.packageManager.getApplicationIcon(context.packageName)
        val bmp = Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp); d.setBounds(0, 0, c.width, c.height); d.draw(c)
        Bitmap.createScaledBitmap(bmp, 54, 54, true)
    } catch (_: Exception) { null }
    if (icon != null) canvas.drawBitmap(icon, 22f, (headerH - 54) / 2f, null)

    val hx = if (icon != null) 90f else margin
    canvas.drawText("Portfolio Report", hx, 42f, bp(GColor.WHITE, 22f))
    canvas.drawText("TrackKaro · Personal Finance Tracker", hx, 62f, rp(cSlate, 10f))
    val dateTxt = "Generated ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())}"
    drawRight(dateTxt, pageW - margin, 62f, rp(cSlate, 9f))
    y = headerH

    // ── STAT STRIP ───────────────────────────────────────────────────────────
    val total = items.sumOf { it.amount }
    val statsH = 66f
    canvas.drawRect(0f, y, pageW.toFloat(), y + statsH, fp(cStatBg))
    canvas.drawLine(0f, y, pageW.toFloat(), y, lp(cBorder))
    val statW = pageW / 3f
    listOf(
        "TOTAL PORTFOLIO VALUE" to FormatUtils.formatINR(total),
        "TOTAL HOLDINGS"        to "${items.size}",
        "ASSET TYPES"           to "${items.map { it.investmentType }.distinct().size}"
    ).forEachIndexed { i, (label, value) ->
        if (i > 0) canvas.drawLine(i * statW, y + 12f, i * statW, y + statsH - 12f, lp(cBorder))
        canvas.drawText(label, i * statW + 16f, y + 24f, rp(cMuted, 8f))
        canvas.drawText(value,  i * statW + 16f, y + 48f, bp(cTableHdr, 14f))
    }
    y += statsH
    canvas.drawLine(0f, y, pageW.toFloat(), y, lp(cBorder))
    y += 18f

    // ── TABLE HEADER ROW ─────────────────────────────────────────────────────
    val rowH = 22f
    canvas.drawRect(margin, y, rightEdge, y + 26f, fp(cTableHdr))
    cols.forEach { col ->
        val hdrPaint = bp(GColor.WHITE, 10f)
        if (col.right) drawRight(col.hdr, col.x - 4f, y + 18f, hdrPaint)
        else canvas.drawText(col.hdr, col.x + 2f, y + 18f, hdrPaint)
    }
    y += 26f

    // ── DATA ROWS ────────────────────────────────────────────────────────────
    fun drawDataRow(rowIdx: Int, cells: List<String>) {
        maybeNewPage(rowH)
        if (rowIdx % 2 == 1) canvas.drawRect(margin, y, rightEdge, y + rowH, fp(cAltRow))
        val cellPaint = rp(cBody, 10f)
        val amtPaint  = bp(cAmt, 10f)
        cells.dropLast(1).forEachIndexed { i, cell ->
            if (i < cols.size - 1) canvas.drawText(cell, cols[i].x + 2f, y + 15f, cellPaint)
        }
        drawRight(cells.last(), rightEdge - 4f, y + 15f, amtPaint)
        canvas.drawLine(margin, y + rowH, rightEdge, y + rowH, lp(cRowLine, 0.5f))
        y += rowH
    }

    if (singleType == null) {
        // Mixed types — group by asset type with a mini group label row
        var rowIdx = 0
        items.groupBy { it.investmentType }.forEach { (type, typeItems) ->
            maybeNewPage(rowH + 14f)
            y += 8f
            val groupPaint = bp(cAccent, 9f)
            canvas.drawText(type.uppercase(Locale.ENGLISH), margin + 2f, y + 10f, groupPaint)
            val labelEnd = margin + 2f + groupPaint.measureText(type.uppercase(Locale.ENGLISH)) + 6f
            canvas.drawLine(labelEnd, y + 6f, rightEdge, y + 6f, lp(GColor.parseColor("#BFDBFE"), 0.5f))
            y += 14f
            typeItems.forEach { e ->
                drawDataRow(rowIdx, buildRow(rowIdx, e))
                rowIdx++
            }
        }
    } else {
        items.forEachIndexed { idx, e -> drawDataRow(idx, buildRow(idx, e)) }
    }

    // ── TOTAL ROW ────────────────────────────────────────────────────────────
    maybeNewPage(32f)
    y += 6f
    canvas.drawRect(margin, y, rightEdge, y + 28f, fp(cTableHdr))
    canvas.drawText("TOTAL", margin + 8f, y + 19f, bp(GColor.WHITE, 11f))
    drawRight(FormatUtils.formatINR(total), rightEdge - 6f, y + 19f, bp(GColor.WHITE, 13f))
    y += 28f

    // ── FOOTER ───────────────────────────────────────────────────────────────
    canvas.drawLine(margin, pageH - 30f, rightEdge, pageH - 30f, lp(cBorder))
    canvas.drawText("Generated by TrackKaro · Personal Finance Tracker", margin, pageH - 15f, rp(cMuted, 8f))
    drawRight("Page $pageNum", rightEdge, pageH - 15f, rp(cMuted, 8f))

    doc.finishPage(page)
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}

// ── Share / Save ─────────────────────────────────────────────────────────────

fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

fun saveToDownloads(context: Context, file: File, mimeType: String, displayName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(file.readBytes()) }
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(it, values, null, null)
            }
            uri != null
        } else {
            @Suppress("DEPRECATION")
            val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), displayName)
            file.copyTo(dest, overwrite = true)
            true
        }
    } catch (_: Exception) { false }
}
