package com.example.OrganizacionPersonal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Importar la clase necesaria para el manejo del botón de retroceso
import androidx.activity.OnBackPressedCallback;

public class VoiceNoteActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1000;

    private MediaRecorder recorder;
    private String fileName;
    private Button btnRecord, btnStop;

    private String audioDownloadUrl = null;

    private ActivityResultLauncher<Intent> addEventLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_note);

        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);

        // Inicializar botones (asegurarse de que estén en el estado correcto al inicio)
        btnRecord.setEnabled(false); // Iniciar deshabilitado hasta tener permisos
        btnStop.setEnabled(false);

        // --- Registrar el ActivityResultLauncher ---
        addEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Evento de nota de voz agregado al calendario.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Creación de evento de nota de voz cancelada.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Comprobar y solicitar permisos de grabación
        if (checkPermissions()) {
            btnRecord.setEnabled(true);
        } else {
            requestPermissions();
        }

        btnRecord.setOnClickListener(v -> startRecording());
        btnStop.setOnClickListener(v -> stopAndUpload());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed(); // Esto disparará el comportamiento por defecto
                getOnBackPressedDispatcher().onBackPressed(); // Esto disparará el comportamiento por defecto
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return recordPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de grabación concedido", Toast.LENGTH_SHORT).show();
                btnRecord.setEnabled(true);
            } else {
                Toast.makeText(this, "Permiso de grabación denegado. No se puede grabar.", Toast.LENGTH_LONG).show();
                btnRecord.setEnabled(false);
            }
        }
    }

    private void startRecording() {
        File musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null || !musicDir.exists() && !musicDir.mkdirs()) {
            Toast.makeText(this, "No se pudo crear el directorio para guardar audio", Toast.LENGTH_LONG).show();
            return;
        }

        fileName = musicDir.getAbsolutePath() + "/nota_" + System.currentTimeMillis() + ".3gp";

        try {
            recorder = new MediaRecorder(this);

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(fileName);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar grabación: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    private void stopAndUpload() {
        if (recorder == null) {
            Toast.makeText(this, "No hay grabación activa.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            Toast.makeText(this, "Grabación finalizada", Toast.LENGTH_SHORT).show();
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);

            uploadToFirebase();
        } catch (RuntimeException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al detener grabación. Intenta de nuevo.", Toast.LENGTH_LONG).show();
            recorder = null;
        }
    }

    private void uploadToFirebase() {
        File audioFile = new File(fileName);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Archivo de audio no encontrado para subir.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = Uri.fromFile(audioFile);
        String nombreArchivo = audioFile.getName();

        StorageReference audioRef = FirebaseStorage.getInstance().getReference().child("notas/" + nombreArchivo);

        audioRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(this, "Nota de voz subida a Firebase Storage", Toast.LENGTH_SHORT).show();
                    audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        audioDownloadUrl = uri.toString();
                        crearEventoEnCalendario(nombreArchivo, audioDownloadUrl);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al obtener URL de descarga", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        crearEventoEnCalendario(nombreArchivo, null);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al subir nota de voz a Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void crearEventoEnCalendario(String nombreArchivo, String downloadUrl) {
        Calendar beginTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.MINUTE, 30);

        String description = "Nota de voz grabada en 'Organización Personal'.";
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            description += "\nEnlace de audio: " + downloadUrl;
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Revisar Nota de Voz - " + nombreArchivo)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.ALL_DAY, false)
                .putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                .putExtra(CalendarContract.Events.VISIBLE, 0)
                .putExtra(CalendarContract.Events.GUESTS_CAN_MODIFY, true)
                .putExtra(CalendarContract.Events.CALENDAR_ID, 1)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, "App Organización Personal");

        intent.putExtra(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        intent.putExtra(CalendarContract.Reminders.MINUTES, 10);

        addEventLauncher.launch(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void deleteAudioFile() {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
            Toast.makeText(this, "Archivo local eliminado.", Toast.LENGTH_SHORT).show();
        }
    }

}