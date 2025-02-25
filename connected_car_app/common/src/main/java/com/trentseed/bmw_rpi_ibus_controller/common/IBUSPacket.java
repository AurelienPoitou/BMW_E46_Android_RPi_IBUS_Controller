package com.trentseed.bmw_rpi_ibus_controller.common;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IBUS "packet" that is sent between BMW and Raspberry Pi
 */
public class IBUSPacket {
    public List<Integer> raw;
    public long timestamp;

    public IBUSPacket(){ }

    public String getSourceName(){
        return getDeviceName(this.raw.get(0));
    }

    public String getDestinationName(){
        return getDeviceName(raw.get(2));
    }

    private String getDeviceName(int device_id){
        switch (device_id) {
            case 0:
                return "Broadcast 00";
            case 0x18:
                return "CDW - CDC CD-Player";
            case 0x3b:
                return "NAV Navigation/Video Module";
            case 0x43:
                return "Menu Screen";
            case 0x50:
                return "MFL Steering Wheel Controls";
            case 0x60:
                return "PDC Park Distance Control";
            case 0x68:
                return "RAD Radio";
            case 0x6a:
                return "DSP Digital Sound Processor";
            case 0x80:
                return "IKE Instrument Kombi Electronics";
            case 0xbb:
                return "TV Module";
            case 0xbf:
                return "LCM Light Control Module";
            case 0xc0:
                return "MID Multi-Information Display Buttons";
            case 0xc8:
                return "TEL Telephone";
            case 0xd0:
                return "Navigation Location";
            case 0xe7:
                return "OBC Text Bar";
            case 0xed:
                return "Lights, Wipers, Seat Memory";
            case 0xf0:
                return "BMB Board Monitor Buttons";
            case 0xff:
                return "Broadcast FF";
            default:
                return "Unknown";
        }
    }

    /**
     * Parses hex string and returns ASCII character representation
     * http://www.mkyong.com/java/how-to-convert-hex-to-ascii-in-java/
     * @return String
     */
    public String getAsciiFromRaw(){
        StringBuilder sb = new StringBuilder();
        for( int i=0; i<raw.size(); i+=1 ){
            int decimal = raw.get(i);
            sb.append((char)decimal);
        }
        String normalized = Normalizer.normalize(sb.toString(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        normalized = normalized.replace("#E", "");  // extra data to remove
        normalized = normalized.replaceAll("[,;]", "");  // extra data to remove
        normalized = normalized.replaceAll("[a-z]", "");  // extra data to remove
        return normalized.replace(" CAP", " CA");  // extra data to remove
    }

    public String getHexFromRaw(){
        return toPaddedHexString(raw);
    }

    public static String toPaddedHexString(List<Integer> numbers) {
        return numbers.stream()
                .map(IBUSPacket::toPaddedHex)
                .collect(Collectors.joining(" "));
    }

    private static String toPaddedHex(int number) {
        String hex = Integer.toHexString(number).toUpperCase();
        return String.format("%02X", number);
    }

    public String getTime() {
        return IBUSPacket.convertTimestampToDateTime(timestamp);
    }

    private static String convertTimestampToDateTime(long timestamp) {
        // Convert the timestamp to an Instant
        Instant instant = Instant.ofEpochSecond(timestamp);

        // Convert the Instant to a LocalDateTime (using the system's default time zone)
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        // Format the LocalDateTime to a human-readable string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }
}
