package com.example.gebruiker.netphone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static Socket socket;
    public static DataInputStream in;
    public static DataOutputStream out;

    private Button btnConnect;
    private EditText etUsername;

    public static boolean connected = false;

    public static String playerid;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerid = UUID.randomUUID().toString();

        etUsername = (EditText) findViewById(R.id.ma_et_username);

        btnConnect = (Button) findViewById(R.id.ma_btn_connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            username = String.valueOf(etUsername.getText());

                if(!connected) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                socket = new Socket("192.168.2.52", 8080);

                                out = new DataOutputStream(socket.getOutputStream());
                                in = new DataInputStream(socket.getInputStream());

                                out.writeUTF(playerid + "|1|connect|1|Connecting client");

                                connected = true;

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
                    while(!connected);

                    Intent i = new Intent(getApplicationContext(), GameActivity.class);
                    i.putExtra("USERNAME", username);
                    startActivity(i);

                }else{
                    disconnect();
                }
            }
        });
    }

    public static void disconnect(){
        try {
            socket.close();
            connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
