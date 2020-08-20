package com.example.bluetoothtest;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;
public class RobotController extends AppCompatActivity {

    byte[] readBuffer;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    Button btn, btnDis;
    EditText txtinput ;
    String address = null;
    TextView lumn;
    TextView txtResponseFromDevice;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_controller);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        new ConnectBT().execute();
        txtinput = (EditText) findViewById((R.id.inTxtSignal));
        txtResponseFromDevice = (TextView) findViewById(R.id.txtResponseFromDevice);
        btn = (Button) findViewById(R.id.btnSendSignal);
        btnDis = (Button) findViewById(R.id.button4);
        lumn = (TextView) findViewById(R.id.textView2);

        // receive the signal after btn clicked



        // send the signal after btn clicked
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                String value = txtinput.getText().toString();
                if(!value.isEmpty()){
                    sendSignal(value);
                    // retrieve from input and write to the terminal
                    if(!value.isEmpty()){
                        writeToTerminal("Input Sent :"+value);
                    }
                    Toast.makeText(getApplicationContext(), "Input sent", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "No Input", Toast.LENGTH_LONG).show();
                }
            }
        });



    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(RobotController.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Dok jadi .. connect semula");
                finish();
            } else {
                msg("Connected dh .. ");
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }

    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    private void sendSignal ( String number ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(number.toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        finish();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            // retrieve data from arduino and write to the terminal
                                            if(!data.isEmpty()){
                                                writeToTerminal(data);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void writeToTerminal(String data){
        txtResponseFromDevice.setText(data);
        if(txtResponseFromDevice.getText().toString().length() >= 30){
            txtResponseFromDevice.setText("");
            txtResponseFromDevice.append(data);
        }else{
            txtResponseFromDevice.append("\n" + data);
        }
    }

}