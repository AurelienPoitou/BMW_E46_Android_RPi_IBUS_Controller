package com.trentseed.bmw_rpi_ibus_controller;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothDataHolder;
import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothInterface;
import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket;
import com.trentseed.bmw_rpi_ibus_controller.common.LogConfig;

import java.util.List;
import java.util.logging.Logger;

public class ActivityIBUS extends AppCompatActivity implements BluetoothInterface.IBUSPacketListener, AdapterIBUS.OnItemClickListener {

	private BluetoothInterface mBluetoothInterface;
	private static final Logger logger = LogConfig.getLogger();
	private ImageView ivBack;
	private ImageView ivBusActivity;
	private RecyclerView rvIBUSPackets;
	private AdapterIBUS adapterIBUS;
	private TextView tvNoActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ibus);
		logger.info("onCreate: Activity created");
		mBluetoothInterface = BluetoothInterface.getInstance();

		ivBack = findViewById(R.id.ivBack);
		ivBusActivity = findViewById(R.id.ivBusActivity);
		ivBusActivity.setVisibility(View.GONE);

		// RecyclerView setup
		rvIBUSPackets = findViewById(R.id.rvIBUSPackets);
		rvIBUSPackets.setLayoutManager(new LinearLayoutManager(this));
		adapterIBUS = new AdapterIBUS(this);
		adapterIBUS.addPackets(BluetoothDataHolder.INSTANCE.getReceivedPackets());
		rvIBUSPackets.setAdapter(adapterIBUS);

		// Activity indicator setup
		tvNoActivity = findViewById(R.id.tvNoActivity);
		if (adapterIBUS.getItemCount() == 0) {
			tvNoActivity.setVisibility(View.VISIBLE); // Initially show the indicator
		} else {
			tvNoActivity.setVisibility(View.GONE);
		}

		// set click handlers
		ivBack.setOnClickListener(v -> finish());
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set the listener in onResume to ensure it's set when the activity is active
		BluetoothInterface.mIBUSPacketListener = this;
		mBluetoothInterface.connectToRaspberryPi();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Remove the listener when the activity is paused
		BluetoothInterface.mIBUSPacketListener = null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		mBluetoothInterface.onRequestPermissionsResult(requestCode, grantResults);
	}

	@Override
	public void onIBUSPacketReceived(IBUSPacket[] ibusPackets) {
		runOnUiThread(() -> {
			adapterIBUS.addPackets(List.of(ibusPackets));
			flashActivity();
			if (adapterIBUS.getItemCount() == 0) {
				tvNoActivity.setVisibility(View.VISIBLE); // Initially show the indicator
			} else {
				tvNoActivity.setVisibility(View.GONE);
			}
		});
	}

	/**
	 * Indicate IBUS activity on UI by "flashing" green indicator.
	 */
	private void flashActivity() {
		// fade in animation
		ivBusActivity.setVisibility(View.VISIBLE);
		Animation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new AccelerateInterpolator());
		fadeIn.setDuration(300);
		fadeIn.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				// fade out animation
				Animation fadeOut = new AlphaAnimation(1, 0);
				fadeOut.setInterpolator(new AccelerateInterpolator());
				fadeOut.setDuration(300);
				fadeOut.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						ivBusActivity.setVisibility(View.GONE);
					}
				});
				ivBusActivity.startAnimation(fadeOut);
			}
		});
		ivBusActivity.startAnimation(fadeIn);
	}

	@Override
	public void onItemClick(IBUSPacket packet) {
		logger.info(packet.getAsciiFromRaw());
		AlertDialog.Builder msgBuilder = new AlertDialog.Builder(ActivityIBUS.this);
		msgBuilder.setTitle("IBUS Packet");
		msgBuilder.setMessage("Data: " + packet.raw + "\nASCII: " + packet.getAsciiFromRaw());
		msgBuilder.setPositiveButton("Done", (dialog, which) -> {
        });
		msgBuilder.create().show();
	}
}