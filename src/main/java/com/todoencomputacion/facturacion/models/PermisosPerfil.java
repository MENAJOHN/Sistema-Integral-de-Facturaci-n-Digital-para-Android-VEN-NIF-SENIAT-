package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class PermisosPerfil implements Serializable {
    // Documentos de Emisión
    public boolean factura = true;
    public boolean facturaTerceros = true;
    public boolean notaCredito = true;
    public boolean notaCreditoParcial = true;
    public boolean notaDebito = true;
    public boolean ordenEntrega = true;
    public boolean ordenEntregaIndividual = true;
    public boolean guiaDespacho = true;

    // Catálogos
    public boolean articulos = true;
    public boolean clientes = true;
    public boolean vendedores = true;

    // Reportes
    public boolean repHistorial = true;
    public boolean repVentasGeneral = true;
    public boolean repVendedores = true;
    public boolean repInventario = true;
    public boolean repNotasGuias = true;

    public PermisosPerfil() {
    }
}

