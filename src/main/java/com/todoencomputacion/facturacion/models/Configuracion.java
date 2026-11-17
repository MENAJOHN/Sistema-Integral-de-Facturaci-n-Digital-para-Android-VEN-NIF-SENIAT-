package com.todoencomputacion.facturacion.models;

import java.io.Serializable;

public class Configuracion implements Serializable {
    public String empresaNombre;
    public String eslogan;
    public String rif;
    public String telefono;
    public String correo;
    public String direccion;
    public double tasaDolar;
    public double tasaEuro;
    public double porcentajeIva;
    public int proximoNumeroFactura;
    public int proximoNumeroControl;
    public String monedaSimbolo;
    public String monedaSecundaria;
    public String usuarioMaster;
    public String claveMaster;
    public String usuarioNormal;
    public String claveNormal;
    public String usuarioCaja;
    public String claveCaja;
    public String usuarioImprenta;
    public String claveImprenta;
    public String tokenImprenta;
    public boolean permitirStockNegativo;
    public boolean permitirEditarPrecio;
    public PermisosPerfil permisosUsuario;
    public PermisosPerfil permisosCaja;

    public Configuracion() {
        this.empresaNombre = "TODO EN COMPUTACION C.A.";
        this.eslogan = "Soluciones Inteligentes - Tu Facturación Online";
        this.rif = "J-40982314-0";
        this.telefono = "04241958013";
        this.correo = "todoencomputacioncl@gmail.com";
        this.direccion = "Caracas, La California, Venezuela";
        this.tasaDolar = 899.27;
        this.tasaEuro = 1006.61;
        this.porcentajeIva = 16.0;
        this.proximoNumeroFactura = 1001;
        this.proximoNumeroControl = 501;
        this.monedaSimbolo = "Bs.";
        this.monedaSecundaria = "$";
        this.usuarioMaster = "mena";
        this.claveMaster = "mena26";
        this.usuarioNormal = "usuario";
        this.claveNormal = "1234";
        this.usuarioCaja = "caja";
        this.claveCaja = "1234";
        this.usuarioImprenta = "";
        this.claveImprenta = "";
        this.tokenImprenta = "";
        this.permitirStockNegativo = false;
        this.permitirEditarPrecio = false;
        this.permisosUsuario = new PermisosPerfil();
        this.permisosCaja = new PermisosPerfil();
    }
}
