package com.todoencomputacion.facturacion.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Articulo;

import java.util.List;
import java.util.Locale;

public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ViewHolder> {

    public interface OnArticuloClickListener {
        void onArticuloClick(Articulo articulo);
    }

    private final Context context;
    private List<Articulo> articulos;
    private final OnArticuloClickListener listener;

    public ArticuloAdapter(Context context, List<Articulo> articulos, OnArticuloClickListener listener) {
        this.context = context;
        this.articulos = articulos;
        this.listener = listener;
    }

    public void updateList(List<Articulo> newList) {
        this.articulos = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_articulo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Articulo art = articulos.get(position);
        holder.tvCodigo.setText(art.codigo + " • " + art.getEtiquetaTipo() + " • " + art.getEtiquetaIva());
        holder.tvDescripcion.setText(art.descripcion);
        if (art.esArticuloGenerico()) {
            holder.tvPrecioBs.setText("Precio Libre (Editable)");
            holder.tvPrecioUSD.setText("(Descripción Libre)");
            holder.tvStock.setText("SERVICIO GENÉRICO • Sin Stock");
            holder.tvStock.setTextColor(Color.parseColor("#7C3AED"));
        } else {
            holder.tvPrecioBs.setText(String.format(Locale.US, "Bs. %,.2f", art.precioBs));
            holder.tvPrecioUSD.setText(String.format(Locale.US, "$ %,.2f", art.precioUSD));

            if (!art.manejaStock()) {
                holder.tvStock.setText("SERVICIO (No lleva Stock)");
                holder.tvStock.setTextColor(Color.parseColor("#0284C7"));
            } else {
                holder.tvStock.setText("Stock disponible: " + art.stock);
                if (art.stock <= 0) {
                    holder.tvStock.setTextColor(Color.parseColor("#DC2626"));
                } else {
                    holder.tvStock.setTextColor(Color.parseColor("#64748B"));
                }
            }
        }

        Bitmap bmp = DataManager.cargarImagenArticulo(context, art.imagen);
        if (bmp != null) {
            holder.ivFoto.setImageBitmap(bmp);
        } else {
            holder.ivFoto.setImageResource(R.drawable.app_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onArticuloClick(art);
        });
    }

    @Override
    public int getItemCount() {
        return articulos != null ? articulos.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvCodigo, tvDescripcion, tvPrecioBs, tvPrecioUSD, tvStock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivArticuloFoto);
            tvCodigo = itemView.findViewById(R.id.tvArticuloCodigo);
            tvDescripcion = itemView.findViewById(R.id.tvArticuloDesc);
            tvPrecioBs = itemView.findViewById(R.id.tvArticuloPrecioBs);
            tvPrecioUSD = itemView.findViewById(R.id.tvArticuloPrecioUSD);
            tvStock = itemView.findViewById(R.id.tvArticuloStock);
        }
    }
}
