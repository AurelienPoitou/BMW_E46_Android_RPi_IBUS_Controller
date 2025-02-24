package com.trentseed.bmw_rpi_ibus_controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket;

import java.util.List;

public class AdapterIBUS extends BaseAdapter {

	private Context context;
	private List<IBUSPacket> ibusPackets;

	public AdapterIBUS(Context context, List<IBUSPacket> ibusPackets) {
		this.context = context;
		this.ibusPackets = ibusPackets;
	}

	@Override
	public int getCount() {
		return ibusPackets.size();
	}

	@Override
	public IBUSPacket getItem(int position) {
		return ibusPackets.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_item_ibus, parent, false);
		}

		TextView tvRaw = convertView.findViewById(R.id.tvRaw);
		TextView tvAscii = convertView.findViewById(R.id.tvAscii);

		IBUSPacket packet = getItem(position);
		tvRaw.setText("Raw: " + packet.raw);
		tvAscii.setText("ASCII: " + packet.getAsciiFromRaw());

		return convertView;
	}

	public void setIbusPackets(List<IBUSPacket> ibusPackets) {
		this.ibusPackets = ibusPackets;
	}
}