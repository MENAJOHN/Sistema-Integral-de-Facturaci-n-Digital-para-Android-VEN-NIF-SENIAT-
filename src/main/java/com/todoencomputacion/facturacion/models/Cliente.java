package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class Cliente implements Serializable {
    public String codigo;
    public String nombre;
    public String rif;
    public String telefono;
    public String correo;
    public String direccion;

    public Cliente() {}

    public Cliente(String codigo, String nombre, String rif, String telefono, String correo, String direccion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.rif = rif;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
    }

    @Override
    public String toString() {
        return nombre + " (" + rif + ")";
    }
}