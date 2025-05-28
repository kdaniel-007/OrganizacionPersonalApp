package com.example.OrganizacionPersonal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import com.google.firebase.storage.FirebaseStorage;


public class VoiceNoteActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1000;
    private MediaRecorder recorder;
    private String fileName;
    private Button btnRecord, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_note);

        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);

        if (!checkPermissions()) {
            requestPermissions();
        }

        btnRecord.setOnClickListener(v -> startRecording());
        btnStop.setOnClickListener(v -> stopAndUpload());
    }

    private void startRecording() {
        fileName = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/nota_" + System.currentTimeMillis() + ".3gp";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAndUpload() {
        recorder.stop();
        recorder.release();
        recorder = null;
        Toast.makeText(this, "Grabación finalizada", Toast.LENGTH_SHORT).show();
        btnStop.setEnabled(false);

        uploadToFirebase(); // Vamos a crear esto después
    }

    private void uploadToFirebase() {
        File audioFile = new File(fileName);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = Uri.fromFile(audioFile);
        String nombreArchivo = audioFile.getName();

        FirebaseStorage.getInstance()
                .getReference()
                .child("notas/" + nombreArchivo)
                .putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(this, "Subido a Firebase", Toast.LENGTH_SHORT).show();
                    crearEventoEnCalendario(nombreArchivo);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void crearEventoEnCalendario(String nombreArchivo) {
        Calendar beginTime = Calendar.getInstance();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Nota de voz - " + nombreArchivo)
                .putExtra(CalendarContract.Events.DESCRIPTION, "Revisar grabación")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis());

        startActivity(intent);
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
}
