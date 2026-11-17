package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.adapters.ArticuloAdapter;
import com.todoencomputacion.facturacion.adapters.ItemFacturaAdapter;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.data.FacturaPdfGenerator;
import com.todoencomputacion.facturacion.models.Articulo;
import com.todoencomputacion.facturacion.models.Cliente;
import com.todoencomputacion.facturacion.models.Configuracion;
import com.todoencomputacion.facturacion.models.Factura;
import com.todoencomputacion.facturacion.models.ItemFactura;
import com.todoencomputacion.facturacion.models.Vendedor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NuevaFacturaActivity extends AppCompatActivity {

    public static final String EXTRA_TIPO_DOCUMENTO = "EXTRA_TIPO_DOCUMENTO";

    private String tipoDocumento = "FACTURA";
    private TextView tvTituloTipoDoc, tvFacturaCorrelativo, tvFacturaReloj, tvLabelRefExtra;
    private EditText etReferenciaExtra;
    private Spinner spnClientes, spnVendedores;
    private RecyclerView rvItemsFactura;
    private TextView tvEmptyItems, tvSubtotalBs, tvIvaLabel, tvIvaBs, tvTotalBs, tvTasaInfo, tvTotalUSD;
    private Button btnAgregarItem, btnAgregarServicioLibre, btnProcesarFactura;

    private List<Cliente> clientesList;
    private List<Vendedor> vendedoresList;
    private List<Articulo> articulosList;
    private final List<ItemFactura> itemsFactura = new ArrayList<>();
    private ItemFacturaAdapter itemAdapter;
    private Configuracion config;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private double totalSubtotalBs = 0;
    private double totalExentoBs = 0;
    private double totalIvaBs = 0;
    private double totalGeneralBs = 0;
    private double totalGeneralUSD = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_factura);

        String extraTipo = getIntent().getStringExtra(EXTRA_TIPO_DOCUMENTO);
        if (extraTipo != null && !extraTipo.trim().isEmpty()) {
            tipoDocumento = extraTipo.trim().toUpperCase(Locale.ROOT);
        }

        config = DataManager.getConfiguracion(this);
        clientesList = DataManager.getClientes(this);
        vendedoresList = DataManager.getVendedores(this);
        articulosList = DataManager.getArticulos(this);

        initViews();
        iniciarReloj();
        setupSpinners();
        setupRecyclerView();
        calcularTotales();

        // Sincronizar tasa automáticamente desde https://www.bcv.org.ve/
        DataManager.actualizarTasaDesdeBCV(this, nuevaTasa -> {
            config.tasaDolar = nuevaTasa;
            articulosList = DataManager.getArticulos(this);
            tvTasaInfo.setText(String.format(Locale.US, "Ref. Dólares (Tasa: Bs. %.2f):", config.tasaDolar));
            for (ItemFactura it : itemsFactura) {
                it.precioUnitarioBs = Math.round(it.precioUnitarioUSD * config.tasaDolar * 100.0) / 100.0;
                it.totalBs = it.cantidad * it.precioUnitarioBs;
            }
            if (itemAdapter != null) {
                itemAdapter.notifyDataSetChanged();
            }
            calcularTotales();
        });

        btnAgregarItem.setOnClickListener(v -> mostrarDialogoSeleccionarArticulo());
        if (btnAgregarServicioLibre != null) {
            btnAgregarServicioLibre.setOnClickListener(v -> {
                Articulo gen = buscarArticuloPorCodigo(Articulo.CODIGO_GENERICO);
                if (gen == null) {
                    gen = new Articulo(Articulo.CODIGO_GENERICO, "SERVICIO GENÉRICO (Descripción y Precio Libre)", 0.0, 0.0, 0, "licencia.png", "S", "16%", 16.0);
                    gen.esGenerico = true;
                }
                pedirCantidadYPrecioArticulo(gen, -1);
            });
        }
        btnProcesarFactura.setOnClickListener(v -> emitirFactura());
    }

    @Override
    protected void onResume() {
        super.onResume();
        config = DataManager.getConfiguracion(this);
        if (itemAdapter != null) {
            itemAdapter.setPermitirEditarPrecio(config.permitirEditarPrecio);
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvTituloTipoDoc = findViewById(R.id.tvTituloTipoDoc);
        tvFacturaCorrelativo = findViewById(R.id.tvFacturaCorrelativo);
        tvFacturaReloj = findViewById(R.id.tvFacturaReloj);
        tvLabelRefExtra = findViewById(R.id.tvLabelRefExtra);
        etReferenciaExtra = findViewById(R.id.etReferenciaExtra);
        spnClientes = findViewById(R.id.spnClientes);
        spnVendedores = findViewById(R.id.spnVendedores);
        rvItemsFactura = findViewById(R.id.rvItemsFactura);
        tvEmptyItems = findViewById(R.id.tvEmptyItems);
        tvSubtotalBs = findViewById(R.id.tvSubtotalBs);
        tvIvaLabel = findViewById(R.id.tvIvaLabel);
        tvIvaBs = findViewById(R.id.tvIvaBs);
        tvTotalBs = findViewById(R.id.tvTotalBs);
        tvTasaInfo = findViewById(R.id.tvTasaInfo);
        tvTotalUSD = findViewById(R.id.tvTotalUSD);
        btnAgregarItem = findViewById(R.id.btnAgregarItem);
        btnAgregarServicioLibre = findViewById(R.id.btnAgregarServicioLibre);
        btnProcesarFactura = findViewById(R.id.btnProcesarFactura);

        if (tvTituloTipoDoc != null) {
            tvTituloTipoDoc.setText(tipoDocumento);
        }
        btnProcesarFactura.setText("EMITIR " + tipoDocumento);

        if (tipoDocumento.contains("TERCEROS")) {
            tvLabelRefExtra.setText("Datos del Tercero / Mandante (RIF y Razón Social):");
            etReferenciaExtra.setHint("Ej: J-12345678-9 Comercializadora XYZ");
        } else if (tipoDocumento.contains("NOTA")) {
            tvLabelRefExtra.setText("Factura Afectada N° y Motivo:");
            etReferenciaExtra.setHint("Ej: Factura #001001 - Ajuste / Devolución");
        } else if (tipoDocumento.contains("ORDEN") || tipoDocumento.contains("GUÍA") || tipoDocumento.contains("GUIA")) {
            tvLabelRefExtra.setText("Datos de Transporte / Placa / Destino:");
            etReferenciaExtra.setHint("Ej: Chofer, Placa, Dirección de Entrega");
        } else {
            tvLabelRefExtra.setText("Observación / Referencia (Opcional):");
            etReferenciaExtra.setHint("Observación adicional del documento");
        }

        actualizarCorrelativosVista();
        tvIvaLabel.setText("IVA (16% / 8% / 32% / Exento):");
        tvTasaInfo.setText(String.format(Locale.US, "Ref. Dólares (Tasa: Bs. %.2f):", config.tasaDolar));
    }

    private void actualizarCorrelativosVista() {
        tvFacturaCorrelativo.setText(String.format(Locale.US, "%s #%06d | Control #%06d",
                tipoDocumento, config.proximoNumeroFactura, config.proximoNumeroControl));
    }

    private void iniciarReloj() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy hh:mm:ss a", new Locale("es", "VE"));
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String txt = sdf.format(new Date());
                if (!txt.isEmpty()) {
                    txt = Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
                }
                tvFacturaReloj.setText(txt);
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

    private void setupSpinners() {
        ArrayAdapter<Cliente> clienteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clientesList);
        spnClientes.setAdapter(clienteAdapter);

        ArrayAdapter<Vendedor> vendedorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vendedoresList);
        spnVendedores.setAdapter(vendedorAdapter);
    }

    private Articulo buscarArticuloPorCodigo(String codigo) {
        if (codigo == null) return null;
        for (Articulo a : articulosList) {
            if (codigo.equalsIgnoreCase(a.codigo)) {
                return a;
            }
        }
        return null;
    }

    private void setupRecyclerView() {
        itemAdapter = new ItemFacturaAdapter(this, itemsFactura, config.permitirEditarPrecio, new ItemFacturaAdapter.OnItemActionListener() {
            @Override
            public void onIncrease(int position) {
                if (position < 0 || position >= itemsFactura.size()) return;
                ItemFactura it = itemsFactura.get(position);
                Articulo art = buscarArticuloPorCodigo(it.codigoArticulo);
                if (art != null && art.manejaStock() && !config.permitirStockNegativo) {
                    if (it.cantidad + 1 > art.stock) {
                        Toast.makeText(NuevaFacturaActivity.this,
                                "Stock insuficiente. Disponible: " + art.stock + " unidades.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                it.cantidad++;
                it.totalBs = it.cantidad * it.precioUnitarioBs;
                it.totalUSD = it.cantidad * it.precioUnitarioUSD;
                itemAdapter.notifyItemChanged(position);
                calcularTotales();
            }

            @Override
            public void onDecrease(int position) {
                if (position < 0 || position >= itemsFactura.size()) return;
                ItemFactura it = itemsFactura.get(position);
                if (it.cantidad > 1) {
                    it.cantidad--;
                    it.totalBs = it.cantidad * it.precioUnitarioBs;
                    it.totalUSD = it.cantidad * it.precioUnitarioUSD;
                    itemAdapter.notifyItemChanged(position);
                } else {
                    itemsFactura.remove(position);
                    itemAdapter.notifyItemRemoved(position);
                }
                calcularTotales();
            }

            @Override
            public void onEditPrice(int position) {
                if (position < 0 || position >= itemsFactura.size()) return;
                ItemFactura it = itemsFactura.get(position);
                Articulo art = buscarArticuloPorCodigo(it.codigoArticulo);
                if (art == null) {
                    art = new Articulo(it.codigoArticulo, it.descripcion, it.precioUnitarioUSD, it.precioUnitarioBs, 0, "licencia.png", it.tipoArticulo, it.tipoIva, it.porcentajeIva);
                }
                pedirCantidadYPrecioArticulo(art, position);
            }

            @Override
            public void onRemove(int position) {
                if (position < 0 || position >= itemsFactura.size()) return;
                itemsFactura.remove(position);
                itemAdapter.notifyItemRemoved(position);
                calcularTotales();
            }
        });
        rvItemsFactura.setLayoutManager(new LinearLayoutManager(this));
        rvItemsFactura.setAdapter(itemAdapter);
    }

    private void calcularTotales() {
        totalSubtotalBs = 0;
        totalExentoBs = 0;
        totalIvaBs = 0;
        double subtotalUSD = 0;

        for (ItemFactura it : itemsFactura) {
            totalSubtotalBs += it.totalBs;
            subtotalUSD += it.totalUSD;
            if ("EXENTO".equalsIgnoreCase(it.tipoIva) || it.porcentajeIva <= 0) {
                totalExentoBs += it.totalBs;
            } else {
                totalIvaBs += it.totalBs * (it.porcentajeIva / 100.0);
            }
        }

        totalGeneralBs = totalSubtotalBs + totalIvaBs;
        totalGeneralUSD = config.tasaDolar > 0 ? (totalGeneralBs / config.tasaDolar) : subtotalUSD;

        tvSubtotalBs.setText(String.format(Locale.US, "Bs. %,.2f", totalSubtotalBs));
        tvIvaBs.setText(String.format(Locale.US, "Bs. %,.2f", totalIvaBs));
        tvTotalBs.setText(String.format(Locale.US, "Bs. %,.2f", totalGeneralBs));
        tvTotalUSD.setText(String.format(Locale.US, "$ %,.2f", totalGeneralUSD));

        if (itemsFactura.isEmpty()) {
            tvEmptyItems.setVisibility(View.VISIBLE);
            rvItemsFactura.setVisibility(View.GONE);
        } else {
            tvEmptyItems.setVisibility(View.GONE);
            rvItemsFactura.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDialogoSeleccionarArticulo() {
        config = DataManager.getConfiguracion(this);
        articulosList = DataManager.getArticulos(this);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_seleccionar_articulo);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etBuscar = dialog.findViewById(R.id.etBuscarDialogArt);
        RecyclerView rvDialog = dialog.findViewById(R.id.rvDialogArticulos);
        Button btnCancelar = dialog.findViewById(R.id.btnCerrarDialogArt);

        List<Articulo> filtrados = new ArrayList<>(articulosList);
        ArticuloAdapter adapter = new ArticuloAdapter(this, filtrados, articulo -> {
            if (articulo.manejaStock() && !config.permitirStockNegativo && articulo.stock <= 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Sin Stock Disponible")
                        .setMessage("El artículo \"" + articulo.descripcion + "\" es Tipo Venta (V) y no tiene stock disponible (Stock actual: " + articulo.stock + ").\n\nSi desea facturar sin stock, active la opción 'Vender con Stock Negativo' en Configuración.")
                        .setPositiveButton("Entendido", null)
                        .show();
                return;
            }
            dialog.dismiss();
            pedirCantidadYPrecioArticulo(articulo, -1);
        });
        rvDialog.setLayoutManager(new LinearLayoutManager(this));
        rvDialog.setAdapter(adapter);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().toLowerCase().trim();
                filtrados.clear();
                for (Articulo a : articulosList) {
                    if (a.descripcion.toLowerCase().contains(q) || a.codigo.toLowerCase().contains(q)) {
                        filtrados.add(a);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Muestra el diálogo para indicar Cantidad y (si está permitido o es Servicio Genérico) el Precio de Venta al lado.
     * @param a Artículo seleccionado
     * @param editPosition Si >= 0, edita la línea existente en itemsFactura; si -1, agrega a la factura.
     */
    private void pedirCantidadYPrecioArticulo(Articulo a, int editPosition) {
        config = DataManager.getConfiguracion(this);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_cantidad);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvNombre = dialog.findViewById(R.id.tvDialogArtNombre);
        TextView tvPrecios = dialog.findViewById(R.id.tvDialogArtPrecios);
        LinearLayout layoutGenerico = dialog.findViewById(R.id.layoutCamposGenerico);
        EditText etDescLibre = dialog.findViewById(R.id.etDialogDescLibre);
        RadioGroup rgIvaGen = dialog.findViewById(R.id.rgDialogIvaGenerico);
        RadioButton rbGen16 = dialog.findViewById(R.id.rbGenIva16);
        RadioButton rbGen8 = dialog.findViewById(R.id.rbGenIva8);
        RadioButton rbGen32 = dialog.findViewById(R.id.rbGenIva32);
        RadioButton rbGenExento = dialog.findViewById(R.id.rbGenIvaExento);

        EditText etCantidad = dialog.findViewById(R.id.etDialogCantidad);
        EditText etPrecioUSD = dialog.findViewById(R.id.etDialogPrecioVentaUSD);
        EditText etPrecioBs = dialog.findViewById(R.id.etDialogPrecioVentaBs);
        TextView tvAvisoPrecio = dialog.findViewById(R.id.tvAvisoEdicionPrecio);
        Button btnConfirmar = dialog.findViewById(R.id.btnDialogConfirmarAgregar);
        Button btnCancelar = dialog.findViewById(R.id.btnDialogCancelarCantidad);

        boolean esGenerico = a.esArticuloGenerico();
        boolean puedeEditarPrecio = esGenerico || config.permitirEditarPrecio;

        ItemFactura itemExistente = (editPosition >= 0 && editPosition < itemsFactura.size())
                ? itemsFactura.get(editPosition) : null;

        if (esGenerico) {
            tvNombre.setText("SERVICIO GENÉRICO (Descripción y Precio Libre)");
            tvPrecios.setText(String.format(Locale.US, "Tipo Servicio (Sin Stock) | Tasa BCV: Bs. %.2f", config.tasaDolar));
            layoutGenerico.setVisibility(View.VISIBLE);
            if (itemExistente != null) {
                etDescLibre.setText(itemExistente.descripcion);
                if ("EXENTO".equalsIgnoreCase(itemExistente.tipoIva) || itemExistente.porcentajeIva <= 0) {
                    rbGenExento.setChecked(true);
                } else if (Math.abs(itemExistente.porcentajeIva - 8.0) < 0.01) {
                    rbGen8.setChecked(true);
                } else if (Math.abs(itemExistente.porcentajeIva - 32.0) < 0.01) {
                    rbGen32.setChecked(true);
                } else {
                    rbGen16.setChecked(true);
                }
            }
        } else {
            tvNombre.setText(a.descripcion);
            String stockInfo = a.manejaStock() ? ("Stock: " + a.stock) : "Servicio (Sin Stock)";
            tvPrecios.setText(String.format(Locale.US, "Catálogo: Bs. %,.2f ($ %,.2f) | %s | %s",
                    a.precioBs, a.precioUSD, stockInfo, a.getEtiquetaIva()));
            layoutGenerico.setVisibility(View.GONE);
        }

        double initUSD = itemExistente != null ? itemExistente.precioUnitarioUSD : a.precioUSD;
        double initBs = itemExistente != null ? itemExistente.precioUnitarioBs : a.precioBs;
        int initCant = itemExistente != null ? itemExistente.cantidad : 1;

        etCantidad.setText(String.valueOf(initCant));
        if (initUSD > 0 || !esGenerico) {
            etPrecioUSD.setText(String.format(Locale.US, "%.2f", initUSD));
            etPrecioBs.setText(String.format(Locale.US, "%.2f", initBs));
        } else {
            etPrecioUSD.setText("");
            etPrecioBs.setText("");
        }

        etPrecioUSD.setEnabled(puedeEditarPrecio);
        etPrecioBs.setEnabled(puedeEditarPrecio);

        if (puedeEditarPrecio) {
            tvAvisoPrecio.setText("✓ Edición de precio de venta habilitada (cambie $ o Bs. al lado de Cantidad)");
            tvAvisoPrecio.setTextColor(getResources().getColor(R.color.accent_green));
        } else {
            tvAvisoPrecio.setText("🔒 Precio de venta fijo (Para editar, active 'Permitir editar precio: SÍ' en Configuración)");
            tvAvisoPrecio.setTextColor(getResources().getColor(R.color.text_muted));
        }

        if (editPosition >= 0) {
            btnConfirmar.setText("ACTUALIZAR LÍNEA");
        }

        // Sincronizar conversión entre Precio USD y Precio Bs. mientras el usuario escribe
        final boolean[] actualizandoTexto = {false};
        etPrecioUSD.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (actualizandoTexto[0] || !etPrecioUSD.hasFocus()) return;
                actualizandoTexto[0] = true;
                try {
                    String val = s.toString().trim().replace(",", ".");
                    if (!val.isEmpty()) {
                        double usd = Double.parseDouble(val);
                        double bs = Math.round(usd * config.tasaDolar * 100.0) / 100.0;
                        etPrecioBs.setText(String.format(Locale.US, "%.2f", bs));
                    } else {
                        etPrecioBs.setText("");
                    }
                } catch (Exception ignored) {}
                actualizandoTexto[0] = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPrecioBs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (actualizandoTexto[0] || !etPrecioBs.hasFocus()) return;
                actualizandoTexto[0] = true;
                try {
                    String val = s.toString().trim().replace(",", ".");
                    if (!val.isEmpty() && config.tasaDolar > 0) {
                        double bs = Double.parseDouble(val);
                        double usd = Math.round((bs / config.tasaDolar) * 100.0) / 100.0;
                        etPrecioUSD.setText(String.format(Locale.US, "%.2f", usd));
                    } else {
                        etPrecioUSD.setText("");
                    }
                } catch (Exception ignored) {}
                actualizandoTexto[0] = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnConfirmar.setOnClickListener(v -> {
            String cantStr = etCantidad.getText().toString().trim();
            if (cantStr.isEmpty()) {
                Toast.makeText(this, "Indique la cantidad", Toast.LENGTH_SHORT).show();
                return;
            }
            int cant = Integer.parseInt(cantStr);
            if (cant <= 0) {
                Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }

            String descFinal = a.descripcion;
            String tipoIvaFinal = a.tipoIva;
            double porcIvaFinal = a.porcentajeIva;

            if (esGenerico) {
                descFinal = etDescLibre.getText().toString().trim();
                if (descFinal.isEmpty()) {
                    Toast.makeText(this, "Escriba la descripción del servicio a vender", Toast.LENGTH_SHORT).show();
                    etDescLibre.requestFocus();
                    return;
                }
                int checkedIva = rgIvaGen.getCheckedRadioButtonId();
                if (checkedIva == R.id.rbGenIva8) {
                    tipoIvaFinal = "8%";
                    porcIvaFinal = 8.0;
                } else if (checkedIva == R.id.rbGenIva32) {
                    tipoIvaFinal = "32%";
                    porcIvaFinal = 32.0;
                } else if (checkedIva == R.id.rbGenIvaExento) {
                    tipoIvaFinal = "EXENTO";
                    porcIvaFinal = 0.0;
                } else {
                    tipoIvaFinal = "16%";
                    porcIvaFinal = 16.0;
                }
            }

            double precioUSD = a.precioUSD;
            double precioBs = a.precioBs;

            if (puedeEditarPrecio) {
                String pUsdStr = etPrecioUSD.getText().toString().trim().replace(",", ".");
                String pBsStr = etPrecioBs.getText().toString().trim().replace(",", ".");
                if (pUsdStr.isEmpty() && pBsStr.isEmpty()) {
                    Toast.makeText(this, "Indique el precio de venta", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (!pBsStr.isEmpty() && etPrecioBs.hasFocus()) {
                        precioBs = Double.parseDouble(pBsStr);
                        precioUSD = config.tasaDolar > 0 ? (Math.round((precioBs / config.tasaDolar) * 100.0) / 100.0) : 0;
                    } else if (!pUsdStr.isEmpty()) {
                        precioUSD = Double.parseDouble(pUsdStr);
                        precioBs = Math.round(precioUSD * config.tasaDolar * 100.0) / 100.0;
                    } else {
                        precioBs = Double.parseDouble(pBsStr);
                        precioUSD = config.tasaDolar > 0 ? (Math.round((precioBs / config.tasaDolar) * 100.0) / 100.0) : 0;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (precioBs <= 0 && precioUSD <= 0) {
                    Toast.makeText(this, "El precio de venta debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Si estamos editando una línea existente en la factura
            if (itemExistente != null) {
                if (a.manejaStock() && !config.permitirStockNegativo && cant > a.stock) {
                    Toast.makeText(this, "No se puede superar el stock disponible (" + a.stock + " uds).", Toast.LENGTH_LONG).show();
                    return;
                }
                itemExistente.descripcion = descFinal;
                itemExistente.cantidad = cant;
                itemExistente.precioUnitarioUSD = precioUSD;
                itemExistente.precioUnitarioBs = precioBs;
                itemExistente.totalUSD = cant * precioUSD;
                itemExistente.totalBs = cant * precioBs;
                itemExistente.tipoIva = tipoIvaFinal;
                itemExistente.porcentajeIva = porcIvaFinal;

                itemAdapter.notifyDataSetChanged();
                calcularTotales();
                dialog.dismiss();
                return;
            }

            // Si es un artículo normal (no genérico), verificar stock acumulado
            if (!esGenerico) {
                int cantidadYaEnFactura = 0;
                for (ItemFactura it : itemsFactura) {
                    if (it.codigoArticulo.equalsIgnoreCase(a.codigo)) {
                        cantidadYaEnFactura = it.cantidad;
                        break;
                    }
                }

                if (a.manejaStock() && !config.permitirStockNegativo) {
                    if (cantidadYaEnFactura + cant > a.stock) {
                        Toast.makeText(this,
                                "No se puede superar el stock disponible (" + a.stock + " uds).",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                boolean encontrado = false;
                for (ItemFactura it : itemsFactura) {
                    if (it.codigoArticulo.equalsIgnoreCase(a.codigo)) {
                        it.cantidad += cant;
                        it.precioUnitarioUSD = precioUSD;
                        it.precioUnitarioBs = precioBs;
                        it.totalBs = it.cantidad * it.precioUnitarioBs;
                        it.totalUSD = it.cantidad * it.precioUnitarioUSD;
                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) {
                    itemsFactura.add(new ItemFactura(
                            a.codigo, descFinal, cant, precioBs, precioUSD,
                            a.tipoArticulo, tipoIvaFinal, porcIvaFinal
                    ));
                }
            } else {
                // El Servicio Genérico siempre se agrega como línea independiente con la descripción que escribió el usuario
                itemsFactura.add(new ItemFactura(
                        Articulo.CODIGO_GENERICO, descFinal, cant, precioBs, precioUSD,
                        "S", tipoIvaFinal, porcIvaFinal
                ));
            }

            itemAdapter.notifyDataSetChanged();
            calcularTotales();
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void emitirFactura() {
        if (itemsFactura.isEmpty()) {
            Toast.makeText(this, "Debe agregar al menos un artículo a la factura", Toast.LENGTH_SHORT).show();
            return;
        }

        config = DataManager.getConfiguracion(this);
        articulosList = DataManager.getArticulos(this);

        // Validar stock antes de facturar si no está permitido vender en negativo
        if (!config.permitirStockNegativo) {
            for (ItemFactura it : itemsFactura) {
                Articulo art = buscarArticuloPorCodigo(it.codigoArticulo);
                if (art != null && art.manejaStock() && it.cantidad > art.stock) {
                    new AlertDialog.Builder(this)
                            .setTitle("Stock Insuficiente para Facturar")
                            .setMessage("El artículo \"" + art.descripcion + "\" solo cuenta con " + art.stock
                                    + " unidades en stock y se intentan facturar " + it.cantidad + ".\n\n"
                                    + "Para facturar sin stock, habilite 'Vender con Stock Negativo' en Configuración.")
                            .setPositiveButton("Aceptar", null)
                            .show();
                    return;
                }
            }
        }

        Cliente cliente = (Cliente) spnClientes.getSelectedItem();
        Vendedor vendedor = (Vendedor) spnVendedores.getSelectedItem();

        if (cliente == null || vendedor == null) {
            Toast.makeText(this, "Debe seleccionar un cliente y un vendedor", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdfFecha = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("es", "VE"));
        SimpleDateFormat sdfHora = new SimpleDateFormat("hh:mm:ss a", new Locale("es", "VE"));
        Date ahora = new Date();

        Factura f = new Factura();
        f.tipoDocumento = tipoDocumento;
        f.referenciaExtra = etReferenciaExtra != null ? etReferenciaExtra.getText().toString().trim() : "";
        String fechaStr = sdfFecha.format(ahora);
        if (!fechaStr.isEmpty()) {
            fechaStr = Character.toUpperCase(fechaStr.charAt(0)) + fechaStr.substring(1);
        }
        f.fecha = fechaStr;
        f.hora = sdfHora.format(ahora);
        f.clienteCodigo = cliente.codigo;
        f.clienteNombre = cliente.nombre;
        f.clienteRif = cliente.rif;
        f.clienteDireccion = cliente.direccion;
        f.clienteTelefono = cliente.telefono != null ? cliente.telefono : "";
        f.clienteCorreo = cliente.correo != null ? cliente.correo : "";
        f.vendedorCodigo = vendedor.codigo;
        f.vendedorNombre = vendedor.nombre;
        f.items = new ArrayList<>(itemsFactura);
        f.subtotalBs = totalSubtotalBs;
        f.exentoBs = totalExentoBs;
        f.ivaBs = totalIvaBs;
        f.totalBs = totalGeneralBs;
        f.totalUSD = totalGeneralUSD;
        f.tasaDolar = config.tasaDolar;
        f.usuarioEmisor = DataManager.currentUser;

        // Registrar en data manager (actualiza correlativos y descuenta stock)
        Factura emitida = DataManager.registrarFactura(this, f);

        mostrarDialogoRecibo(emitida);
    }

    private void mostrarDialogoRecibo(Factura f) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_detalle_factura);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);

        android.widget.ImageView ivPreview = dialog.findViewById(R.id.ivFacturaPdfPreview);
        TextView tvRecibo = dialog.findViewById(R.id.tvFacturaReciboTexto);
        Button btnCompartir = dialog.findViewById(R.id.btnCompartirFactura);
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDetalle);

        if (ivPreview != null) {
            ivPreview.setVisibility(View.VISIBLE);
            ivPreview.setImageBitmap(FacturaPdfGenerator.renderizarPreviewBitmap(this, f));
        }
        if (tvRecibo != null) {
            tvRecibo.setVisibility(View.GONE);
        }

        btnCompartir.setOnClickListener(v -> FacturaPdfGenerator.compartirPdf(this, f));

        btnCerrar.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }
}
