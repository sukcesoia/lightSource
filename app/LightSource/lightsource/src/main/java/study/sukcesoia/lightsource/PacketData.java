package study.sukcesoia.lightsource;

import android.util.Log;

import java.util.Locale;

public class PacketData {
    public final static String TAG = "PacketData";
    public final static boolean DEBUG = true;
    private PacketDataInterface mPacketDataInterface;

    /* Buffer */
    private static final int bufferSize = 32;
    private int bufferIndex = 0;
    private byte[] buffer = new byte[bufferSize];

    /* ADC */
    public static float ADC_RESOLUTION = 5.0f / 1024.0f;
    public static float ADC_RESISTIVE = (100000.0f + 47000.0f) / 47000.0f;

    public PacketData(PacketDataInterface packetDataInterface) {
        Log.d(TAG, "PacketData: " + "Init...");
        this.mPacketDataInterface = packetDataInterface;
    }

    public interface PacketDataInterface {
        void returnDataStr(
                String power, String current, String voltage, String batteryState, String temperature, String ledPwm, String buttonState
        );

        void returnData(
                float power, float current, float voltage, int batteryState, float temperature, int ledPwm, int buttonState
        );
    }

    private void packetAnalytical(int[] data) {

        int valueSum = 0;
        for (int i = 2; i < 11; i++)
            valueSum += data[i];

        if ((valueSum % 256) == data[11] | true) {
            int tmpData;
            float power;
            float current;
            float voltage;
            float temperature;
            int ledPwm;
            int buttonState;

            // Current
            tmpData = (data[2] << 8) + data[3];
            current = ADC_RESOLUTION * tmpData;

            // Battery V1
            tmpData = (data[4] << 8) + data[5];
            voltage = ADC_RESOLUTION * (float) tmpData * ADC_RESISTIVE;

            // Power
            power = voltage * current;

            // Battery State
            String batteryState = "";
            if (data[6] == 0) batteryState = "Charge";
            if (data[6] == 1) batteryState = "Discharge";

            // Temperature
            temperature = ((data[7] << 8) + data[8]) / 100.0f;

            // Led PWM
            ledPwm = data[9];

            // Button State
            buttonState = data[10];

            if (DEBUG) {
                Log.d(TAG, "packetAnalytical.current: " + current);
                Log.d(TAG, "packetAnalytical.power: " + power);
                Log.d(TAG, "packetAnalytical.voltage: " + voltage);
                Log.d(TAG, "packetAnalytical.batteryState: " + batteryState);
                Log.d(TAG, "packetAnalytical.ledPwm: " + ledPwm);
                Log.d(TAG, "packetAnalytical.temperature: " + temperature);
            }

            mPacketDataInterface.returnDataStr(
                    String.format("%.3f", power),
                    String.format("%.3f", current),
                    String.format("%.3f", voltage),
                    batteryState,
                    String.format("%.2f", temperature),
                    String.format("%d", ledPwm),
                    ""
            );

            mPacketDataInterface.returnData(
                    power, current, voltage, data[6], temperature, ledPwm, buttonState
            );
        }
    }

    public void setPacketMOSI(byte[] data) {

        // 4D 4F 00 0A 01 04 02 0B 03 0D 01 00 00 00 00 00 00 2D 6D

        if (data != null) {
            // Log.d(TAG, "packetMOSI: " + bytes.length);
            // Log.d(TAG, "packetMOSI: " + byte2HexStr(data, data.length));

            int dataLength = data.length;
            byte[] bufferStep1 = new byte[dataLength];
            for (int i = 0; i < dataLength; i++)
                bufferStep1[i] = data[i];
            for (int i = 0; i < bufferStep1.length; i++)
                if (i + bufferIndex > buffer.length - 1)
                    bufferIndex = 0;
                else
                    buffer[i + bufferIndex] = bufferStep1[i];
            bufferIndex += bufferStep1.length;
            dataLength = buffer.length;
            for (int i = 0; i < dataLength; i++) {
                if ((buffer[i] & 0xFF) == 0x4D && (i + 12) < dataLength) {
                    if ((buffer[i + 1] & 0xFF) == 0x4F && (buffer[i + 12] & 0xFF) == 0x6D) {

                        int[] tmp = new int[12];
                        for (int j = 0; j < tmp.length; j++)
                            tmp[j] = (buffer[i + j] & 0xFF);

                        // Log.d(TAG, "packetMOSI: " + "Pass!");

                        packetAnalytical(tmp);

                        bufferIndex = 0;
                        buffer = new byte[bufferSize];
                        break;
                    }
                }
            }
        }
    }

    public static String byte2HexStr(byte[] b, int iLen) {
        char[] mChars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < iLen; n++) {
            sb.append(mChars[(b[n] & 0xFF) >> 4]);
            sb.append(mChars[b[n] & 0x0F]);
            sb.append(' ');
        }
        return sb.toString().trim().toUpperCase(Locale.US);
    }

}
