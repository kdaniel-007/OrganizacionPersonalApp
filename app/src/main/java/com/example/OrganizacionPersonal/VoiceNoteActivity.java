package com.example.OrganizacionPersonal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceNoteActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private TextView tvTextoNota;
    private Button btnSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_note);

        tvTextoNota = findViewById(R.id.tvTextoNota);
        btnSpeech = findViewById(R.id.btnSpeech);

        btnSpeech.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "No se puede iniciar el reconocimiento de voz", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && result.size() > 0) {
                String textoReconocido = result.get(0);
                tvTextoNota.setText(textoReconocido);
                procesarTextoReconocido(textoReconocido);
            }
        }
    }

    private void procesarTextoReconocido(String texto) {
        Calendar beginTime = Calendar.getInstance();

        texto = texto.toLowerCase();

        // Día específico como "el viernes"
        Pattern diaPattern = Pattern.compile("el (lunes|martes|miércoles|jueves|viernes|sábado|domingo)");
        Matcher diaMatcher = diaPattern.matcher(texto);
        if (diaMatcher.find()) {
            String diaTexto = diaMatcher.group(1);
            int diaSemana = obtenerDiaSemana(diaTexto);
            if (diaSemana != -1) {
                int hoy = beginTime.get(Calendar.DAY_OF_WEEK);
                int diferenciaDias = (diaSemana - hoy + 7) % 7;
                beginTime.add(Calendar.DAY_OF_MONTH, diferenciaDias);
            }
        } else if (texto.contains("mañana")) {
            beginTime.add(Calendar.DAY_OF_MONTH, 1);
        } else if (texto.contains("pasado mañana")) {
            beginTime.add(Calendar.DAY_OF_MONTH, 2);
        }

        // Soporte para "a las 3", "a las 14:30", "a las 3 y media"
        Pattern horaPattern = Pattern.compile("a las (\\d{1,2})(?::(\\d{2}))?(?:\\s*y\\s*media)?");
        Matcher horaMatcher = horaPattern.matcher(texto);

        if (horaMatcher.find()) {
            int hora = Integer.parseInt(horaMatcher.group(1));
            int minuto = 0;

            if (horaMatcher.group(2) != null) {
                minuto = Integer.parseInt(horaMatcher.group(2));
            } else if (texto.contains("y media")) {
                minuto = 30;
            }

            beginTime.set(Calendar.HOUR_OF_DAY, hora);
            beginTime.set(Calendar.MINUTE, minuto);
            beginTime.set(Calendar.SECOND, 0);
        } else if (texto.contains("esta noche")) {
            beginTime.set(Calendar.HOUR_OF_DAY, 20);
            beginTime.set(Calendar.MINUTE, 0);
        } else if (texto.contains("en la tarde")) {
            beginTime.set(Calendar.HOUR_OF_DAY, 15);
            beginTime.set(Calendar.MINUTE, 0);
        } else if (texto.contains("en la mañana")) {
            beginTime.set(Calendar.HOUR_OF_DAY, 9);
            beginTime.set(Calendar.MINUTE, 0);
        } else {
            // Buscar "dentro de X horas"
            Pattern dentroPattern = Pattern.compile("dentro de (\\d{1,2}) horas");
            Matcher dentroMatcher = dentroPattern.matcher(texto);
            if (dentroMatcher.find()) {
                int horas = Integer.parseInt(dentroMatcher.group(1));
                beginTime.add(Calendar.HOUR_OF_DAY, horas);
            } else {
                beginTime.add(Calendar.MINUTE, 2);
            }
        }

        crearEventoDesdeTexto(texto, beginTime);
        programarNotificacion(beginTime, texto);
    }

    private int obtenerDiaSemana(String diaTexto) {
        switch (diaTexto) {
            case "domingo": return Calendar.SUNDAY;
            case "lunes": return Calendar.MONDAY;
            case "martes": return Calendar.TUESDAY;
            case "miércoles": return Calendar.WEDNESDAY;
            case "jueves": return Calendar.THURSDAY;
            case "viernes": return Calendar.FRIDAY;
            case "sábado": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    private void crearEventoDesdeTexto(String texto, Calendar beginTime) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Inicia sesión para guardar la nota", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Evento nuevoEvento = new Evento("Nota de voz", texto, beginTime.getTime());

        db.collection("users")
                .document(uid)
                .collection("eventos")
                .add(nuevoEvento)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Nota guardada en Firestore", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra esta Activity y regresa al CalendarFragment
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                });

    }


    private void programarNotificacion(Calendar calendar, String mensaje) {
        Intent intent = new Intent(this, NotificacionReceiver.class);
        intent.putExtra("mensaje", "Recordatorio: " + mensaje);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
}
