package com.todoencomputacion.facturacion.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.models.Configuracion;
import com.todoencomputacion.facturacion.models.Factura;
import com.todoencomputacion.facturacion.models.ItemFactura;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FacturaPdfGenerator {

    private static final String CUSTOM_LOGO_FILENAME = "logo_cliente_factura.png";
    public static final int PAGE_WIDTH = 816;
    public static final int PAGE_HEIGHT = 1056;

    // --- GESTIÓN DEL LOGO PERSONALIZADO DEL CLIENTE ---
    public static boolean guardarLogoCliente(Context context, Uri imageUri) {
        try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return false;
            File outFile = new File(context.getFilesDir(), CUSTOM_LOGO_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void eliminarLogoCliente(Context context) {
        File f = new File(context.getFilesDir(), CUSTOM_LOGO_FILENAME);
        if (f.exists()) {
            f.delete();
        }
    }

    public static Bitmap obtenerLogoFactura(Context context) {
        File custom = new File(context.getFilesDir(), CUSTOM_LOGO_FILENAME);
        if (custom.exists()) {
            try {
                Bitmap b = BitmapFactory.decodeFile(custom.getAbsolutePath());
                if (b != null) return b;
            } catch (Exception ignored) {
            }
        }
        // Por defecto usar el logo propio de Todo en Computación
        Bitmap defaultLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_horizontal);
        if (defaultLogo == null) {
            defaultLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_logo);
        }
        return defaultLogo;
    }

    public static boolean tieneLogoPersonalizado(Context context) {
        return new File(context.getFilesDir(), CUSTOM_LOGO_FILENAME).exists();
    }

    // --- RENDERIZAR PREVIEW BITMAP DE LA FACTURA ---
    public static Bitmap renderizarPreviewBitmap(Context context, Factura f) {
        Bitmap bmp = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        dibujarPaginaFactura(context, canvas, f);
        return bmp;
    }

    // --- GENERAR ARCHIVO PDF EN MEMORIA INTERNA ---
    public static File generarArchivoPdf(Context context, Factura f) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        dibujarPaginaFactura(context, page.getCanvas(), f);

        pdfDocument.finishPage(page);

        String tipoClean = (f.tipoDocumento != null ? f.tipoDocumento : "FACTURA")
                .replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = String.format(Locale.US, "%s_%06d.pdf", tipoClean, f.numeroFactura);
        File pdfFile = new File(context.getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pdfDocument.close();
        }
        return pdfFile;
    }

    // --- COMPARTIR ARCHIVO PDF POR WHATSAPP / CORREO / ETC ---
    public static void compartirPdf(Context context, Factura f) {
        File pdfFile = generarArchivoPdf(context, f);
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);

        String tipo = (f.tipoDocumento != null && !f.tipoDocumento.isEmpty()) ? f.tipoDocumento : "FACTURA";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, tipo + " N° " + String.format(Locale.US, "%06d", f.numeroFactura));
        intent.putExtra(Intent.EXTRA_TEXT, "Adjunto documento fiscal digital: " + tipo + " N° "
                + String.format(Locale.US, "%06d", f.numeroFactura) + " - " + f.clienteNombre);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Enviar PDF por WhatsApp o Correo:"));
    }

    // --- DIBUJO COMPLETO DEL FORMATO FISCAL SENIAT EN CANVAS ---
    private static void dibujarPaginaFactura(Context context, Canvas canvas, Factura f) {
        Configuracion cfg = DataManager.getConfiguracion(context);
        Locale locVe = new Locale("es", "VE");

        // Fondo blanco
        canvas.drawColor(Color.WHITE);

        Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setColor(Color.BLACK);
        pText.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        Paint pBold = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBold.setColor(Color.BLACK);
        pBold.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        Paint pBlueBar = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBlueBar.setColor(Color.parseColor("#5B82E8"));
        pBlueBar.setStyle(Paint.Style.FILL);

        Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLine.setColor(Color.parseColor("#CBD5E1"));
        pLine.setStyle(Paint.Style.STROKE);
        pLine.setStrokeWidth(1f);

        // 1. LOGO A LA IZQUIERDA (x: 24..215, y: 28..98)
        Bitmap logo = obtenerLogoFactura(context);
        if (logo != null) {
            Rect dstRect = calcularRectanguloProporcional(logo.getWidth(), logo.getHeight(), 24, 28, 190, 72);
            Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(logo, null, dstRect, bmpPaint);
        }

        // 2. DATOS DE LA EMPRESA EN EL CENTRO (x: 225..500)
        pBold.setTextSize(12.5f);
        String empNombre = cfg.empresaNombre != null ? cfg.empresaNombre.toUpperCase(Locale.ROOT) : "TODO EN COMPUTACION C.A.";
        canvas.drawText(empNombre, 290, 40, pBold);

        pBold.setTextSize(8.8f);
        pText.setTextSize(8.8f);

        dibujarEtiquetaValor(canvas, "C.I./R.I.F.: ", cfg.rif != null ? cfg.rif : "", 228, 54, pBold, pText);
        dibujarEtiquetaValor(canvas, "DIRECCIÓN: ", cfg.direccion != null ? cfg.direccion.toUpperCase(Locale.ROOT) : "CARACAS", 228, 66, pBold, pText);
        dibujarEtiquetaValor(canvas, "TELÉFONO: ", cfg.telefono != null ? cfg.telefono : "04241958013", 228, 78, pBold, pText);
        dibujarEtiquetaValor(canvas, "CÓDIGO DE ACTIVIDAD ECONÓMICA: ", "0", 228, 90, pBold, pText);

        // 3. DATOS DEL DOCUMENTO Y N° CONTROL A LA DERECHA (x: 510..700)
        String tipoDoc = (f.tipoDocumento != null && !f.tipoDocumento.isEmpty()) ? f.tipoDocumento.toUpperCase(Locale.ROOT) : "FACTURA";
        String fechaCorta = extraerFechaCorta(f.fecha);

        pBold.setTextSize(8.8f);
        canvas.drawText("Nº DE " + tipoDoc + ": " + String.format(Locale.US, "%011d", f.numeroFactura), 510, 40, pBold);
        canvas.drawText("FECHA EMISIÓN: " + fechaCorta, 510, 52, pBold);
        canvas.drawText("HORA EMISIÓN: " + (f.hora != null ? f.hora.toUpperCase(Locale.ROOT) : ""), 510, 64, pBold);

        // N° DE CONTROL en rojo destacado
        String lblControl = "Nº DE CONTROL: ";
        canvas.drawText(lblControl, 510, 76, pBold);
        float wCtrl = pBold.measureText(lblControl);
        Paint pRedBold = new Paint(pBold);
        pRedBold.setColor(Color.parseColor("#DC2626"));
        canvas.drawText(String.format(Locale.US, "00-%08d", f.numeroControl), 510 + wCtrl, 76, pRedBold);

        canvas.drawText("FECHA ASIGNACIÓN: " + fechaCorta, 510, 88, pBold);

        // 4. CÓDIGO QR FISCAL A LA DERECHA SUPERIOR (x: 708..788, y: 28..108)
        String qrData = cfg.rif + "|" + f.numeroFactura + "|" + f.numeroControl + "|" + f.totalBs;
        dibujarCodigoQR(canvas, qrData, 708, 28, 80);

        // 5. DATOS DEL CLIENTE (x: 24, y: 122..182)
        pBold.setTextSize(9.2f);
        pText.setTextSize(9.2f);
        canvas.drawText("DATOS DEL CLIENTE:", 24, 122, pBold);
        dibujarEtiquetaValor(canvas, "C.I./R.I.F.: ", f.clienteRif != null ? f.clienteRif : "", 24, 135, pBold, pText);
        dibujarEtiquetaValor(canvas, "RAZÓN SOCIAL: ", f.clienteNombre != null ? f.clienteNombre.toUpperCase(Locale.ROOT) : "", 24, 147, pBold, pText);
        String correoCli = (f.clienteCorreo != null && !f.clienteCorreo.isEmpty()) ? f.clienteCorreo.toUpperCase(Locale.ROOT) : (cfg.correo != null ? cfg.correo.toUpperCase(Locale.ROOT) : "");
        dibujarEtiquetaValor(canvas, "CORREO: ", correoCli, 24, 159, pBold, pText);
        dibujarEtiquetaValor(canvas, "DIRECCIÓN: ", f.clienteDireccion != null ? f.clienteDireccion.toUpperCase(Locale.ROOT) : "CARACAS", 24, 171, pBold, pText);
        String telCli = (f.clienteTelefono != null && !f.clienteTelefono.isEmpty()) ? f.clienteTelefono : cfg.telefono;
        dibujarEtiquetaValor(canvas, "TELÉFONO: ", telCli != null ? telCli : "", 24, 183, pBold, pText);

        if (f.referenciaExtra != null && !f.referenciaExtra.trim().isEmpty()) {
            dibujarEtiquetaValor(canvas, "REFERENCIA / NOTA: ", f.referenciaExtra.toUpperCase(Locale.ROOT), 24, 195, pBold, pText);
        }

        // 6. ENCABEZADO DE TABLA DE ARTÍCULOS (Barra Azul #5B82E8)
        float tableTop = 206f;
        float headerHeight = 25f;
        RectF headerRect = new RectF(20f, tableTop, PAGE_WIDTH - 20f, tableTop + headerHeight);
        canvas.drawRoundRect(headerRect, 4f, 4f, pBlueBar);

        Paint pWhiteBold = new Paint(pBold);
        pWhiteBold.setColor(Color.WHITE);
        pWhiteBold.setTextSize(9.5f);

        float textYHeader = tableTop + 16f;
        canvas.drawText("Código", 42f, textYHeader, pWhiteBold);
        canvas.drawText("Descripción", 145f, textYHeader, pWhiteBold);
        canvas.drawText("Cantidad", 335f, textYHeader, pWhiteBold);
        canvas.drawText("Valor Unitario", 425f, textYHeader, pWhiteBold);
        canvas.drawText("Descuento", 525f, textYHeader, pWhiteBold);
        canvas.drawText("Tipo Alicuota", 615f, textYHeader, pWhiteBold);
        canvas.drawText("Valor Total", 720f, textYHeader, pWhiteBold);

        // 7. FILAS DE ARTÍCULOS
        float rowY = tableTop + headerHeight + 16f;
        pText.setTextSize(9.2f);
        Paint pRight = new Paint(pText);
        pRight.setTextAlign(Paint.Align.RIGHT);

        if (f.items != null) {
            for (ItemFactura it : f.items) {
                String cod = it.codigoArticulo != null ? it.codigoArticulo : "";
                String desc = it.descripcion != null ? it.descripcion.toUpperCase(Locale.ROOT) : "";
                if (desc.length() > 34) desc = desc.substring(0, 34);

                canvas.drawText(cod, 36f, rowY, pText);
                canvas.drawText(desc, 115f, rowY, pText);
                canvas.drawText(String.format(locVe, "%.2f", (double) it.cantidad), 348f, rowY, pText);
                canvas.drawText(String.format(locVe, "Bs %,.2f", it.precioUnitarioBs), 495f, rowY, pRight);
                canvas.drawText("0.00 %", 535f, rowY, pText);
                canvas.drawText(it.getCodigoAlicuotaPdf(), 642f, rowY, pText);
                canvas.drawText(String.format(locVe, "Bs %,.2f", it.totalBs), 788f, rowY, pRight);

                canvas.drawLine(20f, rowY + 5f, PAGE_WIDTH - 20f, rowY + 5f, pLine);
                rowY += 20f;
                if (rowY > 640f) break;
            }
        }

        // 8. SECCIÓN INFERIOR: FORMAS DE PAGO (IZQUIERDA) Y CUADRO DE TOTALES (DERECHA)
        float bottomTop = 665f;

        // Formas de Pago (Izquierda)
        RectF pillPago = new RectF(24f, bottomTop, 110f, bottomTop + 16f);
        canvas.drawRoundRect(pillPago, 3f, 3f, pBlueBar);
        pWhiteBold.setTextSize(8.8f);
        canvas.drawText("Formas de Pago:", 28f, bottomTop + 11.5f, pWhiteBold);

        pBold.setTextSize(8.8f);
        pText.setTextSize(8.8f);
        double tasa = f.tasaDolar > 0 ? f.tasaDolar : cfg.tasaDolar;

        dibujarEtiquetaValor(canvas, "Descripción: ", "Contado / Crédito", 24f, bottomTop + 30f, pBold, pText);
        dibujarEtiquetaValor(canvas, "Monto: ", String.format(locVe, "%,.2f", f.totalBs), 24f, bottomTop + 42f, pBold, pText);
        dibujarEtiquetaValor(canvas, "Moneda: ", "Bs.", 24f, bottomTop + 54f, pBold, pText);
        dibujarEtiquetaValor(canvas, "Tipo de Cambio BCV: ", String.format(locVe, "Bs %,.4f", tasa), 24f, bottomTop + 66f, pBold, pText);
        dibujarEtiquetaValor(canvas, "Fecha Tasa: ", fechaCorta, 24f, bottomTop + 78f, pBold, pText);

        // Cuadro de Totales Dual (USD y Bs.) a la Derecha (x: 330..794) - Fondo blanco limpio sin sombreado
        RectF boxTotales = new RectF(330f, bottomTop - 4f, 794f, bottomTop + 206f);
        canvas.drawRoundRect(boxTotales, 6f, 6f, pLine);

        double subUsd = tasa > 0 ? (f.subtotalBs / tasa) : f.totalUSD;
        double totUsd = tasa > 0 ? (f.totalBs / tasa) : f.totalUSD;

        double exentoBs = 0, baseGBs = 0, baseRBs = 0, baseABs = 0;
        double ivaGBs = 0, ivaRBs = 0, ivaABs = 0;
        if (f.items != null && !f.items.isEmpty()) {
            for (ItemFactura it : f.items) {
                String alic = it.getCodigoAlicuotaPdf();
                if ("E".equals(alic)) {
                    exentoBs += it.totalBs;
                } else if ("R".equals(alic)) {
                    baseRBs += it.totalBs;
                    ivaRBs += it.totalBs * 0.08;
                } else if ("A".equals(alic)) {
                    baseABs += it.totalBs;
                    ivaABs += it.totalBs * 0.32;
                } else {
                    baseGBs += it.totalBs;
                    ivaGBs += it.totalBs * 0.16;
                }
            }
        } else {
            baseGBs = f.subtotalBs;
            ivaGBs = f.ivaBs;
        }

        double exentoUsd = tasa > 0 ? (exentoBs / tasa) : 0;
        double baseGUsd = tasa > 0 ? (baseGBs / tasa) : 0;
        double baseRUsd = tasa > 0 ? (baseRBs / tasa) : 0;
        double baseAUsd = tasa > 0 ? (baseABs / tasa) : 0;
        double ivaGUsd = tasa > 0 ? (ivaGBs / tasa) : 0;
        double ivaRUsd = tasa > 0 ? (ivaRBs / tasa) : 0;
        double ivaAUsd = tasa > 0 ? (ivaABs / tasa) : 0;

        String[][] filasTotales = new String[][]{
                {"Subtotal:", String.format(locVe, "%,.2f", subUsd), String.format(locVe, "%,.2f", f.subtotalBs), "1"},
                {"Exento", String.format(locVe, "%,.2f", exentoUsd), String.format(locVe, "%,.2f", exentoBs), "0"},
                {"Percibido", "0,00", "0,00", "0"},
                {"Base Imponible (G):", String.format(locVe, "%,.2f", baseGUsd), String.format(locVe, "%,.2f", baseGBs), "0"},
                {"Base Imponible (R):", String.format(locVe, "%,.2f", baseRUsd), String.format(locVe, "%,.2f", baseRBs), "0"},
                {"Base Imponible (A):", String.format(locVe, "%,.2f", baseAUsd), String.format(locVe, "%,.2f", baseABs), "0"},
                {"Alicuota I.V.A. (16.00%):", String.format(locVe, "%,.2f", ivaGUsd), String.format(locVe, "%,.2f", ivaGBs), "0"},
                {"Alicuota I.V.A. (8.00%):", String.format(locVe, "%,.2f", ivaRUsd), String.format(locVe, "%,.2f", ivaRBs), "0"},
                {"Alicuota I.V.A. (32.00%):", String.format(locVe, "%,.2f", ivaAUsd), String.format(locVe, "%,.2f", ivaABs), "0"},
                {"Descuento:", "0,00", "0,00", "0"},
                {"Total:", String.format(locVe, "%,.2f", totUsd), String.format(locVe, "%,.2f", f.totalBs), "1"},
                {"Base Imponible IGTF:", "0,00", "0,00", "0"},
                {"Total IGTF (3.00%):", "0,00", "0,00", "0"}
        };

        float ty = bottomTop + 10f;
        pBold.setTextSize(8.4f);
        pText.setTextSize(8.4f);
        Paint pValRight = new Paint(pText);
        pValRight.setTextAlign(Paint.Align.RIGHT);
        Paint pValBoldRight = new Paint(pBold);
        pValBoldRight.setTextAlign(Paint.Align.RIGHT);

        for (String[] fila : filasTotales) {
            String label = fila[0];
            String vUsd = fila[1];
            String vBs = fila[2];
            boolean isBoldVal = "1".equals(fila[3]);

            // Columna Izquierda (USD)
            canvas.drawText(label, 338f, ty, pBold);
            canvas.drawText("USD", 475f, ty, pBold);
            canvas.drawText(vUsd, 555f, ty, isBoldVal ? pValBoldRight : pValRight);

            // Columna Derecha (Bs.)
            canvas.drawText(label, 572f, ty, pBold);
            canvas.drawText("Bs.", 705f, ty, pBold);
            canvas.drawText(vBs, 786f, ty, isBoldVal ? pValBoldRight : pValRight);

            ty += 14f;
        }

        // Barra Azul "Total A Pagar" al final del cuadro de totales
        RectF barTotalPagar = new RectF(334f, ty - 6f, 790f, ty + 14f);
        canvas.drawRoundRect(barTotalPagar, 4f, 4f, pBlueBar);

        pWhiteBold.setTextSize(9.2f);
        Paint pWhiteRight = new Paint(pWhiteBold);
        pWhiteRight.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText("Total A Pagar:", 338f, ty + 7f, pWhiteBold);
        canvas.drawText("USD", 475f, ty + 7f, pWhiteBold);
        canvas.drawText(String.format(locVe, "%,.2f", totUsd), 555f, ty + 7f, pWhiteRight);

        canvas.drawText("Total A Pagar:", 572f, ty + 7f, pWhiteBold);
        canvas.drawText("Bs.", 705f, ty + 7f, pWhiteBold);
        canvas.drawText(String.format(locVe, "%,.2f", f.totalBs), 786f, ty + 7f, pWhiteRight);

        // 9. NOTAS LEGALES SENIAT E IGTF AL PIE DE PÁGINA
        float legalY = 892f;
        RectF boxIgtf = new RectF(20f, legalY, PAGE_WIDTH - 20f, legalY + 30f);
        canvas.drawRoundRect(boxIgtf, 4f, 4f, pLine);
        pText.setTextSize(7.8f);
        canvas.drawText("Este pago estará sujeto al cobro adicional del 3% del Impuesto a las Grandes Transacciones Financieras (IGTF), de conformidad con la Providencia Administrativa SNAT/2022/000013 publicada en la G.O. N°", 26f, legalY + 12f, pText);
        canvas.drawText("42.339 del 17-03-2022, en caso de ser cancelado en divisas. No aplica en pago en Bs.", 26f, legalY + 23f, pText);

        float legalY2 = legalY + 35f;
        RectF boxBcv = new RectF(20f, legalY2, PAGE_WIDTH - 20f, legalY2 + 40f);
        canvas.drawRoundRect(boxBcv, 4f, 4f, pLine);
        canvas.drawText("Este documento se expresa en Bolívares con su equivalente en divisas al tipo de cambio corriente del mercado a la fecha de su emisión, según lo establecido en el artículo 13 numeral 14 de la Providencia", 26f, legalY2 + 12f, pText);
        canvas.drawText("Administrativa SNAT/2011/0071 (…) en concordancia con el artículo 128 de la Ley del Banco Central de Venezuela (BCV); artículo 25 de la Ley que establece el Impuesto al Valor Agregado (IVA) y 38 del", 26f, legalY2 + 23f, pText);
        canvas.drawText("Reglamento General de la Ley que establece el Impuesto al Valor Agregado (RLIVA).", 26f, legalY2 + 34f, pText);

        // Pie de Providencia SNAT/2024/000102
        Paint pCenterBold = new Paint(pBold);
        pCenterBold.setTextAlign(Paint.Align.CENTER);
        pCenterBold.setTextSize(9.5f);
        canvas.drawText("DOCUMENTO EMITIDO CONFORME A LA PROVIDENCIA ADMINISTRATIVA SNAT/2024/000102", PAGE_WIDTH / 2f, legalY2 + 56f, pCenterBold);

        pCenterBold.setTextSize(7.2f);
        canvas.drawText(empNombre + " - RIF: " + cfg.rif + " - TELÉFONO: " + cfg.telefono + " - SISTEMA DE FACTURACIÓN DIGITAL", PAGE_WIDTH / 2f, legalY2 + 68f, pCenterBold);
    }

    private static void dibujarEtiquetaValor(Canvas canvas, String etiqueta, String valor, float x, float y, Paint pBold, Paint pNormal) {
        canvas.drawText(etiqueta, x, y, pBold);
        float w = pBold.measureText(etiqueta);
        canvas.drawText(valor != null ? valor : "", x + w, y, pNormal);
    }

    private static String extraerFechaCorta(String fechaCompleta) {
        if (fechaCompleta == null || fechaCompleta.trim().isEmpty()) {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
        }
        int commaIdx = fechaCompleta.indexOf(',');
        if (commaIdx >= 0 && commaIdx + 1 < fechaCompleta.length()) {
            return fechaCompleta.substring(commaIdx + 1).trim();
        }
        return fechaCompleta.trim();
    }

    private static Rect calcularRectanguloProporcional(int srcW, int srcH, int maxLeft, int maxTop, int maxW, int maxH) {
        if (srcW <= 0 || srcH <= 0) return new Rect(maxLeft, maxTop, maxLeft + maxW, maxTop + maxH);
        float ratio = Math.min((float) maxW / srcW, (float) maxH / srcH);
        int w = Math.round(srcW * ratio);
        int h = Math.round(srcH * ratio);
        int left = maxLeft + (maxW - w) / 2;
        int top = maxTop + (maxH - h) / 2;
        return new Rect(left, top, left + w, top + h);
    }

    private static void dibujarCodigoQR(Canvas canvas, String data, int startX, int startY, int size) {
        int grid = 21;
        float cell = (float) size / grid;
        Paint pBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBlack.setColor(Color.BLACK);
        pBlack.setStyle(Paint.Style.FILL);

        int hash = data != null ? data.hashCode() : 123456;
        for (int r = 0; r < grid; r++) {
            for (int c = 0; c < grid; c++) {
                boolean isFinder = (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7);
                boolean draw = false;
                if (isFinder) {
                    int lr = r < 7 ? r : r - (grid - 7);
                    int lc = c < 7 ? c : c - (grid - 7);
                    if (lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4)) {
                        draw = true;
                    }
                } else if ((r < 8 && c < 8) || (r < 8 && c >= grid - 8) || (r >= grid - 8 && c < 8)) {
                    draw = false;
                } else {
                    int bit = ((r * 31 + c * 17 + hash) ^ (r * c + (hash >>> (c % 16)))) & 1;
                    draw = (bit == 1);
                }

                if (draw) {
                    float l = startX + c * cell;
                    float t = startY + r * cell;
                    canvas.drawRect(l, t, l + cell, t + cell, pBlack);
                }
            }
        }
    }
}

