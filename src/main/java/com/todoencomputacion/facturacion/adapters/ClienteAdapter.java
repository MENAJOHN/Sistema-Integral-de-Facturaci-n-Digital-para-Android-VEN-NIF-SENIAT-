package com.todoencomputacion.facturacion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.models.Cliente;

import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ViewHolder> {

    public interface OnClienteClickListener {
        void onClienteClick(Cliente cliente);
    }

    private final Context context;
    private List<Cliente> clientes;
    private final OnClienteClickListener listener;

    public ClienteAdapter(Context context, List<Cliente> clientes, OnClienteClickListener listener) {
        this.context = context;
        this.clientes = clientes;
        this.listener = listener;
    }

    public void updateList(List<Cliente> newList) {
        this.clientes = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cliente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cliente cli = clientes.get(position);
        holder.tvNombre.setText(cli.nombre);
        holder.tvRif.setText("RIF: " + cli.rif + " | Cód: " + cli.codigo);
        holder.tvTelefono.setText("Tel: " + cli.telefono + " | " + cli.correo);
        holder.tvDireccion.setText(cli.direccion);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClienteClick(cli);
        });
    }

    @Override
    public int getItemCount() {
        return clientes != null ? clientes.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvRif, tvTelefono, tvDireccion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvClienteNombre);
            tvRif = itemView.findViewById(R.id.tvClienteRif);
            tvTelefono = itemView.findViewById(R.id.tvClienteTelefono);
            tvDireccion = itemView.findViewById(R.id.tvClienteDireccion);
        }
    }
}