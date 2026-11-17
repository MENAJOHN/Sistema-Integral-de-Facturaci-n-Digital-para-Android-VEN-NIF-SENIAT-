package com.todoencomputacion.facturacion.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.adapters.ClienteAdapter;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Cliente;

import java.util.ArrayList;
import java.util.List;

public class ClientesActivity extends AppCompatActivity {

    private RecyclerView rvClientes;
    private ClienteAdapter adapter;
    private List<Cliente> clientesList;
    private List<Cliente> filtrados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        clientesList = DataManager.getClientes(this);
        filtrados = new ArrayList<>(clientesList);

        ImageButton btnBack = findViewById(R.id.btnBackClientes);
        ImageButton btnNuevo = findViewById(R.id.btnNuevoCliente);
        EditText etBuscar = findViewById(R.id.etBuscarCliente);
        rvClientes = findViewById(R.id.rvClientes);

        btnBack.setOnClickListener(v -> finish());
        btnNuevo.setOnClickListener(v -> mostrarDialogoCliente(null));

        adapter = new ClienteAdapter(this, filtrados, cliente -> mostrarDialogoCliente(cliente));

        rvClientes.setLayoutManager(new LinearLayoutManager(this));
        rvClientes.setAdapter(adapter);

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
        for (Cliente c : clientesList) {
            if (c.nombre.toLowerCase().contains(q) || c.rif.toLowerCase().contains(q) || c.codigo.toLowerCase().contains(q)) {
                filtrados.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoCliente(Cliente c) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_nuevo_cliente);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etCod = dialog.findViewById(R.id.etCliCodigo);
        EditText etNom = dialog.findViewById(R.id.etCliNombre);
        EditText etRif = dialog.findViewById(R.id.etCliRif);
        EditText etTel = dialog.findViewById(R.id.etCliTelefono);
        EditText etCor = dialog.findViewById(R.id.etCliCorreo);
        EditText etDir = dialog.findViewById(R.id.etCliDireccion);
        Button btnGuardar = dialog.findViewById(R.id.btnGuardarCliente);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelarCliente);

        if (c != null) {
            etCod.setText(c.codigo);
            etCod.setEnabled(false);
            etNom.setText(c.nombre);
            etRif.setText(c.rif);
            etTel.setText(c.telefono);
            etCor.setText(c.correo);
            etDir.setText(c.direccion);
        } else {
            etCod.setText("CLI-00" + (clientesList.size() + 1));
        }

        btnGuardar.setOnClickListener(v -> {
            String cod = etCod.getText().toString().trim();
            String nom = etNom.getText().toString().trim();
            String rif = etRif.getText().toString().trim();
            String tel = etTel.getText().toString().trim();
            String cor = etCor.getText().toString().trim();
            String dir = etDir.getText().toString().trim();

            if (cod.isEmpty() || nom.isEmpty() || rif.isEmpty()) {
                Toast.makeText(this, "Código, Nombre y RIF son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            Cliente nuevo = new Cliente(cod, nom, rif, tel, cor, dir);
            DataManager.addOrUpdateCliente(this, nuevo);

            clientesList = DataManager.getClientes(this);
            filtrar("");
            dialog.dismiss();
            Toast.makeText(this, "Cliente guardado exitosamente", Toast.LENGTH_SHORT).show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}