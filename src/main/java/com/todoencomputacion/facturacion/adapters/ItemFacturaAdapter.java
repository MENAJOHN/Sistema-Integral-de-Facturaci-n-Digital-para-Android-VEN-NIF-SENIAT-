package com.todoencomputacion.facturacion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.models.Articulo;
import com.todoencomputacion.facturacion.models.ItemFactura;

import java.util.List;
import java.util.Locale;

public class ItemFacturaAdapter extends RecyclerView.Adapter<ItemFacturaAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onIncrease(int position);
        void onDecrease(int position);
        void onEditPrice(int position);
        void onRemove(int position);
    }

    private final Context context;
    private final List<ItemFactura> items;
    private final OnItemActionListener listener;
    private boolean permitirEditarPrecio = false;

    public ItemFacturaAdapter(Context context, List<ItemFactura> items, boolean permitirEditarPrecio, OnItemActionListener listener) {
        this.context = context;
        this.items = items;
        this.permitirEditarPrecio = permitirEditarPrecio;
        this.listener = listener;
    }

    public void setPermitirEditarPrecio(boolean permitir) {
        this.permitirEditarPrecio = permitir;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_linea_factura, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemFactura it = items.get(position);
        holder.tvDesc.setText(it.descripcion);
        holder.tvCantidad.setText(String.valueOf(it.cantidad));
        holder.tvPrecio.setText(String.format(Locale.US, "P. Venta: Bs. %,.2f ($ %,.2f) • IVA: %s",
                it.precioUnitarioBs, it.precioUnitarioUSD, it.tipoIva));
        holder.tvSubtotal.setText(String.format(Locale.US, "Bs. %,.2f", it.totalBs));

        boolean puedeEditarEsteItem = permitirEditarPrecio || Articulo.CODIGO_GENERICO.equalsIgnoreCase(it.codigoArticulo);
        if (holder.btnEditPrecio != null) {
            holder.btnEditPrecio.setVisibility(puedeEditarEsteItem ? View.VISIBLE : View.GONE);
            holder.btnEditPrecio.setOnClickListener(v -> {
                if (listener != null) listener.onEditPrice(holder.getAdapterPosition());
            });
        }

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onIncrease(holder.getAdapterPosition());
        });
        holder.btnMinus.setOnClickListener(v -> {
            if (listener != null) listener.onDecrease(holder.getAdapterPosition());
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvCantidad, tvPrecio, tvSubtotal;
        ImageButton btnPlus, btnMinus, btnDelete;
        Button btnEditPrecio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvItemDesc);
            tvCantidad = itemView.findViewById(R.id.tvItemCantidad);
            tvPrecio = itemView.findViewById(R.id.tvItemPrecio);
            tvSubtotal = itemView.findViewById(R.id.tvItemSubtotal);
            btnPlus = itemView.findViewById(R.id.btnItemPlus);
            btnMinus = itemView.findViewById(R.id.btnItemMinus);
            btnEditPrecio = itemView.findViewById(R.id.btnItemEditPrecio);
            btnDelete = itemView.findViewById(R.id.btnItemDelete);
        }
    }
}
