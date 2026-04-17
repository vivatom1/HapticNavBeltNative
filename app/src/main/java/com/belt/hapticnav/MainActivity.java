package com.belt.hapticnav;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BluetoothSPPManager.BluetoothCallback {

    private BluetoothSPPManager btManager;
    private TextView tvStatus;
    private LocationManager locationManager;
    private SpeechRecognizer speechRecognizer;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnF = findViewById(R.id.btnForward);
        Button btnL = findViewById(R.id.btnLeft);
        Button btnR = findViewById(R.id.btnRight);
        Button btnS = findViewById(R.id.btnStop);
        Button btnH = findViewById(R.id.btnHazard);
        Button btnVoice = findViewById(R.id.btnVoice);

        btManager = new BluetoothSPPManager(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        setupSpeechRecognizer();

        btnConnect.setOnClickListener(v -> checkPermissionsAndConnect());

        btnF.setOnClickListener(v -> btManager.sendCommand("F"));
        btnL.setOnClickListener(v -> btManager.sendCommand("L"));
        btnR.setOnClickListener(v -> btManager.sendCommand("R"));
        btnS.setOnClickListener(v -> btManager.sendCommand("S"));
        btnH.setOnClickListener(v -> btManager.sendCommand("H"));

        btnVoice.setOnClickListener(v -> startVoiceRecording());
    }

    private void checkPermissionsAndConnect() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        };
        boolean allGranted = true;
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            btManager.connect();
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String locData = String.format("LAT:%.4f,LON:%.4f", location.getLatitude(), location.getLongitude());
                btManager.sendCommand(locData);
            }
        });
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    String destination = data.get(0);
                    Toast.makeText(MainActivity.this, "Navigate to: " + destination, Toast.LENGTH_SHORT).show();
                    // Maps API integration to be called here
                }
            }
            @Override public void onReadyForSpeech(Bundle bundle) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] bytes) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int i) {
                Toast.makeText(MainActivity.this, "Voice input failed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onPartialResults(Bundle bundle) {}
            @Override public void onEvent(int i, Bundle bundle) {}
        });
    }

    private void startVoiceRecording() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        runOnUiThread(() -> {
            tvStatus.setText("Status: " + message);
            tvStatus.setTextColor(connected ? 0xFF4CAF50 : 0xFFFF5252);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btManager.close();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
