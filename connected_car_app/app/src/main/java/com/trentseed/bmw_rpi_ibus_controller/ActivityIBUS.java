package com.trentseed.bmw_rpi_ibus_controller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.trentseed.bmw_rpi_ibus_controller.common.BluetoothInterface;
import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket;
import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacketListener;
import com.trentseed.bmw_rpi_ibus_controller.common.IBUSWrapper;

import java.util.List;

public class ActivityIBUS extends FragmentActivity implements IBUSPacketListener {

	// layout objects
	ImageView ivBack;
	ImageView ivBusActivity;
	ListView lvBusEvents;
	TextView tvNoActivity;
	AdapterIBUS adapter;
	IBUSViewModel ibusViewModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		onNewIntent(getIntent());
		setContentView(R.layout.activity_ibus);
		BluetoothInterface.mActivity = this;

		// get layout objects
		ivBack = findViewById(R.id.ivBack);
		ivBusActivity = findViewById(R.id.ivBusActivity);
		ivBusActivity.setVisibility(View.GONE);
		lvBusEvents = findViewById(R.id.lvBusEvents);
		tvNoActivity = findViewById(R.id.tvNoActivity);

		// set click handlers
		ivBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		// Get the ViewModel
		ibusViewModel = new ViewModelProvider(this).get(IBUSViewModel.class); // Correct now

		// create and set adapter
		adapter = new AdapterIBUS(this, ibusViewModel.getIBUSPackets().getValue());
		lvBusEvents.setAdapter(adapter);
		if (adapter.getCount() > 0) tvNoActivity.setVisibility(View.GONE);
		lvBusEvents.setOnItemClickListener(new AbsListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				IBUSPacket ibPacket = adapter.getItem(position);
				AlertDialog.Builder msgBuilder = new AlertDialog.Builder(ActivityIBUS.this);
				msgBuilder.setTitle("IBUS Packet");
				msgBuilder.setMessage("Data: " + ibPacket.raw + "\nASCII: " + ibPacket.getAsciiFromRaw());
				msgBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				msgBuilder.create().show();

			}
		});

		// Observe the LiveData
		ibusViewModel.getIBUSPackets().observe(this, packets -> {
			// This code will run on the UI thread
			adapter.setIbusPackets(packets);
			adapter.notifyDataSetChanged();
			flashActivity();
			if (adapter.getCount() > 0) tvNoActivity.setVisibility(View.GONE);
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
		fadeIn.setAnimationListener(new AnimationListener() {
			public void onAnimationEnd(Animation animation) {
				// fade out animation
				Animation fadeOut = new AlphaAnimation(1, 0);
				fadeOut.setInterpolator(new AccelerateInterpolator());
				fadeOut.setDuration(300);
				fadeOut.setAnimationListener(new AnimationListener() {
					public void onAnimationEnd(Animation animation) {
						ivBusActivity.setVisibility(View.GONE);
					}

					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationStart(Animation animation) {
					}
				});
				ivBusActivity.setVisibility(View.VISIBLE);
				ivBusActivity.startAnimation(fadeOut);
			}

			public void onAnimationRepeat(Animation animation) {
			}

			public void onAnimationStart(Animation animation) {
			}
		});
		ivBusActivity.startAnimation(fadeIn);
	}

	@Override
	protected void onResume() {
		super.onResume();
		BluetoothInterface.mActivity = this;
		BluetoothInterface.setIBUSPacketListener(this); // Set the listener
		BluetoothInterface.checkConnection();
	}

	@Override
	public void onIBUSPacketsReceived(List<IBUSPacket> packets) {
		runOnUiThread(() -> {
			// Process the packets
			for (final IBUSPacket ibPacket : packets) {
				IBUSWrapper.processPacket(ibPacket);
			}
			// Add the packets to the ViewModel
			ibusViewModel.addIBUSPackets(packets);
		});
	}
}