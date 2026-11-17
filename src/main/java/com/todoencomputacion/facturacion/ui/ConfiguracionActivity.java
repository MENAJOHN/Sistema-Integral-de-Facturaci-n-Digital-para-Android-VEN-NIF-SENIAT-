package com.todoencomputacion.facturacion.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.data.FacturaPdfGenerator;
import com.todoencomputacion.facturacion.models.Configuracion;
import com.todoencomputacion.facturacion.models.PermisosPerfil;

import java.util.Locale;

public class ConfiguracionActivity extends AppCompatActivity {

    private static final int REQ_PICK_LOGO = 501;

    private EditText etEmpresa, etEslogan, etRif, etTelefono, etCorreo, etDireccion;
    private EditText etTasaDolar, etIva, etProxFactura, etProxControl;
    private EditText etUsuarioImprenta, etClaveImprenta, etTokenImprenta;
    private ImageView ivCfgLogoPreview;
    private TextView tvEstadoLogoPdf;

    private Button btnSelectPerfilUsuario, btnSelectPerfilCaja;
    private TextView tvPerfilEditandoLabel;
    private CheckBox cbCfgPermitirStockNegativo;
    private RadioGroup rgCfgEditarPrecio;
    private RadioButton rbCfgEditarPrecioSi, rbCfgEditarPrecioNo;
    private CheckBox cbPermFactura, cbPermFacturaTerceros, cbPermNotaCredito, cbPermNotaCreditoParcial,
            cbPermNotaDebito, cbPermOrdenEntrega, cbPermOrdenEntregaInd, cbPermGuiaDespacho;
    private CheckBox cbPermArticulos, cbPermClientes, cbPermVendedores;
    private CheckBox cbPermRepHistorial, cbPermRepVentas, cbPermRepVendedores, cbPermRepInventario, cbPermRepNotasGuias;

    private boolean editandoCaja = false;
    private boolean cargandoCheckboxes = false;
    private Configuracion config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        config = DataManager.getConfiguracion(this);

        ImageButton btnBack = findViewById(R.id.btnBackConfig);
        etEmpresa = findViewById(R.id.etCfgEmpresa);
        etEslogan = findViewById(R.id.etCfgEslogan);
        etRif = findViewById(R.id.etCfgRif);
        etTelefono = findViewById(R.id.etCfgTelefono);
        etCorreo = findViewById(R.id.etCfgCorreo);
        etDireccion = findViewById(R.id.etCfgDireccion);
        etTasaDolar = findViewById(R.id.etCfgTasaDolar);
        etIva = findViewById(R.id.etCfgIva);
        etProxFactura = findViewById(R.id.etCfgProxFactura);
        etProxControl = findViewById(R.id.etCfgProxControl);
        etUsuarioImprenta = findViewById(R.id.etCfgUsuarioImprenta);
        etClaveImprenta = findViewById(R.id.etCfgClaveImprenta);
        etTokenImprenta = findViewById(R.id.etCfgTokenImprenta);
        ivCfgLogoPreview = findViewById(R.id.ivCfgLogoPreview);
        tvEstadoLogoPdf = findViewById(R.id.tvEstadoLogoPdf);
        Button btnAdjuntarLogoCliente = findViewById(R.id.btnAdjuntarLogoCliente);
        Button btnRestaurarLogoPropio = findViewById(R.id.btnRestaurarLogoPropio);

        btnSelectPerfilUsuario = findViewById(R.id.btnSelectPerfilUsuario);
        btnSelectPerfilCaja = findViewById(R.id.btnSelectPerfilCaja);
        tvPerfilEditandoLabel = findViewById(R.id.tvPerfilEditandoLabel);

        cbCfgPermitirStockNegativo = findViewById(R.id.cbCfgPermitirStockNegativo);
        rgCfgEditarPrecio = findViewById(R.id.rgCfgEditarPrecio);
        rbCfgEditarPrecioSi = findViewById(R.id.rbCfgEditarPrecioSi);
        rbCfgEditarPrecioNo = findViewById(R.id.rbCfgEditarPrecioNo);

        cbPermFactura = findViewById(R.id.cbPermFactura);
        cbPermFacturaTerceros = findViewById(R.id.cbPermFacturaTerceros);
        cbPermNotaCredito = findViewById(R.id.cbPermNotaCredito);
        cbPermNotaCreditoParcial = findViewById(R.id.cbPermNotaCreditoParcial);
        cbPermNotaDebito = findViewById(R.id.cbPermNotaDebito);
        cbPermOrdenEntrega = findViewById(R.id.cbPermOrdenEntrega);
        cbPermOrdenEntregaInd = findViewById(R.id.cbPermOrdenEntregaInd);
        cbPermGuiaDespacho = findViewById(R.id.cbPermGuiaDespacho);

        cbPermArticulos = findViewById(R.id.cbPermArticulos);
        cbPermClientes = findViewById(R.id.cbPermClientes);
        cbPermVendedores = findViewById(R.id.cbPermVendedores);

        cbPermRepHistorial = findViewById(R.id.cbPermRepHistorial);
        cbPermRepVentas = findViewById(R.id.cbPermRepVentas);
        cbPermRepVendedores = findViewById(R.id.cbPermRepVendedores);
        cbPermRepInventario = findViewById(R.id.cbPermRepInventario);
        cbPermRepNotasGuias = findViewById(R.id.cbPermRepNotasGuias);

        Button btnActualizarToken = findViewById(R.id.btnActualizarToken);
        Button btnGuardar = findViewById(R.id.btnGuardarConfig);
        Button btnRestablecer = findViewById(R.id.btnRestablecerAssets);

        btnBack.setOnClickListener(v -> finish());

        cargarCampos();
        actualizarVistaLogoPdf();
        configurarListenersPermisos();

        btnAdjuntarLogoCliente.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Seleccione el Logo del Cliente"), REQ_PICK_LOGO);
        });

        btnRestaurarLogoPropio.setOnClickListener(v -> {
            FacturaPdfGenerator.eliminarLogoCliente(this);
            actualizarVistaLogoPdf();
            Toast.makeText(this, "Se restauró tu logo por defecto para las facturas PDF", Toast.LENGTH_SHORT).show();
        });

        btnSelectPerfilUsuario.setOnClickListener(v -> {
            guardarPermisosEnMemoria();
            editandoCaja = false;
            actualizarVistaSelectorPerfil();
        });

        btnSelectPerfilCaja.setOnClickListener(v -> {
            guardarPermisosEnMemoria();
            editandoCaja = true;
            actualizarVistaSelectorPerfil();
        });

        // Actualizar tasa automáticamente desde https://www.bcv.org.ve/
        DataManager.actualizarTasaDesdeBCV(this, nuevaTasa -> {
            config.tasaDolar = nuevaTasa;
            etTasaDolar.setText(String.format(Locale.US, "%.2f", nuevaTasa));
            Toast.makeText(this, "Tasa BCV actualizada automáticamente: Bs. " + String.format(Locale.US, "%.2f", nuevaTasa), Toast.LENGTH_SHORT).show();
        });

        btnActualizarToken.setOnClickListener(v -> actualizarTokenImprenta());
        btnGuardar.setOnClickListener(v -> guardarConfiguracion());
        btnRestablecer.setOnClickListener(v -> confirmarRestablecer());
    }

    private void configurarListenersPermisos() {
        CheckBox[] all = new CheckBox[]{
                cbPermFactura, cbPermFacturaTerceros, cbPermNotaCredito, cbPermNotaCreditoParcial,
                cbPermNotaDebito, cbPermOrdenEntrega, cbPermOrdenEntregaInd, cbPermGuiaDespacho,
                cbPermArticulos, cbPermClientes, cbPermVendedores,
                cbPermRepHistorial, cbPermRepVentas, cbPermRepVendedores, cbPermRepInventario, cbPermRepNotasGuias
        };
        for (CheckBox cb : all) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!cargandoCheckboxes) {
                    guardarPermisosEnMemoria();
                }
            });
        }
    }

    private void actualizarVistaSelectorPerfil() {
        if (!editandoCaja) {
            btnSelectPerfilUsuario.setBackgroundResource(R.drawable.button_primary);
            btnSelectPerfilUsuario.setTextColor(getResources().getColor(R.color.white));
            btnSelectPerfilCaja.setBackgroundResource(R.drawable.button_secondary);
            btnSelectPerfilCaja.setTextColor(getResources().getColor(R.color.primary));
            tvPerfilEditandoLabel.setText("Editando permisos de: PERFIL USUARIO (usuario / 1234)");
        } else {
            btnSelectPerfilCaja.setBackgroundResource(R.drawable.button_primary);
            btnSelectPerfilCaja.setTextColor(getResources().getColor(R.color.white));
            btnSelectPerfilUsuario.setBackgroundResource(R.drawable.button_secondary);
            btnSelectPerfilUsuario.setTextColor(getResources().getColor(R.color.primary));
            tvPerfilEditandoLabel.setText("Editando permisos de: PERFIL CAJA (caja / 1234)");
        }
        cargarCheckboxesDePerfil(editandoCaja ? config.permisosCaja : config.permisosUsuario);
    }

    private void cargarCheckboxesDePerfil(PermisosPerfil p) {
        if (p == null) p = new PermisosPerfil();
        cargandoCheckboxes = true;
        cbPermFactura.setChecked(p.factura);
        cbPermFacturaTerceros.setChecked(p.facturaTerceros);
        cbPermNotaCredito.setChecked(p.notaCredito);
        cbPermNotaCreditoParcial.setChecked(p.notaCreditoParcial);
        cbPermNotaDebito.setChecked(p.notaDebito);
        cbPermOrdenEntrega.setChecked(p.ordenEntrega);
        cbPermOrdenEntregaInd.setChecked(p.ordenEntregaIndividual);
        cbPermGuiaDespacho.setChecked(p.guiaDespacho);

        cbPermArticulos.setChecked(p.articulos);
        cbPermClientes.setChecked(p.clientes);
        cbPermVendedores.setChecked(p.vendedores);

        cbPermRepHistorial.setChecked(p.repHistorial);
        cbPermRepVentas.setChecked(p.repVentasGeneral);
        cbPermRepVendedores.setChecked(p.repVendedores);
        cbPermRepInventario.setChecked(p.repInventario);
        cbPermRepNotasGuias.setChecked(p.repNotasGuias);
        cargandoCheckboxes = false;
    }

    private void guardarPermisosEnMemoria() {
        PermisosPerfil p = editandoCaja ? config.permisosCaja : config.permisosUsuario;
        if (p == null) {
            p = new PermisosPerfil();
            if (editandoCaja) config.permisosCaja = p;
            else config.permisosUsuario = p;
        }
        p.factura = cbPermFactura.isChecked();
        p.facturaTerceros = cbPermFacturaTerceros.isChecked();
        p.notaCredito = cbPermNotaCredito.isChecked();
        p.notaCreditoParcial = cbPermNotaCreditoParcial.isChecked();
        p.notaDebito = cbPermNotaDebito.isChecked();
        p.ordenEntrega = cbPermOrdenEntrega.isChecked();
        p.ordenEntregaIndividual = cbPermOrdenEntregaInd.isChecked();
        p.guiaDespacho = cbPermGuiaDespacho.isChecked();

        p.articulos = cbPermArticulos.isChecked();
        p.clientes = cbPermClientes.isChecked();
        p.vendedores = cbPermVendedores.isChecked();

        p.repHistorial = cbPermRepHistorial.isChecked();
        p.repVentasGeneral = cbPermRepVentas.isChecked();
        p.repVendedores = cbPermRepVendedores.isChecked();
        p.repInventario = cbPermRepInventario.isChecked();
        p.repNotasGuias = cbPermRepNotasGuias.isChecked();
    }

    private void cargarCampos() {
        etEmpresa.setText(config.empresaNombre);
        etEslogan.setText(config.eslogan);
        etRif.setText(config.rif);
        etTelefono.setText(config.telefono);
        etCorreo.setText(config.correo);
        etDireccion.setText(config.direccion);
        etTasaDolar.setText(String.format(Locale.US, "%.2f", config.tasaDolar));
        etIva.setText(String.format(Locale.US, "%.1f", config.porcentajeIva));
        etProxFactura.setText(String.valueOf(config.proximoNumeroFactura));
        etProxControl.setText(String.valueOf(config.proximoNumeroControl));
        etUsuarioImprenta.setText(config.usuarioImprenta != null ? config.usuarioImprenta : "");
        etClaveImprenta.setText(config.claveImprenta != null ? config.claveImprenta : "");
        etTokenImprenta.setText(config.tokenImprenta != null ? config.tokenImprenta : "");
        if (cbCfgPermitirStockNegativo != null) {
            cbCfgPermitirStockNegativo.setChecked(config.permitirStockNegativo);
        }
        if (rbCfgEditarPrecioSi != null && rbCfgEditarPrecioNo != null) {
            if (config.permitirEditarPrecio) {
                rbCfgEditarPrecioSi.setChecked(true);
            } else {
                rbCfgEditarPrecioNo.setChecked(true);
            }
        }

        actualizarVistaSelectorPerfil();
    }

    private void actualizarTokenImprenta() {
        config.usuarioImprenta = etUsuarioImprenta.getText().toString().trim();
        config.claveImprenta = etClaveImprenta.getText().toString().trim();
        config.tokenImprenta = etTokenImprenta.getText().toString().trim();
        guardarPermisosEnMemoria();
        DataManager.saveConfiguracion(this, config);
        Toast.makeText(this, "Token y credenciales de Imprenta Digital actualizados", Toast.LENGTH_SHORT).show();
    }

    private void guardarConfiguracion() {
        try {
            config.empresaNombre = etEmpresa.getText().toString().trim();
            config.eslogan = etEslogan.getText().toString().trim();
            config.rif = etRif.getText().toString().trim();
            config.telefono = etTelefono.getText().toString().trim();
            config.correo = etCorreo.getText().toString().trim();
            config.direccion = etDireccion.getText().toString().trim();
            config.tasaDolar = Double.parseDouble(etTasaDolar.getText().toString().trim());
            config.porcentajeIva = Double.parseDouble(etIva.getText().toString().trim());
            config.proximoNumeroFactura = Integer.parseInt(etProxFactura.getText().toString().trim());
            config.proximoNumeroControl = Integer.parseInt(etProxControl.getText().toString().trim());
            config.usuarioImprenta = etUsuarioImprenta.getText().toString().trim();
            config.claveImprenta = etClaveImprenta.getText().toString().trim();
            config.tokenImprenta = etTokenImprenta.getText().toString().trim();
            if (cbCfgPermitirStockNegativo != null) {
                config.permitirStockNegativo = cbCfgPermitirStockNegativo.isChecked();
            }
            if (rbCfgEditarPrecioSi != null) {
                config.permitirEditarPrecio = rbCfgEditarPrecioSi.isChecked();
            }

            guardarPermisosEnMemoria();

            DataManager.saveConfiguracion(this, config);
            Toast.makeText(this, "Configuración y permisos actualizados correctamente", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error en los datos ingresados: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void actualizarVistaLogoPdf() {
        if (ivCfgLogoPreview != null) {
            ivCfgLogoPreview.setImageBitmap(FacturaPdfGenerator.obtenerLogoFactura(this));
        }
        if (tvEstadoLogoPdf != null) {
            if (FacturaPdfGenerator.tieneLogoPersonalizado(this)) {
                tvEstadoLogoPdf.setText("✓ Logo personalizado del cliente activo para las facturas PDF.");
            } else {
                tvEstadoLogoPdf.setText("Actualmente usando tu logo por defecto en el encabezado del PDF.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_LOGO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (FacturaPdfGenerator.guardarLogoCliente(this, uri)) {
                actualizarVistaLogoPdf();
                Toast.makeText(this, "¡Logo del cliente adjuntado con éxito para el formato PDF!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No se pudo cargar la imagen seleccionada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmarRestablecer() {
        new AlertDialog.Builder(this)
                .setTitle("Restablecer Datos de Fábrica")
                .setMessage("¿Desea restablecer todos los datos del sistema a sus valores de fábrica?")
                .setPositiveButton("Sí, Restablecer", (dialog, which) -> {
                    DataManager.restoreFromAssets(this);
                    config = DataManager.getConfiguracion(this);
                    cargarCampos();
                    Toast.makeText(this, "¡Datos de fábrica restablecidos con éxito!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
