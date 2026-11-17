package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.adapters.FacturaAdapter;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.data.FacturaPdfGenerator;
import com.todoencomputacion.facturacion.models.Factura;

import java.util.List;

public class FacturasActivity extends AppCompatActivity {

    private RecyclerView rvFacturas;
    private TextView tvEmpty;
    private FacturaAdapter adapter;
    private List<Factura> facturasList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facturas);

        ImageButton btnBack = findViewById(R.id.btnBackFacturas);
        rvFacturas = findViewById(R.id.rvFacturas);
        tvEmpty = findViewById(R.id.tvEmptyFacturas);

        btnBack.setOnClickListener(v -> finish());

        facturasList = DataManager.getFacturas(this);

        adapter = new FacturaAdapter(this, facturasList, factura -> mostrarDetalleRecibo(factura));
        rvFacturas.setLayoutManager(new LinearLayoutManager(this));
        rvFacturas.setAdapter(adapter);

        actualizarVista();
    }

    private void actualizarVista() {
        if (facturasList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvFacturas.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvFacturas.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDetalleRecibo(Factura f) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_detalle_factura);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

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

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}