package com.sistema.paciencare.ui.manager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun printOrSavePdf(
        context: Context,
        tabName: String,
        tabIndex: Int,
        periodText: String,
        metrics: List<Pair<String, String>>,
        chartData: List<Int> = emptyList()
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Servicio de impresión no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "PacienCare_Reporte_${tabName}_${System.currentTimeMillis()}"

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var pdfDocument: PdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    pdfDocument = PdfDocument()

                    val builder = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)

                    val info = builder.build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    val document = pdfDocument ?: return
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Formato A4 (595x842 pt)
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val linePaint = Paint()
                    val titlePaint = Paint().apply {
                        color = AndroidColor.parseColor("#0284C7")
                        textSize = 20f
                        isFakeBoldText = true
                    }
                    val subtitlePaint = Paint().apply {
                        color = AndroidColor.parseColor("#64748B")
                        textSize = 11f
                    }
                    val headerPaint = Paint().apply {
                        color = AndroidColor.parseColor("#1E293B")
                        textSize = 14f
                        isFakeBoldText = true
                    }
                    val labelPaint = Paint().apply {
                        color = AndroidColor.parseColor("#1E293B")
                        textSize = 12f
                    }
                    val valuePaint = Paint().apply {
                        color = AndroidColor.parseColor("#0284C7")
                        textSize = 12f
                        isFakeBoldText = true
                    }

                    // Encabezado del PDF
                    canvas.drawText("Centro de Salud Tisma", 40f, 45f, titlePaint)
                    canvas.drawText("Informe Gerencial Administrativo — Módulo: $tabName", 40f, 65f, subtitlePaint)
                    canvas.drawText("Filtro: $periodText | Emisión: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, 82f, subtitlePaint)

                    // Línea divisora superior
                    linePaint.color = AndroidColor.parseColor("#0284C7")
                    linePaint.strokeWidth = 2f
                    canvas.drawLine(40f, 95f, 555f, 95f, linePaint)

                    // Título de Sección
                    canvas.drawText("Resumen de Métricas Registradas", 40f, 120f, headerPaint)

                    var yPosition = 145f
                    metrics.forEach { (label, value) ->
                        canvas.drawText(label, 40f, yPosition, labelPaint)
                        canvas.drawText(value, 420f, yPosition, valuePaint)

                        // Línea divisora suave por fila
                        linePaint.color = AndroidColor.parseColor("#E2E8F0")
                        linePaint.strokeWidth = 1f
                        canvas.drawLine(40f, yPosition + 6f, 555f, yPosition + 6f, linePaint)

                        yPosition += 26f
                    }

                    // DIBUJAR GRÁFICOS SEGÚN EL MÓDULO (TAB)
                    yPosition += 25f

                    when (tabIndex) {
                        0, 1 -> { // Resumen o Citas
                            canvas.drawText("Gráfico: Citas por Mes (12 Meses)", 40f, yPosition, headerPaint)
                            yPosition += 22f

                            val barPaint = Paint().apply { color = AndroidColor.parseColor("#0284C7") }
                            val textMonthPaint = Paint().apply {
                                color = AndroidColor.parseColor("#64748B")
                                textSize = 8f
                                textAlign = Paint.Align.CENTER
                            }

                            val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                            val chartDataList = if (chartData.size == 12) chartData else listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                            val maxVal = (chartDataList.maxOrNull() ?: 10).coerceAtLeast(1)

                            // Centrado de barras en A4
                            val startX = 65f
                            val barSpacing = 41f
                            val chartHeight = 75f
                            val baseY = yPosition + chartHeight

                            // Eje horizontal
                            linePaint.color = AndroidColor.parseColor("#CBD5E1")
                            linePaint.strokeWidth = 1.5f
                            canvas.drawLine(40f, baseY, 555f, baseY, linePaint)

                            months.forEachIndexed { idx, mName ->
                                val xPos = startX + idx * barSpacing
                                val valVal = chartDataList.getOrElse(idx) { 0 }
                                val barH = if (valVal > 0) (valVal.toFloat() / maxVal) * chartHeight else 6f

                                val rect = RectF(xPos - 9f, baseY - barH, xPos + 9f, baseY)
                                canvas.drawRoundRect(rect, 3f, 3f, barPaint)

                                canvas.drawText(mName, xPos, baseY + 14f, textMonthPaint)
                            }

                            yPosition = baseY + 50f

                            // Gráfico 2: Satisfacción General (Donut Chart Centrado)
                            canvas.drawText("Gráfico: Satisfacción General", 40f, yPosition, headerPaint)
                            yPosition += 20f

                            val donutRect = RectF(60f, yPosition, 160f, yPosition + 100f)
                            val ringPaint = Paint().apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 20f
                                isAntiAlias = true
                            }

                            // Donut arcs
                            ringPaint.color = AndroidColor.parseColor("#0284C7")
                            canvas.drawArc(donutRect, -90f, 360f * 0.52f, false, ringPaint)

                            ringPaint.color = AndroidColor.parseColor("#10B981")
                            canvas.drawArc(donutRect, -90f + 360f * 0.52f, 360f * 0.28f, false, ringPaint)

                            ringPaint.color = AndroidColor.parseColor("#F59E0B")
                            canvas.drawArc(donutRect, -90f + 360f * 0.80f, 360f * 0.12f, false, ringPaint)

                            ringPaint.color = AndroidColor.parseColor("#EF4444")
                            canvas.drawArc(donutRect, -90f + 360f * 0.92f, 360f * 0.08f, false, ringPaint)

                            // Legend
                            val legendX = 220f
                            var legendY = yPosition + 22f
                            val legendTextPaint = Paint().apply {
                                color = AndroidColor.parseColor("#1E293B")
                                textSize = 11f
                            }
                            val dotPaint = Paint().apply { isAntiAlias = true }

                            listOf(
                                "Muy Satisfecho (52%)" to "#0284C7",
                                "Satisfecho (28%)" to "#10B981",
                                "Neutral (12%)" to "#F59E0B",
                                "Insatisfecho (8%)" to "#EF4444"
                            ).forEach { (legendText, hexColor) ->
                                dotPaint.color = AndroidColor.parseColor(hexColor)
                                canvas.drawCircle(legendX, legendY - 3f, 5f, dotPaint)
                                canvas.drawText(legendText, legendX + 14f, legendY, legendTextPaint)
                                legendY += 20f
                            }
                        }
                        2 -> { // Medicamentos
                            canvas.drawText("Gráfico: Medicamentos más utilizados", 40f, yPosition, headerPaint)
                            yPosition += 25f

                            val barPaint = Paint().apply { color = AndroidColor.parseColor("#10B981") }
                            val bgBarPaint = Paint().apply { color = AndroidColor.parseColor("#F1F5F9") }

                            listOf(
                                "Losartán" to 0.9f,
                                "Metformina" to 0.7f,
                                "Atorvastatina" to 0.5f,
                                "Aspirina" to 0.4f,
                                "Omeprazol" to 0.3f
                            ).forEach { (medName, fraction) ->
                                canvas.drawText(medName, 40f, yPosition, labelPaint)

                                val rectBg = RectF(160f, yPosition - 10f, 520f, yPosition + 2f)
                                canvas.drawRoundRect(rectBg, 4f, 4f, bgBarPaint)

                                val rectVal = RectF(160f, yPosition - 10f, 160f + (360f * fraction), yPosition + 2f)
                                canvas.drawRoundRect(rectVal, 4f, 4f, barPaint)

                                yPosition += 28f
                            }
                        }
                        else -> { // Satisfacción
                            canvas.drawText("Gráfico: Distribución de Calificaciones", 40f, yPosition, headerPaint)
                            yPosition += 25f

                            val bgBarPaint = Paint().apply { color = AndroidColor.parseColor("#F1F5F9") }
                            val barPaint = Paint()

                            listOf(
                                "5 ★ (Excelente)" to (0.72f to "#10B981"),
                                "4 ★ (Muy bueno)" to (0.20f to "#0284C7"),
                                "3 ★ (Bueno)" to (0.05f to "#F59E0B"),
                                "2 ★ (Malo)" to (0.02f to "#F97316"),
                                "1 ★ (Muy malo)" to (0.01f to "#EF4444")
                            ).forEach { (starLabel, pair) ->
                                val (fraction, hexColor) = pair
                                canvas.drawText(starLabel, 40f, yPosition, labelPaint)

                                val rectBg = RectF(160f, yPosition - 10f, 500f, yPosition + 2f)
                                canvas.drawRoundRect(rectBg, 4f, 4f, bgBarPaint)

                                barPaint.color = AndroidColor.parseColor(hexColor)
                                val rectVal = RectF(160f, yPosition - 10f, 160f + (340f * fraction), yPosition + 2f)
                                canvas.drawRoundRect(rectVal, 4f, 4f, barPaint)

                                canvas.drawText("${(fraction * 100).toInt()}%", 515f, yPosition, subtitlePaint)

                                yPosition += 28f
                            }
                        }
                    }

                    // Pie de página
                    val footerPaint = Paint().apply {
                        color = AndroidColor.parseColor("#94A3B8")
                        textSize = 10f
                    }
                    canvas.drawText("Documento generado desde el sistema local PacienCare.", 40f, 810f, footerPaint)

                    document.finishPage(page)

                    try {
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            document.writeTo(output)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        document.close()
                    }
                }
            },
            null
        )
    }
}
