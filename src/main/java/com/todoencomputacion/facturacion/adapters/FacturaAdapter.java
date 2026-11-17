package com.todoencomputacion.facturacion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.models.Factura;

import java.util.List;
import java.util.Locale;

public class FacturaAdapter extends RecyclerView.Adapter<FacturaAdapter.ViewHolder> {

    public interface OnFacturaClickListener {
        void onFacturaClick(Factura factura);
    }

    private final Context context;
    private List<Factura> facturas;
    private final OnFacturaClickListener listener;

    public FacturaAdapter(Context context, List<Factura> facturas, OnFacturaClickListener listener) {
        this.context = context;
        this.facturas = facturas;
        this.listener = listener;
    }

    public void updateList(List<Factura> newList) {
        this.facturas = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_factura, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Factura f = facturas.get(position);
        String tipo = (f.tipoDocumento != null && !f.tipoDocumento.isEmpty()) ? f.tipoDocumento : "FACTURA";
        holder.tvNumero.setText(String.format(Locale.US, "%s #%06d", tipo, f.numeroFactura));
        holder.tvControl.setText(String.format(Locale.US, "Control: %06d", f.numeroControl));
        holder.tvFechaHora.setText(f.fecha + " " + f.hora);
        holder.tvCliente.setText(f.clienteNombre + " (" + f.clienteRif + ")");
        holder.tvTotalBs.setText(String.format(Locale.US, "Bs. %,.2f", f.totalBs));
        holder.tvTotalUSD.setText(String.format(Locale.US, "$ %,.2f", f.totalUSD));
        holder.tvEmisor.setText("Por: " + f.usuarioEmisor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFacturaClick(f);
        });
    }

    @Override
    public int getItemCount() {
        return facturas != null ? facturas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero, tvControl, tvFechaHora, tvCliente, tvTotalBs, tvTotalUSD, tvEmisor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvFacturaNumero);
            tvControl = itemView.findViewById(R.id.tvFacturaControl);
            tvFechaHora = itemView.findViewById(R.id.tvFacturaFechaHora);
            tvCliente = itemView.findViewById(R.id.tvFacturaCliente);
            tvTotalBs = itemView.findViewById(R.id.tvFacturaTotalBs);
            tvTotalUSD = itemView.findViewById(R.id.tvFacturaTotalUSD);
            tvEmisor = itemView.findViewById(R.id.tvFacturaEmisor);
        }
    }
}