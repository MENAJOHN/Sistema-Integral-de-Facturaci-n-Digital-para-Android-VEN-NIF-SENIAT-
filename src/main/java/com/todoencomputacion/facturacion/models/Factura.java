package com.todoencomputacion.facturacion.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Factura implements Serializable {
    public int numeroFactura;
    public int numeroControl;
    public String tipoDocumento = "FACTURA";
    public String referenciaExtra = "";
    public String fecha;
    public String hora;
    public String clienteCodigo;
    public String clienteNombre;
    public String clienteRif;
    public String clienteDireccion;
    public String clienteTelefono = "";
    public String clienteCorreo = "";
    public String vendedorCodigo;
    public String vendedorNombre;
    public List<ItemFactura> items = new ArrayList<>();
    public double subtotalBs;
    public double exentoBs;
    public double ivaBs;
    public double totalBs;
    public double subtotalUSD;
    public double totalUSD;
    public double tasaDolar;
    public String usuarioEmisor;

    public Factura() {}
}