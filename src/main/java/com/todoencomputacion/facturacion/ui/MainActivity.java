package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Articulo;
import com.todoencomputacion.facturacion.models.Configuracion;
import com.todoencomputacion.facturacion.models.Factura;
import com.todoencomputacion.facturacion.models.PermisosPerfil;
import com.todoencomputacion.facturacion.models.Vendedor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvMainDateTime, tvUserBadge, tvDrawerUserRole;
    private TextView tvTasaUsdMain, tvTasaEurMain;
    private TextView tvCountArticulos, tvCountClientes, tvCountVendedores, tvCountFacturas;

    private Button btnTabDocumentos, btnTabCatalogos, btnTabReportes;
    private LinearLayout layoutTabDocumentos, layoutTabCatalogos, layoutTabReportes;

    // Tarjetas de Emisión de Documentos
    private CardView cardNuevaFactura, cardDocFacturaTerceros, cardDocNotaCredito,
            cardDocNotaCreditoParcial, cardDocNotaDebito, cardDocOrdenEntrega,
            cardDocOrdenEntregaInd, cardDocGuiaDespacho, cardConfiguracion;

    // Tarjetas de Catálogos
    private CardView cardArticulos, cardClientes, cardVendedores;

    // Tarjetas de Reportes
    private CardView cardFacturas, cardRepVentasGeneral, cardRepVendedores, cardRepInventario, cardRepNotasGuias;

    // Botones del Menú Lateral
    private Button btnNavDocumentos, btnNavCatalogos, btnNavReportes;
    private Button btnNavArticulosDirecto, btnNavClientesDirecto, btnNavVendedoresDirecto,
            btnNavHistorialDirecto, btnNavConfigDirecto, btnNavCerrarSesion;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayoutMain);
        tvMainDateTime = findViewById(R.id.tvMainDateTime);
        tvUserBadge = findViewById(R.id.tvUserBadge);
        tvDrawerUserRole = findViewById(R.id.tvDrawerUserRole);
        tvTasaUsdMain = findViewById(R.id.tvTasaUsdMain);
        tvTasaEurMain = findViewById(R.id.tvTasaEurMain);
        tvCountArticulos = findViewById(R.id.tvCountArticulos);
        tvCountClientes = findViewById(R.id.tvCountClientes);
        tvCountVendedores = findViewById(R.id.tvCountVendedores);
        tvCountFacturas = findViewById(R.id.tvCountFacturas);

        btnTabDocumentos = findViewById(R.id.btnTabDocumentos);
        btnTabCatalogos = findViewById(R.id.btnTabCatalogos);
        btnTabReportes = findViewById(R.id.btnTabReportes);

        layoutTabDocumentos = findViewById(R.id.layoutTabDocumentos);
        layoutTabCatalogos = findViewById(R.id.layoutTabCatalogos);
        layoutTabReportes = findViewById(R.id.layoutTabReportes);

        // Documentos
        cardNuevaFactura = findViewById(R.id.cardNuevaFactura);
        cardDocFacturaTerceros = findViewById(R.id.cardDocFacturaTerceros);
        cardDocNotaCredito = findViewById(R.id.cardDocNotaCredito);
        cardDocNotaCreditoParcial = findViewById(R.id.cardDocNotaCreditoParcial);
        cardDocNotaDebito = findViewById(R.id.cardDocNotaDebito);
        cardDocOrdenEntrega = findViewById(R.id.cardDocOrdenEntrega);
        cardDocOrdenEntregaInd = findViewById(R.id.cardDocOrdenEntregaInd);
        cardDocGuiaDespacho = findViewById(R.id.cardDocGuiaDespacho);
        cardConfiguracion = findViewById(R.id.cardConfiguracion);

        // Catálogos
        cardArticulos = findViewById(R.id.cardArticulos);
        cardClientes = findViewById(R.id.cardClientes);
        cardVendedores = findViewById(R.id.cardVendedores);

        // Reportes
        cardFacturas = findViewById(R.id.cardFacturas);
        cardRepVentasGeneral = findViewById(R.id.cardRepVentasGeneral);
        cardRepVendedores = findViewById(R.id.cardRepVendedores);
        cardRepInventario = findViewById(R.id.cardRepInventario);
        cardRepNotasGuias = findViewById(R.id.cardRepNotasGuias);

        // Menú Lateral
        ImageButton btnMenuLateral = findViewById(R.id.btnMenuLateral);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        btnNavDocumentos = findViewById(R.id.btnNavDocumentos);
        btnNavCatalogos = findViewById(R.id.btnNavCatalogos);
        btnNavReportes = findViewById(R.id.btnNavReportes);
        btnNavArticulosDirecto = findViewById(R.id.btnNavArticulosDirecto);
        btnNavClientesDirecto = findViewById(R.id.btnNavClientesDirecto);
        btnNavVendedoresDirecto = findViewById(R.id.btnNavVendedoresDirecto);
        btnNavHistorialDirecto = findViewById(R.id.btnNavHistorialDirecto);
        btnNavConfigDirecto = findViewById(R.id.btnNavConfigDirecto);
        btnNavCerrarSesion = findViewById(R.id.btnNavCerrarSesion);

        // Mostrar Badge del usuario activo
        if (DataManager.isMaster) {
            tvUserBadge.setText("ROL: MASTER (MENA)");
            tvUserBadge.setTextColor(getResources().getColor(R.color.accent_red));
            tvDrawerUserRole.setText("Perfil Activo: MASTER (Acceso Total)");
        } else {
            String rolUpper = DataManager.currentUser.toUpperCase(Locale.ROOT);
            tvUserBadge.setText("PERFIL: " + rolUpper);
            tvUserBadge.setTextColor(getResources().getColor(R.color.primary));
            tvDrawerUserRole.setText("Perfil Activo: " + rolUpper);
        }

        iniciarReloj();

        // Abrir menú lateral
        btnMenuLateral.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Pestañas superiores
        btnTabDocumentos.setOnClickListener(v -> seleccionarPestana(0));
        btnTabCatalogos.setOnClickListener(v -> seleccionarPestana(1));
        btnTabReportes.setOnClickListener(v -> seleccionarPestana(2));

        // Botones del Menú Lateral
        btnNavDocumentos.setOnClickListener(v -> {
            seleccionarPestana(0);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        btnNavCatalogos.setOnClickListener(v -> {
            seleccionarPestana(1);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        btnNavReportes.setOnClickListener(v -> {
            seleccionarPestana(2);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnNavArticulosDirecto.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ArticulosActivity.class));
        });
        btnNavClientesDirecto.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ClientesActivity.class));
        });
        btnNavVendedoresDirecto.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, VendedoresActivity.class));
        });
        btnNavHistorialDirecto.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, FacturasActivity.class));
        });
        btnNavConfigDirecto.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            if (DataManager.isMaster) {
                startActivity(new Intent(this, ConfiguracionActivity.class));
            } else {
                Toast.makeText(this, "Solo el usuario Master puede acceder a Configuración.", Toast.LENGTH_SHORT).show();
            }
        });
        btnNavCerrarSesion.setOnClickListener(v -> cerrarSesion());
        btnLogout.setOnClickListener(v -> cerrarSesion());

        // Acciones de Emisión de Documentos
        cardNuevaFactura.setOnClickListener(v -> abrirEmisionDocumento("FACTURA"));
        cardDocFacturaTerceros.setOnClickListener(v -> abrirEmisionDocumento("FACTURA A TERCEROS"));
        cardDocNotaCredito.setOnClickListener(v -> abrirEmisionDocumento("NOTA DE CRÉDITO"));
        cardDocNotaCreditoParcial.setOnClickListener(v -> abrirEmisionDocumento("NOTA DE CRÉDITO PARCIAL"));
        cardDocNotaDebito.setOnClickListener(v -> abrirEmisionDocumento("NOTA DE DÉBITO"));
        cardDocOrdenEntrega.setOnClickListener(v -> abrirEmisionDocumento("ORDEN DE ENTREGA"));
        cardDocOrdenEntregaInd.setOnClickListener(v -> abrirEmisionDocumento("ORDEN DE ENTREGA INDIVIDUAL"));
        cardDocGuiaDespacho.setOnClickListener(v -> abrirEmisionDocumento("GUÍA DE DESPACHO"));

        // Acciones de Catálogos
        cardArticulos.setOnClickListener(v -> startActivity(new Intent(this, ArticulosActivity.class)));
        cardClientes.setOnClickListener(v -> startActivity(new Intent(this, ClientesActivity.class)));
        cardVendedores.setOnClickListener(v -> startActivity(new Intent(this, VendedoresActivity.class)));

        // Acciones de Reportes
        cardFacturas.setOnClickListener(v -> startActivity(new Intent(this, FacturasActivity.class)));
        cardRepVentasGeneral.setOnClickListener(v -> mostrarReporteGeneralVentas());
        cardRepVendedores.setOnClickListener(v -> mostrarReporteVendedores());
        cardRepInventario.setOnClickListener(v -> mostrarReporteInventario());
        cardRepNotasGuias.setOnClickListener(v -> mostrarReporteNotasYGuias());

        // Configuración (Master)
        cardConfiguracion.setOnClickListener(v -> {
            if (DataManager.isMaster) {
                startActivity(new Intent(this, ConfiguracionActivity.class));
            } else {
                Toast.makeText(this, "Acceso restringido. Solo el usuario Master (mena) puede configurar el sistema.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirEmisionDocumento(String tipoDoc) {
        Intent intent = new Intent(this, NuevaFacturaActivity.class);
        intent.putExtra(NuevaFacturaActivity.EXTRA_TIPO_DOCUMENTO, tipoDoc);
        startActivity(intent);
    }

    private void seleccionarPestana(int index) {
        layoutTabDocumentos.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutTabCatalogos.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutTabReportes.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        btnTabDocumentos.setBackgroundResource(index == 0 ? R.drawable.button_primary : R.drawable.button_secondary);
        btnTabDocumentos.setTextColor(getResources().getColor(index == 0 ? R.color.white : R.color.primary));

        btnTabCatalogos.setBackgroundResource(index == 1 ? R.drawable.button_primary : R.drawable.button_secondary);
        btnTabCatalogos.setTextColor(getResources().getColor(index == 1 ? R.color.white : R.color.primary));

        btnTabReportes.setBackgroundResource(index == 2 ? R.drawable.button_primary : R.drawable.button_secondary);
        btnTabReportes.setTextColor(getResources().getColor(index == 2 ? R.color.white : R.color.primary));
    }

    private void aplicarPermisosPerfil() {
        PermisosPerfil p = DataManager.getPermisosActivos(this);

        // Documentos
        cardNuevaFactura.setVisibility(p.factura ? View.VISIBLE : View.GONE);
        cardDocFacturaTerceros.setVisibility(p.facturaTerceros ? View.VISIBLE : View.GONE);
        cardDocNotaCredito.setVisibility(p.notaCredito ? View.VISIBLE : View.GONE);
        cardDocNotaCreditoParcial.setVisibility(p.notaCreditoParcial ? View.VISIBLE : View.GONE);
        cardDocNotaDebito.setVisibility(p.notaDebito ? View.VISIBLE : View.GONE);
        cardDocOrdenEntrega.setVisibility(p.ordenEntrega ? View.VISIBLE : View.GONE);
        cardDocOrdenEntregaInd.setVisibility(p.ordenEntregaIndividual ? View.VISIBLE : View.GONE);
        cardDocGuiaDespacho.setVisibility(p.guiaDespacho ? View.VISIBLE : View.GONE);

        // Configuración solo visible para Master
        cardConfiguracion.setVisibility(DataManager.isMaster ? View.VISIBLE : View.GONE);
        btnNavConfigDirecto.setVisibility(DataManager.isMaster ? View.VISIBLE : View.GONE);

        // Catálogos
        cardArticulos.setVisibility(p.articulos ? View.VISIBLE : View.GONE);
        cardClientes.setVisibility(p.clientes ? View.VISIBLE : View.GONE);
        cardVendedores.setVisibility(p.vendedores ? View.VISIBLE : View.GONE);

        btnNavArticulosDirecto.setVisibility(p.articulos ? View.VISIBLE : View.GONE);
        btnNavClientesDirecto.setVisibility(p.clientes ? View.VISIBLE : View.GONE);
        btnNavVendedoresDirecto.setVisibility(p.vendedores ? View.VISIBLE : View.GONE);

        boolean algunCatalogo = p.articulos || p.clientes || p.vendedores;
        btnTabCatalogos.setVisibility(algunCatalogo ? View.VISIBLE : View.GONE);
        btnNavCatalogos.setVisibility(algunCatalogo ? View.VISIBLE : View.GONE);

        // Reportes
        cardFacturas.setVisibility(p.repHistorial ? View.VISIBLE : View.GONE);
        cardRepVentasGeneral.setVisibility(p.repVentasGeneral ? View.VISIBLE : View.GONE);
        cardRepVendedores.setVisibility(p.repVendedores ? View.VISIBLE : View.GONE);
        cardRepInventario.setVisibility(p.repInventario ? View.VISIBLE : View.GONE);
        cardRepNotasGuias.setVisibility(p.repNotasGuias ? View.VISIBLE : View.GONE);

        btnNavHistorialDirecto.setVisibility(p.repHistorial ? View.VISIBLE : View.GONE);

        boolean algunReporte = p.repHistorial || p.repVentasGeneral || p.repVendedores || p.repInventario || p.repNotasGuias;
        btnTabReportes.setVisibility(algunReporte ? View.VISIBLE : View.GONE);
        btnNavReportes.setVisibility(algunReporte ? View.VISIBLE : View.GONE);
    }

    private void mostrarReporteGeneralVentas() {
        List<Factura> list = DataManager.getFacturas(this);
        Configuracion cfg = DataManager.getConfiguracion(this);
        double subBs = 0, ivaBs = 0, totBs = 0, totUsd = 0;
        for (Factura f : list) {
            subBs += f.subtotalBs;
            ivaBs += f.ivaBs;
            totBs += f.totalBs;
            totUsd += f.totalUSD;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("   REPORTE GENERAL DE VENTAS E IVA\n");
        sb.append("   ").append(cfg.empresaNombre).append("\n");
        sb.append("========================================\n");
        sb.append("Documentos Procesados: ").append(list.size()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format(Locale.US, "Subtotal Base:   Bs. %14.2f\n", subBs));
        sb.append(String.format(Locale.US, "Total IVA:       Bs. %14.2f\n", ivaBs));
        sb.append(String.format(Locale.US, "TOTAL EN BS.:    Bs. %14.2f\n", totBs));
        sb.append("----------------------------------------\n");
        sb.append(String.format(Locale.US, "TOTAL DIVISAS:     $ %14.2f\n", totUsd));
        sb.append(String.format(Locale.US, "Tasa BCV Actual: Bs. %14.2f\n", cfg.tasaDolar));
        sb.append("========================================\n");

        mostrarDialogoReporte("Reporte General de Ventas", sb.toString());
    }

    private void mostrarReporteVendedores() {
        List<Vendedor> vendedores = DataManager.getVendedores(this);
        List<Factura> facturas = DataManager.getFacturas(this);
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  REPORTE DE VENTAS Y COMISIONES\n");
        sb.append("========================================\n");
        for (Vendedor v : vendedores) {
            int count = 0;
            double totalUsd = 0;
            double totalBs = 0;
            for (Factura f : facturas) {
                if (v.codigo.equalsIgnoreCase(f.vendedorCodigo) || v.nombre.equalsIgnoreCase(f.vendedorNombre)) {
                    count++;
                    totalUsd += f.totalUSD;
                    totalBs += f.totalBs;
                }
            }
            double comisionUsd = totalUsd * (v.comision / 100.0);
            sb.append("Vendedor: ").append(v.nombre).append("\n");
            sb.append("Documentos: ").append(count).append(" | Comisión: ").append(v.comision).append("%\n");
            sb.append(String.format(Locale.US, "Ventas Bs.:   Bs. %,.2f\n", totalBs));
            sb.append(String.format(Locale.US, "Ventas USD:     $ %,.2f\n", totalUsd));
            sb.append(String.format(Locale.US, "Comisión ($):   $ %,.2f\n", comisionUsd));
            sb.append("----------------------------------------\n");
        }
        mostrarDialogoReporte("Reporte por Vendedor", sb.toString());
    }

    private void mostrarReporteInventario() {
        List<Articulo> articulos = DataManager.getArticulos(this);
        Configuracion cfg = DataManager.getConfiguracion(this);
        int totalUnidades = 0;
        double valorTotalUsd = 0;
        double valorTotalBs = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  REPORTE DE INVENTARIO VALORIZADO\n");
        sb.append("========================================\n");
        for (Articulo a : articulos) {
            totalUnidades += a.stock;
            double valUsd = a.stock * a.precioUSD;
            double valBs = valUsd * cfg.tasaDolar;
            valorTotalUsd += valUsd;
            valorTotalBs += valBs;

            sb.append(a.codigo).append(" - ").append(a.descripcion).append("\n");
            sb.append(String.format(Locale.US, "Stock: %d | Unit: $%.2f | Total: $%,.2f\n",
                    a.stock, a.precioUSD, valUsd));
            sb.append("----------------------------------------\n");
        }
        sb.append("TOTAL UNIDADES EN STOCK: ").append(totalUnidades).append("\n");
        sb.append(String.format(Locale.US, "VALORIZACIÓN USD:  $ %,.2f\n", valorTotalUsd));
        sb.append(String.format(Locale.US, "VALORIZACIÓN BS.: Bs. %,.2f\n", valorTotalBs));
        sb.append("========================================\n");

        mostrarDialogoReporte("Reporte de Inventario", sb.toString());
    }

    private void mostrarReporteNotasYGuias() {
        List<Factura> facturas = DataManager.getFacturas(this);
        String[] tipos = new String[]{
                "FACTURA", "FACTURA A TERCEROS", "NOTA DE CRÉDITO", "NOTA DE CRÉDITO PARCIAL",
                "NOTA DE DÉBITO", "ORDEN DE ENTREGA", "ORDEN DE ENTREGA INDIVIDUAL", "GUÍA DE DESPACHO"
        };
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("   RESUMEN POR TIPO DE DOCUMENTO\n");
        sb.append("========================================\n");
        for (String t : tipos) {
            int c = 0;
            double sumUsd = 0;
            double sumBs = 0;
            for (Factura f : facturas) {
                String ft = (f.tipoDocumento != null && !f.tipoDocumento.isEmpty()) ? f.tipoDocumento : "FACTURA";
                if (t.equalsIgnoreCase(ft)) {
                    c++;
                    sumUsd += f.totalUSD;
                    sumBs += f.totalBs;
                }
            }
            sb.append("• ").append(t).append(": ").append(c).append(" doc(s)\n");
            sb.append(String.format(Locale.US, "  Total: Bs. %,.2f  ($ %,.2f)\n", sumBs, sumUsd));
            sb.append("----------------------------------------\n");
        }
        mostrarDialogoReporte("Resumen por Tipo de Documento", sb.toString());
    }

    private void mostrarDialogoReporte(String titulo, String contenido) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_detalle_factura);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvRecibo = dialog.findViewById(R.id.tvFacturaReciboTexto);
        Button btnCompartir = dialog.findViewById(R.id.btnCompartirFactura);
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDetalle);

        tvRecibo.setText(contenido);

        btnCompartir.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, titulo);
            intent.putExtra(Intent.EXTRA_TEXT, contenido);
            startActivity(Intent.createChooser(intent, "Compartir Reporte por:"));
        });

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void cerrarSesion() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aplicarPermisosPerfil();
        actualizarContadores();
        actualizarTasasVista();
        DataManager.actualizarTasaDesdeBCV(this, nuevaTasa -> actualizarTasasVista());
    }

    private void actualizarTasasVista() {
        Configuracion cfg = DataManager.getConfiguracion(this);
        if (tvTasaUsdMain != null) {
            tvTasaUsdMain.setText(String.format(new Locale("es", "VE"), "Bs. %,.2f", cfg.tasaDolar));
        }
        if (tvTasaEurMain != null) {
            double eur = cfg.tasaEuro > 0 ? cfg.tasaEuro : 1006.61;
            tvTasaEurMain.setText(String.format(new Locale("es", "VE"), "Bs. %,.2f", eur));
        }
    }

    private void actualizarContadores() {
        int arts = DataManager.getArticulos(this).size();
        int clis = DataManager.getClientes(this).size();
        int vens = DataManager.getVendedores(this).size();
        int facts = DataManager.getFacturas(this).size();

        tvCountArticulos.setText(arts + " artículos registrados con foto");
        tvCountClientes.setText(clis + " clientes en base de datos");
        tvCountVendedores.setText(vens + " vendedores activos");
        tvCountFacturas.setText(facts + " documentos emitidos en el teléfono");
    }

    private void iniciarReloj() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy - hh:mm:ss a", new Locale("es", "VE"));
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String txt = sdf.format(new Date());
                if (!txt.isEmpty()) {
                    txt = Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
                }
                tvMainDateTime.setText(txt);
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
