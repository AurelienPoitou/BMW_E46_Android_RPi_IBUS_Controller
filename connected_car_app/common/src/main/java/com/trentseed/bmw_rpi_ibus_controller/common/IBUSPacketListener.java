package com.trentseed.bmw_rpi_ibus_controller.common;

import java.util.List;

public interface IBUSPacketListener {
    void onIBUSPacketsReceived(List<IBUSPacket> packets);
}