package com.trentseed.bmw_rpi_ibus_controller;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.trentseed.bmw_rpi_ibus_controller.common.IBUSPacket;

import java.util.ArrayList;
import java.util.List;

public class IBUSViewModel extends ViewModel {

    private MutableLiveData<List<IBUSPacket>> ibusPackets = new MutableLiveData<>();

    public IBUSViewModel() {
        ibusPackets.setValue(new ArrayList<>());
    }

    public LiveData<List<IBUSPacket>> getIBUSPackets() {
        return ibusPackets;
    }

    public void addIBUSPacket(IBUSPacket packet) {
        List<IBUSPacket> currentList = ibusPackets.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.add(packet);
        ibusPackets.postValue(currentList); // postValue() for background threads
    }

    public void addIBUSPackets(List<IBUSPacket> packets) {
        List<IBUSPacket> currentList = ibusPackets.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.addAll(packets);
        ibusPackets.postValue(currentList); // postValue() for background threads
    }
}