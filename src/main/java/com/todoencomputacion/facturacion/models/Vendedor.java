package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class Vendedor implements Serializable {
    public String codigo;
    public String nombre;
    public String cedula = "";
    public String direccion = "";
    public String telefono;
    public String correo;
    public double comision;
    public String foto = "";

    public Vendedor() {}

    public Vendedor(String codigo, String nombre, String telefono, String correo, double comision) {
        this(codigo, nombre, "", "", telefono, correo, comision, "");
    }

    public Vendedor(String codigo, String nombre, String cedula, String direccion,
                    String telefono, String correo, double comision, String foto) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cedula = cedula != null ? cedula : "";
        this.direccion = direccion != null ? direccion : "";
        this.telefono = telefono != null ? telefono : "";
        this.correo = correo != null ? correo : "";
        this.comision = comision;
        this.foto = foto != null ? foto : "";
    }

    @Override
    public String toString() {
        return nombre + " (" + codigo + ")";
    }
}
