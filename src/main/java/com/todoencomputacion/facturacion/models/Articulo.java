package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class Articulo implements Serializable {
    public static final String CODIGO_GENERICO = "SERV-GEN";

    public String codigo;
    public String descripcion;
    public double precioUSD;
    public double precioBs;
    public int stock;
    public String imagen;
    // Tipo de artículo: "V" = Venta (maneja stock), "S" = Servicio (no lleva stock)
    public String tipoArticulo = "V";
    // Tipo de IVA: "16%", "8%", "32%", "EXENTO"
    public String tipoIva = "16%";
    public double porcentajeIva = 16.0;
    public boolean esGenerico = false;

    public Articulo() {}

    public Articulo(String codigo, String descripcion, double precioUSD, double precioBs, int stock, String imagen) {
        this(codigo, descripcion, precioUSD, precioBs, stock, imagen, "V", "16%", 16.0);
    }

    public Articulo(String codigo, String descripcion, double precioUSD, double precioBs, int stock,
                    String imagen, String tipoArticulo, String tipoIva, double porcentajeIva) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precioUSD = precioUSD;
        this.precioBs = precioBs;
        this.stock = stock;
        this.imagen = imagen;
        this.tipoArticulo = (tipoArticulo != null && !tipoArticulo.isEmpty()) ? tipoArticulo : "V";
        this.tipoIva = (tipoIva != null && !tipoIva.isEmpty()) ? tipoIva : "16%";
        this.porcentajeIva = porcentajeIva;
        this.esGenerico = CODIGO_GENERICO.equalsIgnoreCase(codigo);
    }

    public boolean esArticuloGenerico() {
        return esGenerico || CODIGO_GENERICO.equalsIgnoreCase(codigo);
    }

    public boolean manejaStock() {
        if (esArticuloGenerico()) return false;
        return !"S".equalsIgnoreCase(tipoArticulo) && !"SERVICIO".equalsIgnoreCase(tipoArticulo);
    }

    public boolean esExento() {
        return "EXENTO".equalsIgnoreCase(tipoIva) || porcentajeIva <= 0;
    }

    public String getEtiquetaTipo() {
        if (esArticuloGenerico()) return "Servicio Genérico (Descripción y Precio Libre)";
        return manejaStock() ? "Tipo V (Venta)" : "Servicio (Sin Stock)";
    }

    public String getEtiquetaIva() {
        if (esExento()) return "Exento (0%)";
        if (Math.abs(porcentajeIva - 8.0) < 0.01) return "IVA 8%";
        if (Math.abs(porcentajeIva - 32.0) < 0.01) return "IVA 32%";
        return "IVA 16%";
    }

    @Override
    public String toString() {
        return codigo + " - " + descripcion;
    }
}
