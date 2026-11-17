package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.adapters.VendedorAdapter;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Vendedor;

import java.util.ArrayList;
import java.util.List;

public class VendedoresActivity extends AppCompatActivity {

    private static final int REQ_PICK_FOTO_VENDEDOR = 601;

    private RecyclerView rvVendedores;
    private VendedorAdapter adapter;
    private List<Vendedor> vendedoresList;
    private List<Vendedor> filtrados;
    private EditText etBuscar;

    // Referencias temporales para la foto en el diálogo activo
    private ImageView ivFotoPreviewActual;
    private String fotoSeleccionadaActual = "";
    private String codigoVendedorEnEdicion = "ven";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendedores);

        vendedoresList = DataManager.getVendedores(this);
        filtrados = new ArrayList<>(vendedoresList);

        ImageButton btnBack = findViewById(R.id.btnBackVendedores);
        ImageButton btnNuevo = findViewById(R.id.btnNuevoVendedor);
        etBuscar = findViewById(R.id.etBuscarVendedor);
        rvVendedores = findViewById(R.id.rvVendedores);

        btnBack.setOnClickListener(v -> finish());
        btnNuevo.setOnClickListener(v -> {
            if (DataManager.isMaster) {
                mostrarDialogoVendedor(null);
            } else {
                Toast.makeText(this, "Solo el usuario Master (mena) puede crear o editar vendedores.", Toast.LENGTH_SHORT).show();
            }
        });

        adapter = new VendedorAdapter(this, filtrados, vendedor -> {
            if (DataManager.isMaster) {
                mostrarDialogoVendedor(vendedor);
            } else {
                String infoCed = (vendedor.cedula != null && !vendedor.cedula.isEmpty()) ? ("\nCédula: " + vendedor.cedula) : "";
                String infoDir = (vendedor.direccion != null && !vendedor.direccion.isEmpty()) ? ("\nDirección: " + vendedor.direccion) : "";
                Toast.makeText(this, "Vendedor: " + vendedor.nombre + infoCed + infoDir + "\nTel: " + vendedor.telefono, Toast.LENGTH_SHORT).show();
            }
        });

        rvVendedores.setLayoutManager(new LinearLayoutManager(this));
        rvVendedores.setAdapter(adapter);

        if (etBuscar != null) {
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
    }

    private void filtrar(String q) {
        q = q != null ? q.toLowerCase().trim() : "";
        filtrados.clear();
        for (Vendedor v : vendedoresList) {
            boolean matchNombre = v.nombre != null && v.nombre.toLowerCase().contains(q);
            boolean matchCodigo = v.codigo != null && v.codigo.toLowerCase().contains(q);
            boolean matchCedula = v.cedula != null && v.cedula.toLowerCase().contains(q);
            boolean matchTel = v.telefono != null && v.telefono.toLowerCase().contains(q);
            boolean matchDir = v.direccion != null && v.direccion.toLowerCase().contains(q);
            if (matchNombre || matchCodigo || matchCedula || matchTel || matchDir) {
                filtrados.add(v);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoVendedor(Vendedor v) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_nuevo_vendedor);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ivFotoPreviewActual = dialog.findViewById(R.id.ivVenFotoPreview);
        Button btnPickFoto = dialog.findViewById(R.id.btnSeleccionarFotoVendedor);
        EditText etCod = dialog.findViewById(R.id.etVenCodigo);
        EditText etNom = dialog.findViewById(R.id.etVenNombre);
        EditText etCed = dialog.findViewById(R.id.etVenCedula);
        EditText etDir = dialog.findViewById(R.id.etVenDireccion);
        EditText etTel = dialog.findViewById(R.id.etVenTelefono);
        EditText etCor = dialog.findViewById(R.id.etVenCorreo);
        EditText etCom = dialog.findViewById(R.id.etVenComision);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardarVendedor);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelarVendedor);

        if (v != null) {
            etCod.setText(v.codigo);
            etCod.setEnabled(false);
            etNom.setText(v.nombre);
            etCed.setText(v.cedula != null ? v.cedula : "");
            etDir.setText(v.direccion != null ? v.direccion : "");
            etTel.setText(v.telefono);
            etCor.setText(v.correo);
            etCom.setText(String.valueOf(v.comision));
            fotoSeleccionadaActual = v.foto != null ? v.foto : "";
            codigoVendedorEnEdicion = v.codigo;

            Bitmap bmp = DataManager.cargarImagenVendedor(this, fotoSeleccionadaActual);
            if (bmp != null) {
                ivFotoPreviewActual.setImageBitmap(bmp);
            } else {
                ivFotoPreviewActual.setImageResource(R.drawable.app_logo);
            }
        } else {
            String nextCod = "VEN-00" + (vendedoresList.size() + 1);
            etCod.setText(nextCod);
            etCom.setText("5.0");
            fotoSeleccionadaActual = "";
            codigoVendedorEnEdicion = nextCod;
            ivFotoPreviewActual.setImageResource(R.drawable.app_logo);
        }

        btnPickFoto.setOnClickListener(view -> {
            codigoVendedorEnEdicion = etCod.getText().toString().trim();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Seleccione Foto del Vendedor"), REQ_PICK_FOTO_VENDEDOR);
        });

        btnGuardar.setOnClickListener(view -> {
            String cod = etCod.getText().toString().trim();
            String nom = etNom.getText().toString().trim();
            String ced = etCed.getText().toString().trim();
            String dir = etDir.getText().toString().trim();
            String tel = etTel.getText().toString().trim();
            String cor = etCor.getText().toString().trim();
            String comStr = etCom.getText().toString().trim();

            if (cod.isEmpty() || nom.isEmpty()) {
                Toast.makeText(this, "Código y Nombre son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            double com = comStr.isEmpty() ? 0.0 : Double.parseDouble(comStr);
            Vendedor ven = new Vendedor(cod, nom, ced, dir, tel, cor, com, fotoSeleccionadaActual);
            DataManager.addOrUpdateVendedor(this, ven);

            vendedoresList = DataManager.getVendedores(this);
            filtrar(etBuscar != null ? etBuscar.getText().toString() : "");
            dialog.dismiss();
            Toast.makeText(this, "Ficha del vendedor guardada exitosamente", Toast.LENGTH_SHORT).show();
        });

        btnCancelar.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FOTO_VENDEDOR && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String savedName = DataManager.guardarImagenVendedor(this, uri, codigoVendedorEnEdicion);
            if (savedName != null) {
                fotoSeleccionadaActual = savedName;
                if (ivFotoPreviewActual != null) {
                    Bitmap bmp = DataManager.cargarImagenVendedor(this, savedName);
                    if (bmp != null) {
                        ivFotoPreviewActual.setImageBitmap(bmp);
                    }
                }
                Toast.makeText(this, "Foto del vendedor cargada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se pudo procesar la imagen seleccionada", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
