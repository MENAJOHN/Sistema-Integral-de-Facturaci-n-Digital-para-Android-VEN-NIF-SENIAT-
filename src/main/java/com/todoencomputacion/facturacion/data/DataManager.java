package com.todoencomputacion.facturacion.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.todoencomputacion.facturacion.models.Articulo;
import com.todoencomputacion.facturacion.models.Cliente;
import com.todoencomputacion.facturacion.models.Configuracion;
import com.todoencomputacion.facturacion.models.Factura;
import com.todoencomputacion.facturacion.models.ItemFactura;
import com.todoencomputacion.facturacion.models.PermisosPerfil;
import com.todoencomputacion.facturacion.models.Vendedor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DataManager {

    public interface OnTasaBCVListener {
        void onTasaActualizada(double nuevaTasa);
    }

    private static final String FILE_CONFIG = "configuracion.json";
    private static final String FILE_CLIENTES = "clientes.json";
    private static final String FILE_ARTICULOS = "articulos.json";
    private static final String FILE_VENDEDORES = "vendedores.json";
    private static final String FILE_FACTURAS = "facturas.json";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Sesión en memoria
    public static String currentUser = "usuario";
    public static boolean isMaster = false;

    // --- CONFIGURACIÓN ---
    public static Configuracion getConfiguracion(Context context) {
        String json = readFile(context, FILE_CONFIG);
        if (json == null || json.trim().isEmpty()) {
            Configuracion def = new Configuracion();
            saveConfiguracion(context, def);
            return def;
        }
        try {
            Configuracion cfg = gson.fromJson(json, Configuracion.class);
            if (cfg == null) {
                cfg = new Configuracion();
            }
            boolean changed = false;
            if (cfg.claveNormal == null || cfg.claveNormal.equals("usuario")) {
                cfg.claveNormal = "1234";
                changed = true;
            }
            if (cfg.usuarioImprenta == null) {
                cfg.usuarioImprenta = "";
                changed = true;
            }
            if (cfg.claveImprenta == null) {
                cfg.claveImprenta = "";
                changed = true;
            }
            if (cfg.tokenImprenta == null) {
                cfg.tokenImprenta = "";
                changed = true;
            }
            if (cfg.tasaDolar <= 0 || cfg.tasaDolar == 36.50) {
                cfg.tasaDolar = 899.27;
                changed = true;
            }
            if (cfg.tasaEuro <= 0) {
                cfg.tasaEuro = 1006.61;
                changed = true;
            }
            if (cfg.usuarioCaja == null || cfg.usuarioCaja.isEmpty()) {
                cfg.usuarioCaja = "caja";
                changed = true;
            }
            if (cfg.claveCaja == null || cfg.claveCaja.isEmpty()) {
                cfg.claveCaja = "1234";
                changed = true;
            }
            if (cfg.permisosUsuario == null) {
                cfg.permisosUsuario = new PermisosPerfil();
                changed = true;
            }
            if (cfg.permisosCaja == null) {
                cfg.permisosCaja = new PermisosPerfil();
                changed = true;
            }
            if (changed) {
                saveConfiguracion(context, cfg);
            }
            return cfg;
        } catch (Exception e) {
            e.printStackTrace();
            return new Configuracion();
        }
    }

    public static void saveConfiguracion(Context context, Configuracion config) {
        writeFile(context, FILE_CONFIG, gson.toJson(config));
        if (config != null && config.tasaDolar > 0) {
            List<Articulo> articulos = getArticulos(context);
            if (!articulos.isEmpty()) {
                for (Articulo a : articulos) {
                    a.precioBs = Math.round(a.precioUSD * config.tasaDolar * 100.0) / 100.0;
                }
                saveArticulos(context, articulos);
            }
        }
    }

    // --- ACTUALIZACIÓN AUTOMÁTICA DE TASA DESDE BCV (https://www.bcv.org.ve/) ---
    public static void actualizarTasaDesdeBCV(Context context, OnTasaBCVListener listener) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                            @Override
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());

                URL url = new URL("https://www.bcv.org.ve/");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setSSLSocketFactory(sslContext.getSocketFactory());
                conn.setHostnameVerifier((hostname, session) -> true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }
                    String html = sb.toString();

                    // Extraer el valor al lado de EUR dentro del bloque id="euro"
                    double tasaEuroRedondeada = 0;
                    Pattern patEuro = Pattern.compile("id=[\"']euro[\"'][\\s\\S]*?<strong[^>]*>\\s*([0-9.,]+)\\s*</strong>", Pattern.CASE_INSENSITIVE);
                    Matcher matEuro = patEuro.matcher(html);
                    boolean foundEuro = matEuro.find();
                    if (!foundEuro) {
                        patEuro = Pattern.compile("EUR\\s*</span>[\\s\\S]*?<strong[^>]*>\\s*([0-9.,]+)\\s*</strong>", Pattern.CASE_INSENSITIVE);
                        matEuro = patEuro.matcher(html);
                        foundEuro = matEuro.find();
                    }
                    if (foundEuro) {
                        String euroRaw = matEuro.group(1).trim().replace(".", "").replace(",", ".");
                        double tasaEuroBCV = Double.parseDouble(euroRaw);
                        tasaEuroRedondeada = Math.round(tasaEuroBCV * 100.0) / 100.0;
                    }

                    // Extraer el valor al lado de USD dentro del bloque id="dolar"
                    Pattern pattern = Pattern.compile("id=[\"']dolar[\"'][\\s\\S]*?<strong[^>]*>\\s*([0-9.,]+)\\s*</strong>", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(html);
                    boolean found = matcher.find();
                    if (!found) {
                        pattern = Pattern.compile("USD\\s*</span>[\\s\\S]*?<strong[^>]*>\\s*([0-9.,]+)\\s*</strong>", Pattern.CASE_INSENSITIVE);
                        matcher = pattern.matcher(html);
                        found = matcher.find();
                    }

                    if (found) {
                        String valorRaw = matcher.group(1).trim();
                        String valorLimpio = valorRaw.replace(".", "").replace(",", ".");
                        double tasaBCV = Double.parseDouble(valorLimpio);
                        double tasaRedondeada = Math.round(tasaBCV * 100.0) / 100.0;

                        if (tasaRedondeada > 0) {
                            Configuracion cfg = getConfiguracion(appContext);
                            cfg.tasaDolar = tasaRedondeada;
                            if (tasaEuroRedondeada > 0) {
                                cfg.tasaEuro = tasaEuroRedondeada;
                            }
                            saveConfiguracion(appContext, cfg);

                            if (listener != null) {
                                new Handler(Looper.getMainLooper()).post(() -> listener.onTasaActualizada(tasaRedondeada));
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- CLIENTES ---
    public static List<Cliente> getClientes(Context context) {
        String json = readFile(context, FILE_CLIENTES);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<Cliente>>() {}.getType();
            List<Cliente> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveClientes(Context context, List<Cliente> clientes) {
        writeFile(context, FILE_CLIENTES, gson.toJson(clientes));
    }

    public static void addOrUpdateCliente(Context context, Cliente cliente) {
        List<Cliente> list = getClientes(context);
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).codigo.equalsIgnoreCase(cliente.codigo)) {
                list.set(i, cliente);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(cliente);
        }
        saveClientes(context, list);
    }

    // --- ARTICULOS ---
    public static List<Articulo> getArticulos(Context context) {
        String json = readFile(context, FILE_ARTICULOS);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<Articulo>>() {}.getType();
            List<Articulo> list = gson.fromJson(json, listType);
            if (list == null) list = new ArrayList<>();
            boolean tieneGenerico = false;
            for (Articulo a : list) {
                if (a.tipoArticulo == null || a.tipoArticulo.isEmpty()) {
                    a.tipoArticulo = "V";
                }
                if (a.tipoIva == null || a.tipoIva.isEmpty()) {
                    a.tipoIva = "16%";
                    a.porcentajeIva = 16.0;
                }
                if (Articulo.CODIGO_GENERICO.equalsIgnoreCase(a.codigo) || a.esGenerico) {
                    a.esGenerico = true;
                    a.tipoArticulo = "S";
                    tieneGenerico = true;
                }
            }
            if (!tieneGenerico) {
                Articulo gen = new Articulo(
                        Articulo.CODIGO_GENERICO,
                        "SERVICIO GENÉRICO (Descripción y Precio Libre)",
                        0.0,
                        0.0,
                        0,
                        "licencia.png",
                        "S",
                        "16%",
                        16.0
                );
                gen.esGenerico = true;
                list.add(0, gen);
                saveArticulos(context, list);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveArticulos(Context context, List<Articulo> articulos) {
        writeFile(context, FILE_ARTICULOS, gson.toJson(articulos));
    }

    public static void addOrUpdateArticulo(Context context, Articulo articulo) {
        List<Articulo> list = getArticulos(context);
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).codigo.equalsIgnoreCase(articulo.codigo)) {
                list.set(i, articulo);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(articulo);
        }
        saveArticulos(context, list);
    }

    // --- VENDEDORES ---
    public static List<Vendedor> getVendedores(Context context) {
        String json = readFile(context, FILE_VENDEDORES);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<Vendedor>>() {}.getType();
            List<Vendedor> list = gson.fromJson(json, listType);
            if (list == null) return new ArrayList<>();
            for (Vendedor v : list) {
                if (v.cedula == null) v.cedula = "";
                if (v.direccion == null) v.direccion = "";
                if (v.foto == null) v.foto = "";
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveVendedores(Context context, List<Vendedor> vendedores) {
        writeFile(context, FILE_VENDEDORES, gson.toJson(vendedores));
    }

    public static void addOrUpdateVendedor(Context context, Vendedor vendedor) {
        List<Vendedor> list = getVendedores(context);
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).codigo.equalsIgnoreCase(vendedor.codigo)) {
                list.set(i, vendedor);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(vendedor);
        }
        saveVendedores(context, list);
    }

    // --- FACTURAS ---
    public static List<Factura> getFacturas(Context context) {
        String json = readFile(context, FILE_FACTURAS);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<Factura>>() {}.getType();
            List<Factura> list = gson.fromJson(json, listType);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveFacturas(Context context, List<Factura> facturas) {
        writeFile(context, FILE_FACTURAS, gson.toJson(facturas));
    }

    public static synchronized Factura registrarFactura(Context context, Factura f) {
        Configuracion cfg = getConfiguracion(context);
        f.numeroFactura = cfg.proximoNumeroFactura;
        f.numeroControl = cfg.proximoNumeroControl;
        
        cfg.proximoNumeroFactura++;
        cfg.proximoNumeroControl++;
        saveConfiguracion(context, cfg);

        // Descontar inventario / stock únicamente para artículos Tipo Venta (V)
        List<Articulo> articulos = getArticulos(context);
        for (ItemFactura it : f.items) {
            for (Articulo a : articulos) {
                if (a.codigo.equalsIgnoreCase(it.codigoArticulo)) {
                    if (a.manejaStock()) {
                        if (cfg.permitirStockNegativo) {
                            a.stock = a.stock - it.cantidad;
                        } else {
                            a.stock = Math.max(0, a.stock - it.cantidad);
                        }
                    }
                    break;
                }
            }
        }
        saveArticulos(context, articulos);

        List<Factura> facturas = getFacturas(context);
        facturas.add(0, f); // al principio
        saveFacturas(context, facturas);
        return f;
    }

    // --- CARGA DE IMAGEN DE ARTICULO ---
    public static Bitmap cargarImagenArticulo(Context context, String nombreImagen) {
        if (nombreImagen == null || nombreImagen.trim().isEmpty()) {
            return null;
        }
        File file = new File(context.getFilesDir(), "articulos_fotos/" + nombreImagen);
        if (file.exists()) {
            try {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            InputStream is = context.getAssets().open("articulos_fotos/" + nombreImagen);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- CARGA Y GUARDADO DE FOTO DE VENDEDOR ---
    public static String guardarImagenVendedor(Context context, Uri imageUri, String codigoVendedor) {
        try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return null;
            File dir = new File(context.getFilesDir(), "vendedores_fotos");
            if (!dir.exists()) dir.mkdirs();
            String safeCod = (codigoVendedor != null ? codigoVendedor : "ven").replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "vendedor_" + safeCod + "_" + System.currentTimeMillis() + ".png";
            File outFile = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
            }
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap cargarImagenVendedor(Context context, String nombreFoto) {
        if (nombreFoto == null || nombreFoto.trim().isEmpty()) {
            return null;
        }
        File file = new File(context.getFilesDir(), "vendedores_fotos/" + nombreFoto);
        if (file.exists()) {
            try {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            InputStream is = context.getAssets().open("vendedores_fotos/" + nombreFoto);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- RESTABLECER DATOS DE ASSETS ---
    public static void restoreFromAssets(Context context) {
        copyAssetToInternal(context, FILE_CONFIG, true);
        copyAssetToInternal(context, FILE_CLIENTES, true);
        copyAssetToInternal(context, FILE_ARTICULOS, true);
        copyAssetToInternal(context, FILE_VENDEDORES, true);
        copyAssetToInternal(context, FILE_FACTURAS, true);
    }

    // --- AUXILIARES LECTURA / ESCRITURA EN MEMORIA DEL TELÉFONO ---
    private static String readFile(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            copyAssetToInternal(context, filename, false);
        }
        if (!file.exists()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void writeFile(Context context, String filename, String content) {
        File file = new File(context.getFilesDir(), filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyAssetToInternal(Context context, String filename, boolean force) {
        File file = new File(context.getFilesDir(), filename);
        if (file.exists() && !force) {
            return;
        }
        try (InputStream is = context.getAssets().open("data/" + filename);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PermisosPerfil getPermisosActivos(Context context) {
        if (isMaster) {
            return new PermisosPerfil(); // Master tiene todo en true
        }
        Configuracion cfg = getConfiguracion(context);
        if ("caja".equalsIgnoreCase(currentUser) || (cfg.usuarioCaja != null && cfg.usuarioCaja.equalsIgnoreCase(currentUser))) {
            return cfg.permisosCaja != null ? cfg.permisosCaja : new PermisosPerfil();
        }
        return cfg.permisosUsuario != null ? cfg.permisosUsuario : new PermisosPerfil();
    }

    // --- GENERAR RECIBO / FACTURA EN TEXTO FORMATEADO ---
    public static String generarTextoFactura(Context context, Factura f) {
        Configuracion cfg = getConfiguracion(context);
        String tipo = (f.tipoDocumento != null && !f.tipoDocumento.isEmpty()) ? f.tipoDocumento : "FACTURA";
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("        ").append(cfg.empresaNombre).append("\n");
        sb.append("     ").append(cfg.eslogan).append("\n");
        sb.append("RIF: ").append(cfg.rif).append("\n");
        sb.append("Teléfono: ").append(cfg.telefono).append("\n");
        sb.append("Email: ").append(cfg.correo).append("\n");
        sb.append("Dirección: ").append(cfg.direccion).append("\n");
        sb.append("========================================\n");
        sb.append("DOCUMENTO: ").append(tipo).append("\n");
        sb.append("NÚMERO N°: ").append(String.format(Locale.US, "%06d", f.numeroFactura)).append("\n");
        sb.append("N° CONTROL: ").append(String.format(Locale.US, "%06d", f.numeroControl)).append("\n");
        sb.append("FECHA: ").append(f.fecha).append("  HORA: ").append(f.hora).append("\n");
        if (f.referenciaExtra != null && !f.referenciaExtra.trim().isEmpty()) {
            sb.append("REF/DETALLE: ").append(f.referenciaExtra.trim()).append("\n");
        }
        sb.append("----------------------------------------\n");
        sb.append("CLIENTE: ").append(f.clienteNombre).append("\n");
        sb.append("RIF/CI: ").append(f.clienteRif).append("\n");
        if (f.clienteDireccion != null && !f.clienteDireccion.isEmpty()) {
            sb.append("DIR: ").append(f.clienteDireccion).append("\n");
        }
        sb.append("VENDEDOR: ").append(f.vendedorNombre).append("\n");
        sb.append("EMITIDO POR: ").append(f.usuarioEmisor).append("\n");
        sb.append("========================================\n");
        sb.append(String.format("%-18s %4s %8s %9s\n", "DESCRIPCIÓN", "CANT", "PRECIO", "TOTAL"));
        sb.append("----------------------------------------\n");

        for (ItemFactura it : f.items) {
            String desc = it.descripcion;
            if (desc.length() > 18) {
                desc = desc.substring(0, 18);
            }
            sb.append(String.format(Locale.US, "%-18s %4d %8.2f %9.2f\n",
                    desc, it.cantidad, it.precioUnitarioBs, it.totalBs));
        }
        sb.append("========================================\n");
        sb.append(String.format(Locale.US, "SUBTOTAL:        Bs. %12.2f\n", f.subtotalBs));
        sb.append(String.format(Locale.US, "IVA:             Bs. %12.2f\n", f.ivaBs));
        sb.append(String.format(Locale.US, "TOTAL A PAGAR:   Bs. %12.2f\n", f.totalBs));
        sb.append("----------------------------------------\n");
        sb.append(String.format(Locale.US, "TOTAL EN DÓLARES:     $ %12.2f\n", f.totalUSD));
        sb.append(String.format(Locale.US, "(Tasa BCV: Bs. %.2f)\n", f.tasaDolar));
        sb.append("========================================\n");
        sb.append("   ¡GRACIAS POR SU COMPRA Y PREFERENCIA!\n");
        sb.append("========================================\n");
        return sb.toString();
    }
}
