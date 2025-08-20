package com.example.puribot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;

    private static final String DEVICE_NAME = "ESP32-RC-CAR";
    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "PuriBot";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private View statusDot;
    public TextView status_text;
    private boolean isConnected = false;
    private boolean fanOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status_text=findViewById(R.id.status_text);
        statusDot = findViewById(R.id.status_dot);
        MaterialButton fanToggle = findViewById(R.id.fan_toggle_button);
        TextView fanStatusText = findViewById(R.id.fan_status_text);
        MaterialButton settings_button = findViewById(R.id.settings_button);
        settings_button.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, settings_Page.class);
            startActivity(intent);
        });

        // Movement buttons
        MaterialButton btnForward = findViewById(R.id.button_forward);
        MaterialButton btnBackward = findViewById(R.id.button_backward);
        MaterialButton btnLeft = findViewById(R.id.button_left);
        MaterialButton btnRight = findViewById(R.id.button_right);
        MaterialButton fan_toggle_button=findViewById(R.id.fan_toggle_button);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        // Request permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            enableBluetooth();
        }

        fanToggle.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, "Not connected to robot", Toast.LENGTH_SHORT).show();
                return;
            }
            fanOn = !fanOn;
            fanStatusText.setText(fanOn ? "Scanning..." : "Scan Now");
            sendCommand(fanOn ? "SC" : "OSC");
        });

        btnForward.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand("F");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("S");
            }
            return true;
        });

        btnBackward.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand("B");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("S");
            }
            return true;
        });



        btnLeft.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand("L");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("S");
            }
            return true;
        });

        btnRight.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand("R");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("S");
            }
            return true;
        });

        fan_toggle_button.setOnClickListener(v->sendCommand("SC"));

    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } else {
            connectToDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            connectToDevice();
        }
    }

    private void connectToDevice() {
        new Thread(() -> {
            BluetoothDevice espDevice = null;

            for (int attempt = 1; attempt <= 10; attempt++) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> Toast.makeText(this, "Missing Bluetooth permission", Toast.LENGTH_SHORT).show());
                    return;
                }

                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(DEVICE_NAME)) {
                        espDevice = device;
                        break;
                    }
                }

                if (espDevice != null) {
                    try {
                        bluetoothSocket = espDevice.createRfcommSocketToServiceRecord(ESP32_UUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();
                        isConnected = true;
                        // ✅ Share the socket with SettingsActivity
                        settings_Page.setBluetoothSocket(bluetoothSocket);
                        runOnUiThread(() -> {
                            updateStatus(true);
                            Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show();
                        });
                        startListeningForData();
                        return;
                    } catch (IOException e) {
                        Log.e(TAG, "Connection attempt " + attempt + " failed", e);
                    }
                }

                runOnUiThread(() -> updateStatus(false));

                try {
                    Thread.sleep(3000); // wait 3 seconds before retrying
                } catch (InterruptedException ignored) {
                }
            }

            runOnUiThread(() -> Toast.makeText(this, "Failed to connect to ESP32 after multiple attempts", Toast.LENGTH_LONG).show());
        }).start();
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String received = new String(buffer, 0, bytes).trim();

                        runOnUiThread(() -> {
                            TextView air_quality_value = findViewById(R.id.air_quality_value);
                            if(Integer.parseInt(received)>=2500){
                                air_quality_value.setText("Bad");
                                air_quality_value.setTextColor(getResources().getColor(R.color.red_700));
                            }
                            else if(Integer.parseInt(received)>=2000 && Integer.parseInt(received)<2500){
                                air_quality_value.setText("Average");
                                air_quality_value.setTextColor(getResources().getColor(R.color.orange_700));
                            }
                            else if(Integer.parseInt(received)>=1500 && Integer.parseInt(received)<2000){
                                air_quality_value.setText("Good");
                                air_quality_value.setTextColor(getResources().getColor(R.color.green_700));
                            }
//                            receivedDataView.setText("Sensor Value: " + received);
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Reading failed", e);
                    break;
                }
            }
        }).start();
    }



    private void sendCommand(String cmd) {
        if (outputStream != null) {
            try {
                outputStream.write(cmd.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Send failed", e);
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private void updateStatus(boolean connected) {
        isConnected = connected;
        int color = connected ? getResources().getColor(R.color.green_700) : getResources().getColor(R.color.red_700);
        statusDot.setBackgroundTintList(ColorStateList.valueOf(color));

        if (connected) {
            status_text.setText("Connected");
        } else {
            status_text.setText("Disconnected");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException ignored) {
        }
    }
}
