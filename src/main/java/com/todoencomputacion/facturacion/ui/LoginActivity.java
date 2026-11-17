package com.todoencomputacion.facturacion.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.todoencomputacion.facturacion.R;
import com.todoencomputacion.facturacion.data.DataManager;
import com.todoencomputacion.facturacion.models.Configuracion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private EditText etNombreSoporte, etConsultaSoporte;
    private TextView tvLiveClock, tvTelefonosSoporte, tvTasaUsdLogin, tvTasaEurLogin;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar datos predeterminados en teléfono si es primera vez y actualizar tasa BCV automáticamente
        Configuracion cfg = DataManager.getConfiguracion(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etNombreSoporte = findViewById(R.id.etNombreSoporte);
        etConsultaSoporte = findViewById(R.id.etConsultaSoporte);
        tvLiveClock = findViewById(R.id.tvLiveClockLogin);
        tvTelefonosSoporte = findViewById(R.id.tvTelefonosSoporte);
        tvTasaUsdLogin = findViewById(R.id.tvTasaUsdLogin);
        tvTasaEurLogin = findViewById(R.id.tvTasaEurLogin);
        Button btnLogin = findViewById(R.id.btnLogin);
        android.view.View btnHuella = findViewById(R.id.btnHuella);
        Button btnWhatsappSoporte = findViewById(R.id.btnWhatsappSoporte);

        mostrarTasasSuperiores(cfg);

        DataManager.actualizarTasaDesdeBCV(this, nuevaTasa -> {
            Configuracion actualizado = DataManager.getConfiguracion(this);
            mostrarTasasSuperiores(actualizado);
        });

        tvTelefonosSoporte.setText("📞 04241958013");

        iniciarReloj();

        btnLogin.setOnClickListener(v -> autenticar());
        btnHuella.setOnClickListener(v -> autenticarConHuella());
        btnWhatsappSoporte.setOnClickListener(v -> enviarSoporteWhatsapp());
    }

    private void mostrarTasasSuperiores(Configuracion cfg) {
        if (tvTasaUsdLogin != null) {
            tvTasaUsdLogin.setText(String.format(new Locale("es", "VE"), "Bs. %,.2f", cfg.tasaDolar));
        }
        if (tvTasaEurLogin != null) {
            double eur = cfg.tasaEuro > 0 ? cfg.tasaEuro : 1006.61;
            tvTasaEurLogin.setText(String.format(new Locale("es", "VE"), "Bs. %,.2f", eur));
        }
    }

    private void iniciarReloj() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy - hh:mm:ss a", new Locale("es", "VE"));
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                String txt = sdf.format(new Date());
                if (!txt.isEmpty()) {
                    txt = Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
                }
                tvLiveClock.setText(txt);
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    private void autenticar() {
        String u = etUsername.getText().toString().trim();
        String p = etPassword.getText().toString().trim();

        if (u.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        Configuracion cfg = DataManager.getConfiguracion(this);

        // Validar usuario MASTER
        if ((u.equalsIgnoreCase("mena") && p.equals("mena26")) ||
            (u.equalsIgnoreCase(cfg.usuarioMaster) && p.equals(cfg.claveMaster))) {
            DataManager.currentUser = "mena";
            DataManager.isMaster = true;
            Toast.makeText(this, "¡Bienvenido Administrador Master John Mena!", Toast.LENGTH_LONG).show();
            abrirMain();
            return;
        }

        // Validar usuario NORMAL (usuario / 1234)
        if ((u.equalsIgnoreCase("usuario") && p.equals("1234")) ||
            (u.equalsIgnoreCase(cfg.usuarioNormal) && p.equals(cfg.claveNormal))) {
            DataManager.currentUser = "usuario";
            DataManager.isMaster = false;
            Toast.makeText(this, "¡Bienvenido Usuario: usuario!", Toast.LENGTH_SHORT).show();
            abrirMain();
            return;
        }

        // Validar usuario CAJA (caja / 1234)
        String userCaja = (cfg.usuarioCaja != null && !cfg.usuarioCaja.isEmpty()) ? cfg.usuarioCaja : "caja";
        String passCaja = (cfg.claveCaja != null && !cfg.claveCaja.isEmpty()) ? cfg.claveCaja : "1234";
        if ((u.equalsIgnoreCase("caja") && p.equals("1234")) ||
            (u.equalsIgnoreCase(userCaja) && p.equals(passCaja))) {
            DataManager.currentUser = "caja";
            DataManager.isMaster = false;
            Toast.makeText(this, "¡Bienvenido Perfil Caja!", Toast.LENGTH_SHORT).show();
            abrirMain();
            return;
        }

        Toast.makeText(this, "Credenciales incorrectas. Verifique usuario y contraseña.", Toast.LENGTH_LONG).show();
    }

    private void autenticarConHuella() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this)
                    .setTitle("Autenticación con Huella")
                    .setSubtitle("Facturación Digital - Todo en Computación")
                    .setDescription("Coloque su huella digital en el sensor para ingresar")
                    .setNegativeButton("Cancelar", getMainExecutor(), (dialogInterface, i) -> {})
                    .build();

            CancellationSignal cancellationSignal = new CancellationSignal();
            biometricPrompt.authenticate(cancellationSignal, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    ingresarPorHuellaExitoso();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(LoginActivity.this, "Aviso: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(LoginActivity.this, "Huella no reconocida. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager fm = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (fm != null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints()) {
                CancellationSignal cancellationSignal = new CancellationSignal();
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Autenticación con Huella")
                        .setMessage("Coloque su huella digital en el sensor para ingresar al sistema...")
                        .setNegativeButton("Cancelar", (d, w) -> cancellationSignal.cancel())
                        .create();
                dialog.show();

                fm.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        dialog.dismiss();
                        ingresarPorHuellaExitoso();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this, "Huella no reconocida.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorMsgId, CharSequence errString) {
                        super.onAuthenticationError(errorMsgId, errString);
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this, errString, Toast.LENGTH_SHORT).show();
                    }
                }, null);
            } else {
                Toast.makeText(this, "Este dispositivo no tiene sensor de huella configurado.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void ingresarPorHuellaExitoso() {
        Configuracion cfg = DataManager.getConfiguracion(this);
        String u = etUsername.getText().toString().trim();
        if (u.equalsIgnoreCase("mena") || u.equalsIgnoreCase(cfg.usuarioMaster)) {
            DataManager.currentUser = "mena";
            DataManager.isMaster = true;
            Toast.makeText(this, "¡Huella verificada! Bienvenido Administrador Master", Toast.LENGTH_SHORT).show();
        } else if (u.equalsIgnoreCase("caja") || (cfg.usuarioCaja != null && u.equalsIgnoreCase(cfg.usuarioCaja))) {
            etUsername.setText("caja");
            etPassword.setText("1234");
            DataManager.currentUser = "caja";
            DataManager.isMaster = false;
            Toast.makeText(this, "¡Huella verificada! Bienvenido Perfil Caja", Toast.LENGTH_SHORT).show();
        } else {
            etUsername.setText("usuario");
            etPassword.setText("1234");
            DataManager.currentUser = "usuario";
            DataManager.isMaster = false;
            Toast.makeText(this, "¡Huella verificada! Bienvenido Usuario de Facturación", Toast.LENGTH_SHORT).show();
        }
        abrirMain();
    }

    private void enviarSoporteWhatsapp() {
        Configuracion cfg = DataManager.getConfiguracion(this);
        String empresa = (cfg.empresaNombre != null && !cfg.empresaNombre.isEmpty())
                ? cfg.empresaNombre : "TODO EN COMPUTACION C.A.";
        String nombre = etNombreSoporte.getText().toString().trim();
        if (nombre.isEmpty()) {
            String u = etUsername.getText().toString().trim();
            nombre = !u.isEmpty() ? u : "Usuario Facturación Digital";
        }
        String dispositivo = Build.MANUFACTURER.toUpperCase(Locale.ROOT) + " " + Build.MODEL
                + " (Android " + Build.VERSION.RELEASE + ")";
        String consulta = etConsultaSoporte.getText().toString().trim();
        if (consulta.isEmpty()) {
            consulta = "Solicitud de soporte técnico y asistencia en Facturación Digital";
        }

        String mensaje = "*SOPORTE AUTOMÁTICO - FACTURACIÓN DIGITAL*\n"
                + "*Empresa:* " + empresa + "\n"
                + "*Nombre:* " + nombre + "\n"
                + "*Dispositivo:* " + dispositivo + "\n"
                + "*Consulta:* " + consulta;

        String rawPhone = (cfg.telefono != null && !cfg.telefono.isEmpty()) ? cfg.telefono : "0424-1958013";
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            digits = "58" + digits.substring(1);
        } else if (!digits.startsWith("58") && digits.length() == 10) {
            digits = "58" + digits;
        }

        try {
            String url = "https://wa.me/" + digits + "?text=" + Uri.encode(mensaje);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir WhatsApp en este dispositivo.", Toast.LENGTH_LONG).show();
        }
    }

    private void abrirMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}