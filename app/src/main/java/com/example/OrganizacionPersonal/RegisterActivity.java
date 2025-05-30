package com.example.OrganizacionPersonal;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import androidx.activity.OnBackPressedCallback; // Importar esto para API 33+

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private FirebaseAuth mAuth;

    private EditText etRegEmail, etRegPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private ProgressBar regProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        regProgressBar = findViewById(R.id.regProgressBar);

        btnRegister.setOnClickListener(v -> registerUser());
        tvBackToLogin.setOnClickListener(v -> {
            finish(); // Simplemente cierra esta actividad para volver al login
            // Aplicar la animación inversa al volver a MainActivity
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false); // Desactiva temporalmente el callback
                getOnBackPressedDispatcher().onBackPressed(); // Dispara el comportamiento por defecto

                // Aplica la animación inversa
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }

    private void registerUser() {
        showProgressBar();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("El correo es requerido.");
            etRegEmail.requestFocus();
            hideProgressBar();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("La contraseña es requerida.");
            etRegPassword.requestFocus();
            hideProgressBar();
            return;
        }
        if (password.length() < 6) {
            etRegPassword.setError("La contraseña debe tener al menos 6 caracteres.");
            etRegPassword.requestFocus();
            hideProgressBar();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(RegisterActivity.this, "Registro exitoso. ¡Ahora puedes iniciar sesión!", Toast.LENGTH_LONG).show();
                        finish(); // Vuelve a MainActivity (login)
                        // Aplicar la animación al cerrar RegisterActivity y volver a MainActivity
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegisterActivity.this, "Este correo ya está registrado.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Fallo el registro: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                        hideProgressBar();
                    }
                });
    }

    private void showProgressBar() {
        regProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        regProgressBar.setVisibility(View.GONE);
    }
}