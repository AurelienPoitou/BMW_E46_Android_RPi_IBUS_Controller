package com.trentseed.bmw_rpi_ibus_controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
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

import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothInterface;
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig;
import com.trentseed.bmw_rpi_ibus_controller.common.VoiceCommand;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    ImageView ivBtnDevices;
    ImageView ivBtnMaps;
    ImageView ivBtnMedia;
    ImageView ivBtnGear;
    ImageView ivBtnVoice;
    ProgressBar pbConnecting;
    TextView tvDateTime;

    BroadcastReceiver _broadcastReceiver;
    SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("hh:mm a");
    SimpleDateFormat _sdfWatchDate = new SimpleDateFormat("MM/dd");

    private static final Logger logger = LogConfig.getLogger();

    private static final int SPEECH_REQUEST_CODE = 0;

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
        BluetoothInterface.mActivity = this;

        // Register the ActivityResultLauncher
        // Bluetooth has been enabled
        // Bluetooth has not been enabled
        ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Bluetooth has been enabled
                        Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        // Bluetooth has not been enabled
                        Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                    }
                });

        // get layout objects
        ivBmwEmblem = findViewById(R.id.ivBMWEmblem);
        ivBtnRadio = findViewById(R.id.ivBtnRadio);
        ivBtnDevices = findViewById(R.id.ivBtnDevices);
        ivBtnMaps = findViewById(R.id.ivBtnMaps);
        ivBtnMedia = findViewById(R.id.ivBtnMedia);
        ivBtnVoice = findViewById(R.id.ivBtnMic);
        ivBtnGear = findViewById(R.id.ivBtnGear);
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
            // launch Pandora Radio
            Intent launchPlay = getPackageManager().getLaunchIntentForPackage("com.pandora.android");
            if (launchPlay != null) startActivity(launchPlay);
        });
        ivBtnDevices.setOnClickListener(v -> {
            Intent launchWindows = new Intent(ActivityMain.this, ActivityDevices.class);
            startActivity(launchWindows);
        });
        ivBtnMedia.setOnClickListener(v -> {
            // launch Google Play Music
            Intent launchPlay = getPackageManager().getLaunchIntentForPackage("com.google.android.music");
            if (launchPlay != null) startActivity(launchPlay);
        });
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

        if (BluetoothInterface.mBluetoothAdapter != null && !BluetoothInterface.mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        }
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
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
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

    public void setDateTime() {
        Date now = new Date();
        String date = _sdfWatchDate.format(now);
        String time = _sdfWatchTime.format(now);
        tvDateTime.setText(time + "\n" + date);
    }

    /**
     * Create an intent that can start the Speech Recognizer activity
     */
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            String result = VoiceCommand.processSpokenText(spokenText);
            showToast(result);
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                        Toast.makeText(ActivityMain.this, "Connected!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ActivityMain.this, "Unable to connect via bluetooth :(", Toast.LENGTH_SHORT).show();
                    }
                });
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
