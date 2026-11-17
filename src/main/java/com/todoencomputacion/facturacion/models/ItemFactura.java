package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class ItemFactura implements Serializable {
    public String codigoArticulo;
    public String descripcion;
    public int cantidad;
    public double precioUnitarioBs;
    public double precioUnitarioUSD;
    public double totalBs;
    public double totalUSD;
    public String tipoArticulo = "V";
    public String tipoIva = "16%";
    public double porcentajeIva = 16.0;

    public ItemFactura() {}

    public ItemFactura(String codigoArticulo, String descripcion, int cantidad, double precioUnitarioBs, double precioUnitarioUSD) {
        this(codigoArticulo, descripcion, cantidad, precioUnitarioBs, precioUnitarioUSD, "V", "16%", 16.0);
    }

    public ItemFactura(String codigoArticulo, String descripcion, int cantidad,
                       double precioUnitarioBs, double precioUnitarioUSD,
                       String tipoArticulo, String tipoIva, double porcentajeIva) {
        this.codigoArticulo = codigoArticulo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitarioBs = precioUnitarioBs;
        this.precioUnitarioUSD = precioUnitarioUSD;
        this.totalBs = cantidad * precioUnitarioBs;
        this.totalUSD = cantidad * precioUnitarioUSD;
        this.tipoArticulo = tipoArticulo != null ? tipoArticulo : "V";
        this.tipoIva = tipoIva != null ? tipoIva : "16%";
        this.porcentajeIva = porcentajeIva;
    }

    public String getCodigoAlicuotaPdf() {
        if ("EXENTO".equalsIgnoreCase(tipoIva) || porcentajeIva <= 0) return "E";
        if (Math.abs(porcentajeIva - 8.0) < 0.01) return "R";
        if (Math.abs(porcentajeIva - 32.0) < 0.01) return "A";
        return "G";
    }
}
