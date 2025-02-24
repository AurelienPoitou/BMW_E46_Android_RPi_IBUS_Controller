package com.trentseed.bmw_rpi_ibus_controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Activity that handles presents core functionality to user.
 * @author Trent
 */
public class ActivityMain extends AppCompatActivity {

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

        bluetoothHelper = new BluetoothHelper(this);
        BluetoothInterface.mBluetoothHelper = bluetoothHelper;

        if (!bluetoothHelper.isBluetoothSupported()) {
            showToast("Bluetooth is not supported on this device");
            return;
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
        BluetoothInterface.mActivity = this;

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
        ivBmwEmblem.setOnClickListener(v -> {
            if (!BluetoothInterface.isConnected() && !BluetoothInterface.isConnecting) {
                performBackgroundConnect();
            }
        });
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

        // check if bluetooth enabled (prompt to enable)
        refreshConnectingStatus();

        // set the date and time
        setDateTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothInterface.mActivity = this;

        if (!BluetoothInterface.isConnected()) {
            performBackgroundConnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            BluetoothInterface.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if (BluetoothInterface.isConnected()) {
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
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Launch the speech recognizer activity
        speechRecognizerLauncher.launch(intent);
    }

    private void performBackgroundConnect() {
        BluetoothInterface.isConnecting = true;
        refreshConnectingStatus();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                logger.info("Checking connection in background thread");
                BluetoothInterface.checkConnection();
            } finally {
                runOnUiThread(() -> {
                    logger.info("connection on UI thread");
                    BluetoothInterface.isConnecting = false;
                    refreshConnectingStatus();
                    if (BluetoothInterface.isConnected()) {
                        showToast("Connected!");
                    } else {
                        showToast("Unable to connect via bluetooth :(");
                    }
                });
                updateBluetoothStatus();
            }
        });
    }

    /**
     * If Bluetooth connection is being established, show loader.
     */
    public void refreshConnectingStatus() {
        if (BluetoothInterface.isConnecting()) {
            pbConnecting.setVisibility(View.VISIBLE);
        } else {
            pbConnecting.setVisibility(View.GONE);
        }
    }

    /**
     * Display toast message for Toast.LENGTH_SHORT period. Uses the fragments
     * activity for context.
     * @param message content to display in toast message
     */
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
