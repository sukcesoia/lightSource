/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package study.sukcesoia.lightsource;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class LightControlActivity extends Activity implements PacketData.PacketDataInterface {
    private final static String TAG = LightControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private Handler mHandler = new Handler();

    /* Ble */
    private String mDeviceName = "LightSource";
    private String mDeviceAddress = "A4:D5:78:05:52:32";
    // private String mDeviceAddress = "00:13:AA:00:35:78";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic = null;
    private boolean mConnected = false;

    /* UI */
    private ListView mInfoListView;
    private InfoListViewAdapter mInfoListViewAdapter;
    private TextView mTextViewPwm;
    private SeekBar mSeekBarPwm;
    private Button mButtonBle;

    /* Packet */
    private PacketData mPacketData;
    private int ledPwmValueMOSI;
    private int ledPwmValueMISO;

    // Auto Connection
    private boolean isRunAutoConnection = false;
    private Runnable mRunAutoConnection = new Runnable() {
        @Override
        public void run() {
            isRunAutoConnection = true;
            if (!mConnected) {
                mBluetoothLeService.connect(mDeviceAddress);
                mInfoListViewAdapter.setItemValue(0, "Connecting...");
            }
        }
    };

    private boolean isRunLedFeedback = false;
    private Runnable mRunLedFeedback = new Runnable() {
        @Override
        public void run() {
            if (ledPwmValueMOSI != ledPwmValueMISO) {
                packetSend(ledPwmValueMISO);
                // mHandler.postDelayed(mRunLedFeedback, 100);
            } else {
                isRunAutoConnection = false;
                // mHandler.removeCallbacks(mRunLedFeedback);
            }
            mHandler.postDelayed(mRunLedFeedback, 100);
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mBluetoothAdapter.isEnabled())
                mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mInfoListViewAdapter.setItemValue(0, "Connected");
                isRunAutoConnection = false;
                mHandler.removeCallbacks(mRunAutoConnection);

                mButtonBle.setText("Disconnect");

                /* Led */
                if (!isRunLedFeedback) {
                    isRunLedFeedback = true;
                    mHandler.post(mRunLedFeedback);
                }

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mInfoListViewAdapter.setItemValue(0, "Disconnected");
//                if(!isRunAutoConnection)
//                    mHandler.postDelayed(mRunAutoConnection, 1000);

                mButtonBle.setText("Connect");

                /* Led */
                mHandler.removeCallbacks(mRunLedFeedback);
                isRunLedFeedback = false;

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                mPacketData.setPacketMOSI(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_control);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /* Info List View */
        mInfoListView = (ListView) findViewById(R.id.list_view_info);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInfoListViewAdapter = new InfoListViewAdapter(inflater);
        mInfoListView.setAdapter(mInfoListViewAdapter);
        mInfoListView.setOnItemClickListener(onClickListView);

        /* Led Pwm SeekBar */
        mTextViewPwm = findViewById(R.id.textViewPwm);
        mTextViewPwm.setText("Light Control Activity");
        mSeekBarPwm = findViewById(R.id.seekBarPwm);
        mSeekBarPwm.setMax(255);
        mSeekBarPwm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextViewPwm.setText(progress + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                packetSend(seekBar.getProgress());
            }
        });

        /* Ble Button */
        mButtonBle = findViewById(R.id.buttonBle);
        if (mConnected)
            mButtonBle.setText("Disconnect");
        else
            mButtonBle.setText("Connect");
        mButtonBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnected) {
                    mBluetoothLeService.disconnect();
                    mHandler.removeCallbacks(mRunLedFeedback);
                    isRunLedFeedback = false;
                } else {
                    mBluetoothLeService.connect(mDeviceAddress);
                    if (!isRunLedFeedback) {
                        isRunLedFeedback = true;
                        mHandler.post(mRunLedFeedback);
                    }
                }
            }
        });

        /* Packet Data */
        mPacketData = new PacketData(this);
    }

    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunAutoConnection);
        mHandler.removeCallbacks(mRunLedFeedback);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;

        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices_uuid: " + uuid);

            if (uuid.contentEquals("0000ffe0-0000-1000-8000-00805f9b34fb")) {
                Log.d(TAG, "displayGattServices_Pass!");
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    Log.d(TAG, "BluetoothGattCharacteristic_uuid: " + uuid);

                    if (uuid.contentEquals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                        Log.d(TAG, "BluetoothGattCharacteristic_Pass!");
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        mBluetoothGattCharacteristic = gattCharacteristic;
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void packetSend(int value) {
        // 4D 49 00 01 6D

        byte[] bytes = new byte[5];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 0x00;

        bytes[0] = 0x4D;
        bytes[1] = 0x49;
        bytes[2] = (byte) value;
        bytes[3] = 0x00;
        bytes[4] = 0x6D;

        ledPwmValueMISO = value;

        if (mBluetoothGattCharacteristic != null) {
            mBluetoothGattCharacteristic.setValue(bytes);
            mBluetoothLeService.writeCharacteristic(mBluetoothGattCharacteristic);
        }
    }

    @Override
    public void returnDataStr(
            String power, String current, String voltage, String batteryState, String temperature, String ledPwm, String buttonState) {

        mInfoListViewAdapter.setItemValue(1, ledPwm);
        mInfoListViewAdapter.setItemValue(2, voltage);
        mInfoListViewAdapter.setItemValue(3, current);
        mInfoListViewAdapter.setItemValue(4, power);
        mInfoListViewAdapter.setItemValue(5, batteryState);
        mInfoListViewAdapter.setItemValue(6, temperature);
    }

    @Override
    public void returnData(
            float power, float current, float batteryVcc, int batteryState, float temperature, int ledPwm, int buttonState) {
        ledPwmValueMOSI = ledPwm;
    }
}
