package com.gasco.weeklypermitreport.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.gasco.weeklypermitreport.R
import com.gasco.weeklypermitreport.data.local.entity.PermitActivityEntity
import com.gasco.weeklypermitreport.data.local.entity.PermitCategory
import com.gasco.weeklypermitreport.data.local.entity.PermitReportEntity
import java.io.File
import java.io.FileOutputStream

data class NumberedSection(
    val heading: String,
    val category: PermitCategory,
    val items: List<Pair<String, PermitActivityEntity>>
)

object GascoExportEngine {

    private fun escapeLatex(raw: String): String = raw
        .replace("\\", "\\textbackslash{}")
        .replace("&", "\\&")
        .replace("%", "\\%")
        .replace("$", "\\$")
        .replace("#", "\\#")
        .replace("_", "\\_")

    fun buildSections(activities: List<PermitActivityEntity>): List<NumberedSection> {
        val sorted = activities.sortedWith(compareBy({ it.dateIso }, { it.createdAt }))
        val activeCats = PermitCategory.entries.filter { cat ->
            val count = sorted.count { it.category == cat }
            cat == PermitCategory.PM || cat == PermitCategory.CM || count > 0
        }
        var rowCounter = 1
        return activeCats.mapIndexed { idx, cat ->
            val rows = sorted.filter { it.category == cat }.map { item ->
                val no = rowCounter++.toString().padStart(2, '0')
                no to item
            }
            NumberedSection(
                heading = "${idx + 1}. ${cat.sectionTitle}",
                category = cat,
                items = rows
            )
        }
    }

    fun generateLatex(report: PermitReportEntity, activities: List<PermitActivityEntity>): String {
        val sections = buildSections(activities)
        val period = escapeLatex(report.reportPeriod)
        val site = escapeLatex(report.siteName)
        val dept = escapeLatex(report.department)

        return buildString {
            appendLine("\\documentclass[a4paper,10pt]{article}")
            appendLine("\\usepackage[top=1.8cm,bottom=2.2cm,left=1.8cm,right=1.8cm]{geometry}")
            appendLine("\\usepackage{graphicx,longtable,array,fancyhdr,helvet}")
            appendLine("\\usepackage[table]{xcolor}")
            appendLine("\\renewcommand{\\familydefault}{\\sfdefault}")
            appendLine("\\definecolor{gascoNavy}{HTML}{1B365D}")
            appendLine("\\definecolor{gascoHeader}{HTML}{1E436E}")
            appendLine("\\definecolor{gascoBlue}{HTML}{2B76B9}")
            appendLine("\\definecolor{gascoSubhead}{HTML}{DDE8F3}")
            appendLine("\\pagestyle{fancy}\\fancyhf{}\\renewcommand{\\headrulewidth}{0pt}\\renewcommand{\\footrulewidth}{0.5pt}")
            appendLine("\\lfoot{{\\fontsize{7.5}{9}\\selectfont\\bfseries\\color{gascoNavy} INSTRUMENTATION \\& CONTROL DEPARTMENT WEEK ACTIVITIES REPORT}\\\\{\\fontsize{7.5}{9}\\selectfont $period}}")
            appendLine("\\rfoot{{\\fontsize{8.5}{10}\\selectfont\\bfseries\\color{gascoNavy} \\thepage}}")
            appendLine("\\begin{document}")
            appendLine("\\vspace*{0.8cm}\\begin{center}\\includegraphics[width=0.96\\textwidth]{cover.png}\\end{center}")
            appendLine("\\vspace{2.4cm}")
            appendLine("\\noindent{\\fontsize{13}{15}\\selectfont\\bfseries\\color{gascoBlue} GASCO}\\\\[0.2cm]")
            appendLine("{\\fontsize{22}{27}\\selectfont\\bfseries\\color{gascoNavy} Instrumentation \\& Control Department -\\\\Week Activities Report}")
            appendLine("\\vspace{1.8cm}\\noindent\\rule{\\linewidth}{0.7pt}\\\\[0.25cm]")
            appendLine("\\noindent\\begin{tabular}{@{}p{0.34\\linewidth}p{0.38\\linewidth}p{0.28\\linewidth}@{}}")
            appendLine("{\\bfseries\\color{gascoBlue} SITE} & {\\bfseries\\color{gascoBlue} DEPARTMENT} & {\\bfseries\\color{gascoBlue} PERIOD} \\\\[0.15cm]")
            appendLine("{\\bfseries\\color{gascoNavy} $site} & {\\bfseries\\color{gascoNavy} $dept} & {\\bfseries\\color{gascoNavy} $period} \\\\")
            appendLine("\\end{tabular}\\\\[0.25cm]\\noindent\\rule{\\linewidth}{0.7pt}\\newpage")
            appendLine("\\noindent{\\fontsize{16}{20}\\selectfont\\bfseries\\color{gascoNavy} Summary of Site Activities}\\vspace{0.45cm}")
            appendLine("\\renewcommand{\\arraystretch}{1.35}")
            appendLine("\\begin{longtable}{|>{\\centering\\arraybackslash}p{0.85cm}|>{\\raggedright\\arraybackslash}p{4.4cm}|>{\\raggedright\\arraybackslash}p{8.6cm}|>{\\centering\\arraybackslash}p{2.2cm}|}")
            appendLine("\\hline\\rowcolor{gascoHeader}\\textbf{\\color{white} No.} & \\textbf{\\color{white} Equipment} & \\textbf{\\color{white} Activity} & \\textbf{\\color{white} Date} \\\\ \\hline\\endhead")
            sections.forEach { sec ->
                appendLine("\\rowcolor{gascoSubhead}\\multicolumn{4}{|l|}{\\textbf{\\color{gascoNavy} ${escapeLatex(sec.heading)}}} \\\\ \\hline")
                if (sec.items.isEmpty()) {
                    appendLine("\\multicolumn{4}{|c|}{\\textit{No ${sec.category.name} activities recorded.}} \\\\ \\hline")
                } else {
                    sec.items.forEach { (no, item) ->
                        appendLine("\\textbf{$no} & \\textbf{\\color{gascoNavy} ${escapeLatex(item.equipment)}} & ${escapeLatex(item.activity)} & ${escapeLatex(item.displayDate)} \\\\ \\hline")
                    }
                }
            }
            appendLine("\\end{longtable}\\end{document}")
        }
    }

    fun writeLatexFile(context: Context, report: PermitReportEntity, activities: List<PermitActivityEntity>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Instrumentation_Weekly_Report.tex")
        file.writeText(generateLatex(report, activities))
        return file
    }

    fun writeCoverPng(context: Context): File {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cover)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "cover.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    fun writeTwoPagePdf(context: Context, report: PermitReportEntity, activities: List<PermitActivityEntity>): File {
        val pdf = PdfDocument()
        val navy = Color.parseColor("#1B365D")
        val headerBg = Color.parseColor("#1E436E")
        val blue = Color.parseColor("#2B76B9")
        val subheadBg = Color.parseColor("#DDE8F3")

        val navyBold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val blueBold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A"); textSize = 9f }
        val linePaint = Paint().apply { color = Color.parseColor("#1E293B"); strokeWidth = 0.8f }
        val rulePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 0.7f }

        // PAGE 1: Exact GASCO Cover Page using bundled R.drawable.cover
        val page1 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c1 = page1.canvas
        c1.drawColor(Color.WHITE)

        val coverBmp = BitmapFactory.decodeResource(context.resources, R.drawable.cover)
        if (coverBmp != null) {
            val targetW = 495f
            val targetH = targetW * (coverBmp.height.toFloat() / coverBmp.width.toFloat())
            c1.drawBitmap(coverBmp, null, RectF(50f, 95f, 50f + targetW, 95f + targetH), null)
        }

        blueBold.textSize = 13f
        c1.drawText("GASCO", 50f, 495f, blueBold)

        navyBold.textSize = 23f
        c1.drawText("Instrumentation & Control Department -", 50f, 526f, navyBold)
        c1.drawText("Week Activities Report", 50f, 554f, navyBold)

        c1.drawLine(50f, 610f, 545f, 610f, linePaint)
        blueBold.textSize = 8.5f
        c1.drawText("SITE", 50f, 628f, blueBold)
        c1.drawText("DEPARTMENT", 220f, 628f, blueBold)
        c1.drawText("PERIOD", 405f, 628f, blueBold)

        navyBold.textSize = 10.5f
        c1.drawText(report.siteName, 50f, 648f, navyBold)
        c1.drawText(report.department, 220f, 648f, navyBold)
        c1.drawText(report.reportPeriod, 405f, 648f, navyBold)
        c1.drawLine(50f, 662f, 545f, 662f, linePaint)

        drawFooter(c1, report.reportPeriod, "1", navyBold, bodyPaint, rulePaint)
        pdf.finishPage(page1)

        // PAGE 2: Exact 4-column Summary of Site Activities Table
        val page2 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
        val c2 = page2.canvas
        c2.drawColor(Color.WHITE)

        navyBold.textSize = 16f
        c2.drawText("Summary of Site Activities", 46f, 64f, navyBold)

        val left = 46f
        val right = 549f
        val colNo = 78f
        val colEq = 212f
        val colAct = 468f
        var y = 84f

        val fillHeader = Paint().apply { color = headerBg }
        val fillSub = Paint().apply { color = subheadBg }
        val whiteBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val strokeBorder = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }

        // Table Header Row
        val headerH = 22f
        c2.drawRect(left, y, right, y + headerH, fillHeader)
        c2.drawRect(left, y, right, y + headerH, strokeBorder)
        c2.drawText("No.", left + 9f, y + 14.5f, whiteBold)
        c2.drawText("Equipment", colNo + 6f, y + 14.5f, whiteBold)
        c2.drawText("Activity", colEq + 6f, y + 14.5f, whiteBold)
        c2.drawText("Date", colAct + 24f, y + 14.5f, whiteBold)
        y += headerH

        buildSections(activities).forEach { sec ->
            if (y > 760f) return@forEach
            val subH = 19f
            c2.drawRect(left, y, right, y + subH, fillSub)
            c2.drawRect(left, y, right, y + subH, strokeBorder)
            navyBold.textSize = 9f
            c2.drawText(sec.heading, left + 6f, y + 13f, navyBold)
            y += subH

            sec.items.forEach { (no, item) ->
                if (y > 770f) return@forEach
                val rowH = 21f
                c2.drawRect(left, y, right, y + rowH, strokeBorder)
                c2.drawLine(colNo, y, colNo, y + rowH, strokeBorder)
                c2.drawLine(colEq, y, colEq, y + rowH, strokeBorder)
                c2.drawLine(colAct, y, colAct, y + rowH, strokeBorder)

                navyBold.textSize = 8.5f
                bodyPaint.textSize = 8.5f
                c2.drawText(no, left + 10f, y + 13.5f, navyBold)
                c2.drawText(item.equipment.take(24), colNo + 5f, y + 13.5f, navyBold)
                c2.drawText(item.activity.take(52), colEq + 5f, y + 13.5f, bodyPaint)
                c2.drawText(item.displayDate, colAct + 12f, y + 13.5f, bodyPaint)
                y += rowH
            }
        }

        drawFooter(c2, report.reportPeriod, "2", navyBold, bodyPaint, rulePaint)
        pdf.finishPage(page2)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Instrumentation_Weekly_Report.pdf")
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }

    private fun drawFooter(
        canvas: Canvas,
        period: String,
        pageNum: String,
        navyBold: Paint,
        bodyPaint: Paint,
        rulePaint: Paint
    ) {
        canvas.drawLine(46f, 792f, 549f, 792f, rulePaint)
        navyBold.textSize = 7.5f
        canvas.drawText("INSTRUMENTATION & CONTROL DEPARTMENT WEEK ACTIVITIES REPORT", 46f, 806f, navyBold)
        bodyPaint.textSize = 7.5f
        canvas.drawText(period, 46f, 818f, bodyPaint)
        navyBold.textSize = 8.5f
        canvas.drawText(pageNum, 540f, 806f, navyBold)
    }
}
