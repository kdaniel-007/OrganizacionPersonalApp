package com.example.OrganizacionPersonal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView; // Agregado para el texto de registro
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth; // Importar FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException; // Importar para manejar errores de registro

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvRegister; // Agregado para el texto de registro
    private FirebaseAuth mAuth; // Instancia de FirebaseAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ocultar ActionBar si es necesario
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvRegister = findViewById(R.id.tvRegister); // Inicializar el TextView de registro

        // Configurar listener del botón de Login
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Configurar listener del texto "Registrate"
        tvRegister.setOnClickListener(v -> attemptRegistration());
    }

    // Opcional: Para mantener la sesión del usuario si ya inició sesión
    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            // Usuario ya ha iniciado sesión, navegar a HomeActivity
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("El correo es requerido");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Contraseña es requerida");
            etPassword.requestFocus();
            return;
        }

        // Mostrar progreso
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        tvRegister.setEnabled(false); // Deshabilitar también el registro

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    tvRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        Intent activityHome = new Intent(this, HomeActivity.class);
                        startActivity(activityHome);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Fallo el inicio de sesión: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para manejar un registro básico
    private void attemptRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("El correo es requerido");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Contraseña es requerida");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        // Mostrar progreso
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        tvRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    tvRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Registro exitoso. ¡Inicia sesión!", Toast.LENGTH_LONG).show();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(MainActivity.this, "Este correo ya está registrado.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Fallo el registro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}