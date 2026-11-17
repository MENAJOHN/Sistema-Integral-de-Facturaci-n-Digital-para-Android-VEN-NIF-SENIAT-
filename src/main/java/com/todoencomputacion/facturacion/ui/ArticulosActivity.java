package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.adapters.ArticuloAdapter;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Articulo;
import com.todoencomputacion.facturacion.models.Configuracion;

import java.util.ArrayList;
import java.util.List;

public class ArticulosActivity extends AppCompatActivity {

    private RecyclerView rvArticulos;
    private ArticuloAdapter adapter;
    private List<Articulo> articulosList;
    private List<Articulo> filtrados;
    private Configuracion config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_articulos);

        config = DataManager.getConfiguracion(this);
        articulosList = DataManager.getArticulos(this);
        filtrados = new ArrayList<>(articulosList);

        ImageButton btnBack = findViewById(R.id.btnBackArticulos);
        ImageButton btnNuevo = findViewById(R.id.btnNuevoArticulo);
        EditText etBuscar = findViewById(R.id.etBuscarArticulo);
        rvArticulos = findViewById(R.id.rvArticulos);

        btnBack.setOnClickListener(v -> finish());
        btnNuevo.setOnClickListener(v -> {
            if (DataManager.isMaster) {
                mostrarDialogoArticulo(null);
            } else {
                Toast.makeText(this, "Solo el usuario Master (mena) puede crear o modificar artículos.", Toast.LENGTH_LONG).show();
            }
        });

        adapter = new ArticuloAdapter(this, filtrados, articulo -> {
            if (DataManager.isMaster) {
                mostrarDialogoArticulo(articulo);
            } else {
                String infoStock = articulo.manejaStock() ? ("Stock disponible: " + articulo.stock) : "Tipo Servicio (Sin Stock)";
                Toast.makeText(this, articulo.descripcion + "\n" + infoStock + " | " + articulo.getEtiquetaIva(), Toast.LENGTH_SHORT).show();
            }
        });

        rvArticulos.setLayoutManager(new LinearLayoutManager(this));
        rvArticulos.setAdapter(adapter);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrar(String q) {
        q = q.toLowerCase().trim();
        filtrados.clear();
        for (Articulo a : articulosList) {
            if (a.descripcion.toLowerCase().contains(q) || a.codigo.toLowerCase().contains(q)) {
                filtrados.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoArticulo(Articulo a) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_nuevo_articulo);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTitulo = dialog.findViewById(R.id.tvTituloDialogArt);
        EditText etCodigo = dialog.findViewById(R.id.etArtCodigo);
        EditText etDesc = dialog.findViewById(R.id.etArtDesc);
        EditText etPrecioUSD = dialog.findViewById(R.id.etArtPrecioUSD);
        TextView tvLabelStock = dialog.findViewById(R.id.tvLabelArtStock);
        EditText etStock = dialog.findViewById(R.id.etArtStock);
        EditText etImagen = dialog.findViewById(R.id.etArtImagen);

        RadioGroup rgTipo = dialog.findViewById(R.id.rgArtTipo);
        RadioButton rbTipoVenta = dialog.findViewById(R.id.rbArtTipoVenta);
        RadioButton rbTipoServicio = dialog.findViewById(R.id.rbArtTipoServicio);

        RadioGroup rgIva = dialog.findViewById(R.id.rgArtIva);
        RadioButton rbIva16 = dialog.findViewById(R.id.rbIva16);
        RadioButton rbIva8 = dialog.findViewById(R.id.rbIva8);
        RadioButton rbIva32 = dialog.findViewById(R.id.rbIva32);
        RadioButton rbIvaExento = dialog.findViewById(R.id.rbIvaExento);

        Button btnGuardar = dialog.findViewById(R.id.btnGuardarArticulo);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelarArticulo);

        rgTipo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbArtTipoServicio) {
                tvLabelStock.setVisibility(View.GONE);
                etStock.setVisibility(View.GONE);
                etStock.setText("0");
            } else {
                tvLabelStock.setVisibility(View.VISIBLE);
                etStock.setVisibility(View.VISIBLE);
            }
        });

        if (a != null) {
            tvTitulo.setText("Editar Artículo: " + a.codigo);
            etCodigo.setText(a.codigo);
            etCodigo.setEnabled(false);
            etDesc.setText(a.descripcion);
            etPrecioUSD.setText(String.valueOf(a.precioUSD));
            etStock.setText(String.valueOf(a.stock));
            etImagen.setText(a.imagen);

            if (!a.manejaStock()) {
                rbTipoServicio.setChecked(true);
                tvLabelStock.setVisibility(View.GONE);
                etStock.setVisibility(View.GONE);
            } else {
                rbTipoVenta.setChecked(true);
                tvLabelStock.setVisibility(View.VISIBLE);
                etStock.setVisibility(View.VISIBLE);
            }

            if (a.esExento()) {
                rbIvaExento.setChecked(true);
            } else if (Math.abs(a.porcentajeIva - 8.0) < 0.01) {
                rbIva8.setChecked(true);
            } else if (Math.abs(a.porcentajeIva - 32.0) < 0.01) {
                rbIva32.setChecked(true);
            } else {
                rbIva16.setChecked(true);
            }
        } else {
            tvTitulo.setText("Nuevo Artículo / Servicio");
            etCodigo.setText("ART-00" + (articulosList.size() + 1));
            etStock.setText("10");
            etImagen.setText("laptop.png");
            rbTipoVenta.setChecked(true);
            rbIva16.setChecked(true);
        }

        btnGuardar.setOnClickListener(v -> {
            String cod = etCodigo.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String pUSDStr = etPrecioUSD.getText().toString().trim();
            String stkStr = etStock.getText().toString().trim();
            String img = etImagen.getText().toString().trim();

            boolean esServicio = rbTipoServicio.isChecked();
            String tipoArticulo = esServicio ? "S" : "V";

            if (cod.isEmpty() || desc.isEmpty() || pUSDStr.isEmpty() || (!esServicio && stkStr.isEmpty())) {
                Toast.makeText(this, "Complete todos los campos requeridos", Toast.LENGTH_SHORT).show();
                return;
            }

            double pUSD = Double.parseDouble(pUSDStr);
            int stk = esServicio ? 0 : Integer.parseInt(stkStr);
            double pBs = Math.round(pUSD * config.tasaDolar * 100.0) / 100.0;

            String tipoIva = "16%";
            double porcentajeIva = 16.0;
            int ivaId = rgIva.getCheckedRadioButtonId();
            if (ivaId == R.id.rbIva8) {
                tipoIva = "8%";
                porcentajeIva = 8.0;
            } else if (ivaId == R.id.rbIva32) {
                tipoIva = "32%";
                porcentajeIva = 32.0;
            } else if (ivaId == R.id.rbIvaExento) {
                tipoIva = "EXENTO";
                porcentajeIva = 0.0;
            }

            Articulo art = new Articulo(cod, desc, pUSD, pBs, stk, img, tipoArticulo, tipoIva, porcentajeIva);
            DataManager.addOrUpdateArticulo(this, art);

            articulosList = DataManager.getArticulos(this);
            filtrar("");
            dialog.dismiss();
            Toast.makeText(this, "Artículo guardado exitosamente", Toast.LENGTH_SHORT).show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
