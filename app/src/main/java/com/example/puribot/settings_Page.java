package com.example.puribot;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.OutputStream;

public class settings_Page extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private TextInputEditText controlValueInput;
    private MaterialButton sendButton, quickValue1, quickValue2, quickValue3;
    private View statusDot;
    private TextView statusText;

    // Reuse the same socket/stream created in MainActivity
    private static BluetoothSocket bluetoothSocket;
    private static OutputStream outputStream;

    public static void setBluetoothSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
        try {
            if (bluetoothSocket != null) {
                outputStream = bluetoothSocket.getOutputStream();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting output stream", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_page); // use the XML you shared

        // Find views
        controlValueInput = findViewById(R.id.control_value_input);
        sendButton = findViewById(R.id.send_button);
        quickValue1 = findViewById(R.id.quick_value_1);
        quickValue2 = findViewById(R.id.quick_value_2);
        quickValue3 = findViewById(R.id.quick_value_3);
        statusDot = findViewById(R.id.settings_status_dot);
        statusText = findViewById(R.id.settings_status_text);

        // Initial status
        updateStatus(bluetoothSocket != null && bluetoothSocket.isConnected());

        // Send button
        sendButton.setOnClickListener(v -> {
            String value = controlValueInput.getText() != null ?
                    controlValueInput.getText().toString().trim() : "";

            if (value.isEmpty()) {
                Toast.makeText(this, "Enter a value first", Toast.LENGTH_SHORT).show();
            } else {
                sendCommand(value);
            }
        });

        // Quick values
        quickValue1.setOnClickListener(v -> sendCommand("1000"));
        quickValue2.setOnClickListener(v -> sendCommand("2000"));
        quickValue3.setOnClickListener(v -> sendCommand("2500"));
    }

    private void sendCommand(String cmd) {
        if (outputStream == null) {
            Toast.makeText(this, "Not connected to ESP32", Toast.LENGTH_SHORT).show();
            updateStatus(false);
            return;
        }
        try {
            outputStream.write(cmd.getBytes());
            outputStream.flush();
            Toast.makeText(this, "Sent: " + cmd, Toast.LENGTH_SHORT).show();
            updateStatus(true);
        } catch (IOException e) {
            Log.e(TAG, "Send failed", e);
            Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
            updateStatus(false);
        }
    }

    @SuppressLint("ResourceAsColor")
    private void updateStatus(boolean connected) {
        int color = connected ? getResources().getColor(R.color.green_700)
                : getResources().getColor(R.color.red_700);
        statusDot.setBackgroundTintList(ColorStateList.valueOf(color));

        if (connected) {
            statusText.setText("Connected");
            statusText.setTextColor(getResources().getColor(R.color.green_700));
        } else {
            statusText.setText("Disconnected");
            statusText.setTextColor(getResources().getColor(R.color.red_700));
        }
    }
}
