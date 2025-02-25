package com.trentseed.bmw_rpi_ibus_controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothConnectionManager;
import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothHelper;
import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothInterface;
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig;
import com.trentseed.bmw_rpi_ibus_controller.common.VoiceCommand;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Activity that handles presents core functionality to user.
 * @author Trent
 */
public class ActivityMain extends AppCompatActivity implements BluetoothConnectionManager.ConnectionListener {

    ImageView ivBmwEmblem;
    ImageView ivBtnRadio;
    ImageView ivBtnMaps;
    ImageView ivBtnMedia;
    ImageView ivBtnDevices;
    ImageView ivBtnFeatures;
    ImageView ivBtnNew3;
    ImageView ivBtnGear;
    ImageView ivBtnVoice;
    private ImageView ivBluetoothStatus;
    ProgressBar pbConnecting;
    TextView tvDateTime;

    BroadcastReceiver _broadcastReceiver;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    SimpleDateFormat _sdfWatchDate = new SimpleDateFormat("dd/MM", Locale.ENGLISH);

    private static final Logger logger = LogConfig.getLogger();
    private BluetoothHelper bluetoothHelper;
    private BluetoothInterface mBluetoothInterface;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onNewIntent(getIntent());
        setContentView(R.layout.activity_main);
        // Pass the directory to store log files
        File logDir = getExternalFilesDir(null);
        LogConfig.configure(logDir);

        logger.info("onCreate: Activity created");

        // get layout objects
        ivBmwEmblem = findViewById(R.id.ivBMWEmblem);
        ivBtnRadio = findViewById(R.id.ivBtnRadio);
        ivBtnMaps = findViewById(R.id.ivBtnMaps);
        ivBtnMedia = findViewById(R.id.ivBtnMedia);
        ivBtnDevices = findViewById(R.id.ivBtnDevices);
        ivBtnFeatures = findViewById(R.id.ivBtnFeatures);
        ivBtnNew3 = findViewById(R.id.ivBtnNew3);
        ivBtnVoice = findViewById(R.id.ivBtnMic);
        ivBtnGear = findViewById(R.id.ivBtnGear);
        ivBluetoothStatus = findViewById(R.id.ivBluetoothStatus);
        pbConnecting = findViewById(R.id.pbBluetoothConnecting);
        tvDateTime = findViewById(R.id.tvDateTime);

        // bind click handlers to layout objects
        ivBtnMaps.setOnClickListener(v -> {
            // launch Google Maps
            String uri = "http://maps.google.com/maps";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            ActivityMain.this.startActivity(intent);
        });
        ivBtnRadio.setOnClickListener(v -> {
            // Launch Spotify using an explicit intent
            Intent launchRadio = new Intent(Intent.ACTION_MAIN);
            launchRadio.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
            try {
                startActivity(launchRadio);
            } catch (ActivityNotFoundException e) {
                showToast("Spotify app is not installed on your device.");
            }
        });
        ivBtnDevices.setOnClickListener(v -> {
            Intent launchWindows = new Intent(ActivityMain.this, ActivityDevices.class);
            startActivity(launchWindows);
        });
        ivBtnMedia.setOnClickListener(v -> {
            // Launch Spotify using an explicit intent
            Intent launchRadio = new Intent(Intent.ACTION_MAIN);
            launchRadio.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
            try {
                startActivity(launchRadio);
            } catch (ActivityNotFoundException e) {
                showToast("Spotify app is not installed on your device.");
            }
        });

        // Initialize the ActivityResultLauncher
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        List<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String spokenText = results.get(0);
                        String resultText = VoiceCommand.processSpokenText(spokenText);
                        showToast(resultText);
                    }
                }
        );
        ivBtnVoice.setOnClickListener(v -> displaySpeechRecognizer());
        ivBtnGear.setOnClickListener(v -> {
            // TODO - add settings support, launching legacy activity until then
            Intent launchWindows = new Intent(ActivityMain.this, ActivityIBUS.class);
            startActivity(launchWindows);
        });

        bluetoothHelper = new BluetoothHelper(this);
        if (!bluetoothHelper.checkBluetoothPermissions()) {
            bluetoothHelper.requestBluetoothPermissions(this);
        } else {
            initializeBluetooth();
        }

        ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        showToast("Bluetooth enabled");
                    } else {
                        showToast("Bluetooth not enabled");
                    }
                }
        );
        if (!bluetoothHelper.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        }

        // check if bluetooth enabled (prompt to enable)
        refreshConnectingStatus();

        // set the date and time
        setDateTime();
    }

    private void initializeBluetooth() {
        mBluetoothInterface = BluetoothInterface.getInstance(this, this);
        mBluetoothInterface.connectToRaspberryPi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to connect to the Raspberry Pi.", Toast.LENGTH_LONG).show();
                onPermissionsDenied();
            }
        } else {
            mBluetoothInterface.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (Objects.requireNonNull(intent.getAction()).compareTo(Intent.ACTION_TIME_TICK) == 0)
                    setDateTime();
            }
        };

        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_broadcastReceiver != null)
            unregisterReceiver(_broadcastReceiver);
    }

    private void updateBluetoothStatus() {
        if (mBluetoothInterface != null && mBluetoothInterface.isConnected()) {
            ivBluetoothStatus.setImageResource(R.drawable.ic_bluetooth_connected);
        } else {
            ivBluetoothStatus.setImageResource(R.drawable.ic_bluetooth_disabled);
        }
    }

    public void setDateTime() {
        Date now = new Date();
        String date = _sdfWatchDate.format(now);
        String time = _sdfWatchTime.format(now);
        tvDateTime.setText(String.format("%s\n%s", time, date));
    }

    private void displaySpeechRecognizer() {
        // Part 2: ActivityMain.java (Continued)
        // ... (Code from previous response) ...
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Launch the speech recognizer activity
        speechRecognizerLauncher.launch(intent);
    }

    /**
     * If Bluetooth connection is being established, show loader.
     */
    public void refreshConnectingStatus() {
        //TODO - add isConnecting to bluetooth interface
        //if (BluetoothInterface.isConnecting()) {
        //    pbConnecting.setVisibility(View.VISIBLE);
        //} else {
        //    pbConnecting.setVisibility(View.GONE);
        //}
    }

    /**
     * Display toast message for Toast.LENGTH_SHORT period. Uses the fragments
     * activity for context.
     * @param message content to display in toast message
     */
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // --- BluetoothConnectionManager.ConnectionListener methods ---
    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth Connected", Toast.LENGTH_SHORT).show();
            updateBluetoothStatus();
            pbConnecting.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth Disconnected", Toast.LENGTH_SHORT).show();
            updateBluetoothStatus();
            pbConnecting.setVisibility(View.GONE);
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            // ... (process received data) ...
        });
    }

    @Override
    public void onConnecting() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth Connecting...", Toast.LENGTH_SHORT).show();
            pbConnecting.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onPermissionsDenied() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Bluetooth Permissions Denied", Toast.LENGTH_SHORT).show();
            // ... (handle permission denial, e.g., show a message to the user) ...
        });
    }
}