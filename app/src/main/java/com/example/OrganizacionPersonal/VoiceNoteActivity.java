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
import android.util.Log; // Añadir import para Log

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.activity.OnBackPressedCallback;

public class VoiceNoteActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1000;
    private static final String TAG = "VoiceNoteActivity"; // Para logs

    private MediaRecorder recorder;
    private String fileName;
    private Button btnRecord, btnStop;

    private String audioDownloadUrl = null;

    private ActivityResultLauncher<Intent> addEventLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_note); // Asegúrate de que este sea el nombre correcto de tu layout

        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);

        btnRecord.setEnabled(false);
        btnStop.setEnabled(false);

        addEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Evento de nota de voz agregado al calendario.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Creación de evento de nota de voz cancelada.", Toast.LENGTH_SHORT).show();
                    }
                });

        if (checkPermissions()) {
            btnRecord.setEnabled(true);
        } else {
            requestPermissions();
        }

        btnRecord.setOnClickListener(v -> startRecording());
        btnStop.setOnClickListener(v -> stopAndUpload());

        // Manejo del botón de retroceso con animación
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Aplica la animación al salir de la actividad
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                // Llama al comportamiento por defecto de retroceso, que finalizará esta actividad.
                super.getClass();
            }
        });
    }

    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        // Para versiones antiguas, también verifica WRITE_EXTERNAL_STORAGE. Para API 29+ con getExternalFilesDir, no es estrictamente necesario.
        boolean storagePermission = true;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Para API 29 y anteriores
            storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return recordPermission == PackageManager.PERMISSION_GRANTED && storagePermission;
    }

    private void requestPermissions() {
        // Pedir RECORD_AUDIO y WRITE_EXTERNAL_STORAGE para versiones antiguas
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        } else { // Para API 30+ solo RECORD_AUDIO si el almacenamiento es scoped
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
                btnRecord.setEnabled(true);
            } else {
                Toast.makeText(this, "Permisos denegados. Algunas funciones no estarán disponibles.", Toast.LENGTH_LONG).show();
                btnRecord.setEnabled(false);
            }
        }
    }

    private void startRecording() {
        // Usa getExternalFilesDir() para guardar en un lugar específico de la app que no requiere permisos de almacenamiento externos en Android 10+
        File audioDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "NotasVoz");
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            Toast.makeText(this, "No se pudo crear el directorio para guardar audio", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error al crear directorio: " + audioDir.getAbsolutePath());
            return;
        }

        fileName = audioDir.getAbsolutePath() + "/nota_" + System.currentTimeMillis() + ".3gp";
        Log.d(TAG, "Iniciando grabación a: " + fileName);

        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(fileName);

            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Error al iniciar grabación: " + e.getMessage(), e);
            Toast.makeText(this, "Error al iniciar grabación: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseRecorder(); // Asegurarse de liberar recursos en caso de error
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
            Toast.makeText(this, "Grabación finalizada. Subiendo...", Toast.LENGTH_SHORT).show();
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);

            releaseRecorder(); // Liberar recursos después de detener
            uploadToFirebase();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error al detener grabación: " + e.getMessage(), e);
            Toast.makeText(this, "Error al detener grabación. Intenta de nuevo.", Toast.LENGTH_LONG).show();
            releaseRecorder(); // Liberar recursos si hay un error al detener
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
            Log.d(TAG, "MediaRecorder liberado.");
        }
    }

    private void uploadToFirebase() {
        File audioFile = new File(fileName);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Archivo de audio no encontrado para subir.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Archivo de audio no encontrado: " + fileName);
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
                        Log.d(TAG, "URL de descarga obtenida: " + audioDownloadUrl);
                        crearEventoEnCalendario(nombreArchivo, audioDownloadUrl);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al obtener URL de descarga", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al obtener URL de descarga: " + e.getMessage(), e);
                        crearEventoEnCalendario(nombreArchivo, null); // Crear evento sin URL si falla la obtención
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al subir nota de voz a Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error al subir a Firebase: " + e.getMessage(), e);
                });
    }

    private void crearEventoEnCalendario(String nombreArchivo, String downloadUrl) {
        Calendar beginTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.MINUTE, 30); // Evento de 30 minutos

        String description = "Nota de voz grabada en '" + getString(R.string.app_name) + "'.";
        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            description += "\nEnlace de audio: " + downloadUrl;
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Revisar Nota de Voz: " + nombreArchivo)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.ALL_DAY, false)
                .putExtra(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                .putExtra(CalendarContract.Events.VISIBLE, 0) // Visible en la app de calendario
                .putExtra(CalendarContract.Events.GUESTS_CAN_MODIFY, true)
                .putExtra(CalendarContract.Events.CALENDAR_ID, 1) // Puedes probar con ID=1, o dejar que el usuario elija
                .putExtra(CalendarContract.Events.EVENT_LOCATION, "App " + getString(R.string.app_name));

        // Puedes añadir recordatorios si lo deseas
        // intent.putExtra(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        // intent.putExtra(CalendarContract.Reminders.MINUTES, 10);

        if (intent.resolveActivity(getPackageManager()) != null) {
            addEventLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No hay aplicación de calendario para crear el evento.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No se encontró actividad de calendario para crear evento.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Liberar MediaRecorder si la actividad se detiene y aún está activo
        releaseRecorder();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurarse de liberar en onDestroy también
        releaseRecorder();
    }
}