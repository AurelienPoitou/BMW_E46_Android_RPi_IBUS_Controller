package com.trentseed.bmw_rpi_ibus_controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket;

import java.util.ArrayList;
import java.util.List;

public class AdapterIBUS extends RecyclerView.Adapter<AdapterIBUS.IBUSViewHolder> {

	private List<IBUSPacket> mIBUSPackets;
	private OnItemClickListener mListener;

	public interface OnItemClickListener {
		void onItemClick(IBUSPacket packet);
	}

	public AdapterIBUS(OnItemClickListener listener) {
		mIBUSPackets = new ArrayList<>();
		mListener = listener;
	}

	public void addPackets(List<IBUSPacket> packets) {
		mIBUSPackets.addAll(packets);
		notifyDataSetChanged(); // Notify the RecyclerView of the change
	}

	public void clearPackets() {
		mIBUSPackets.clear();
		notifyDataSetChanged(); // Notify the RecyclerView of the change
	}

	@NonNull
	@Override
	public IBUSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_ibus, parent, false);
		return new IBUSViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull IBUSViewHolder holder, int position) {
		IBUSPacket packet = mIBUSPackets.get(position);
		holder.tvRaw.setText("Raw: " + packet.getHexFromRaw());
		holder.tvAscii.setText("Time: " + packet.getTime());

		// Set the click listener for the item
		holder.itemView.setOnClickListener(v -> {
			if (mListener != null) {
				mListener.onItemClick(packet);
			}
		});
	}

	@Override
	public int getItemCount() {
		return mIBUSPackets.size();
	}

	public static class IBUSViewHolder extends RecyclerView.ViewHolder {
		TextView tvRaw;
		TextView tvAscii;

		public IBUSViewHolder(@NonNull View itemView) {
			super(itemView);
			tvRaw = itemView.findViewById(R.id.tvRaw);
			tvAscii = itemView.findViewById(R.id.tvAscii);
		}
	}
}