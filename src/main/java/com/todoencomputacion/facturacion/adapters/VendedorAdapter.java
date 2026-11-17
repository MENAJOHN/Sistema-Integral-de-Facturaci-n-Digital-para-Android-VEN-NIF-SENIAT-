package com.todoencomputacion.facturacion.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Vendedor;

import java.util.List;
import java.util.Locale;

public class VendedorAdapter extends RecyclerView.Adapter<VendedorAdapter.ViewHolder> {

    public interface OnVendedorClickListener {
        void onVendedorClick(Vendedor vendedor);
    }

    private final Context context;
    private List<Vendedor> vendedores;
    private final OnVendedorClickListener listener;

    public VendedorAdapter(Context context, List<Vendedor> vendedores, OnVendedorClickListener listener) {
        this.context = context;
        this.vendedores = vendedores;
        this.listener = listener;
    }

    public void updateList(List<Vendedor> newList) {
        this.vendedores = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vendedor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vendedor ven = vendedores.get(position);
        holder.tvNombre.setText(ven.nombre);

        String cedTxt = (ven.cedula != null && !ven.cedula.trim().isEmpty()) ? (" | C.I.: " + ven.cedula) : "";
        holder.tvCodigo.setText("Código: " + ven.codigo + cedTxt);

        if (holder.tvDireccion != null) {
            if (ven.direccion != null && !ven.direccion.trim().isEmpty()) {
                holder.tvDireccion.setVisibility(View.VISIBLE);
                holder.tvDireccion.setText("Dirección: " + ven.direccion);
            } else {
                holder.tvDireccion.setVisibility(View.GONE);
            }
        }

        holder.tvTelefono.setText("Tel: " + ven.telefono + " | " + ven.correo);
        holder.tvComision.setText(String.format(Locale.US, "Comisión: %.1f%%", ven.comision));

        if (holder.ivFoto != null) {
            Bitmap bmp = DataManager.cargarImagenVendedor(context, ven.foto);
            if (bmp != null) {
                holder.ivFoto.setImageBitmap(bmp);
            } else {
                holder.ivFoto.setImageResource(R.drawable.app_logo);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onVendedorClick(ven);
        });
    }

    @Override
    public int getItemCount() {
        return vendedores != null ? vendedores.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNombre, tvCodigo, tvDireccion, tvTelefono, tvComision;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivVendedorFoto);
            tvNombre = itemView.findViewById(R.id.tvVendedorNombre);
            tvCodigo = itemView.findViewById(R.id.tvVendedorCodigo);
            tvDireccion = itemView.findViewById(R.id.tvVendedorDireccion);
            tvTelefono = itemView.findViewById(R.id.tvVendedorTelefono);
            tvComision = itemView.findViewById(R.id.tvVendedorComision);
        }
    }
}
