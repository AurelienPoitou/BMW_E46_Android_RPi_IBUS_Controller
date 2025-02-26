package com.trentseed.bmw_rpi_ibus_controller.common

import android.util.Log
import com.google.gson.Gson
import com.trentseed.bmw_rpi_ibus_controller.car.IBUSPacketParser
import java.util.logging.Logger

object BluetoothDataHolder {
    private val logger: Logger = LogConfig.getLogger()
    val receivedPackets: MutableList<IBUSPacket> = ArrayList()
    private val packetParser = IBUSPacketParser()

    fun updateData(newData: String): Array<IBUSPacket> {
        logger.info("BluetoothDataHolder: Data updated: $newData")

        try {
            val msg: ControllerMessage =
                Gson().fromJson(newData, ControllerMessage::class.java)
            receivedPackets.addAll(msg.ibusPackets)
            packetParser.parsePackets(listOf<IBUSPacket>(*msg.ibusPackets))
            return msg.ibusPackets
        } catch (e: Exception) {
            // Handle parsing errors
            Log.e("BMW", "Error parsing JSON: " + e.message)
        }
        return emptyArray()
    }
}