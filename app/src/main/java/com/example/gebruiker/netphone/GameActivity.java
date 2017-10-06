package com.example.gebruiker.netphone;

import android.content.Intent;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.regex.Pattern;

public class GameActivity extends AppCompatActivity {

    private int steerAmount = 100;
    private boolean steering = false, speedChanging = false;
    private float speed = 0;

    ImageButton btnBrake, btnGas;
    static ImageView ivSteer;

    private int backcount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        receive();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    MainActivity.out.writeUTF(MainActivity.playerid + "|1|username|1|" + getIntent().getExtras().getString("USERNAME"));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        TextView tvUsername = (TextView) findViewById(R.id.ga_tv_head_username);
        tvUsername.setText(getIntent().getExtras().getString("USERNAME"));

        btnBrake = (ImageButton) findViewById(R.id.ga_btn_brake);
        btnGas = (ImageButton) findViewById(R.id.ga_btn_gas);

        btnBrake.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    speedChanging = true;
                    btnBrake.setImageResource(R.drawable.spr_brake_pressed);
                    sendSpeed(0.2f, false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    speedChanging = false;
                    btnBrake.setImageResource(R.drawable.spr_brake);
                }
                return true;
            }
        });

        btnGas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    speedChanging = true;
                    btnGas.setImageResource(R.drawable.spr_gas_pressed);
                    sendSpeed(0.1f, true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    speedChanging = false;
                    btnGas.setImageResource(R.drawable.spr_gas);
                }
                return true;
            }
        });

        final GestureListener listener = new GestureListener();
        listener.SetListener(new RoundKnobButtonListener() {
            public void onRotate(final int percentage) {
                steerAmount = percentage * 2;
            }
        });

        final GestureDetector gdt = new GestureDetector(getApplicationContext(),listener);

        ivSteer = (ImageView) findViewById(R.id.ga_iv_wheel);
        ivSteer.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == android.view.MotionEvent.ACTION_UP){
                    steering = false;
                    listener.maxLeft = false;
                    listener.maxRight = false;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean up = false;
                            int speed = 3;
                            while (!steering && steerAmount != 100) {
                                if (steerAmount < 100) {
                                    up = true;
                                }
                                if (up) {
                                    if (steerAmount + speed < 100)
                                        steerAmount += speed;
                                    else
                                        steerAmount = 100;
                                }else{
                                    if (steerAmount - speed > 100)
                                        steerAmount -= speed;
                                    else
                                        steerAmount = 100;
                                }

                                listener.setRotorPercentage(steerAmount / 2);
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();

                }else if(event.getAction() == android.view.MotionEvent.ACTION_DOWN){
                    steering = true;
                    sendSteering();
                }
                return gdt.onTouchEvent(event);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(backcount == 0)
            Toast.makeText(this, "Press again to disconnect.", Toast.LENGTH_SHORT).show();
        else if (backcount >= 1) {
            if(MainActivity.connected)
                MainActivity.disconnect();
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
        backcount++;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                backcount = 0;
            }
        }).start();
    }

    private void receive(){
        Log.i("receive", "ACK");
        new Thread(new Runnable() {
            @Override
            public void run() {
               while(MainActivity.connected){
                   try {
                       Log.i("receive", "Waiting for message");
                       String input = MainActivity.in.readUTF();
                       String[] data = input.split(Pattern.quote("|1|"));

                       switch (data[0]){
                           case "index": changeDashboard(Integer.parseInt(data[1]));
                               break;
                       }

                   } catch (IOException e) {
                       MainActivity.disconnect();
                       Intent i = new Intent(getApplicationContext(), MainActivity.class);
                       startActivity(i);
                   }
               }
            }
        }).start();
    }

    private void changeDashboard(final int index){
        findViewById(android.R.id.content).post(new Runnable() {
            @Override
            public void run() {
                ImageView ivDash = (ImageView) findViewById(R.id.ga_iv_dash);
                if(index == 0)
                    ivDash.setImageResource(R.drawable.spr_dash_blue);
                if(index == 1)
                    ivDash.setImageResource(R.drawable.spr_dash_red);
                if(index == 2)
                    ivDash.setImageResource(R.drawable.spr_dash_green);
                if(index == 3)
                    ivDash.setImageResource(R.drawable.spr_dash_yellow);
            }
        });
    }

    private void sendSpeed(final float tick, final boolean inc) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (speedChanging){
                    if(speed - tick <= 0 && !inc)
                        speed = 0;
                    else if(speed + tick >= 50 && inc)
                        speed = 50;
                    else {
                        if(inc)
                            speed += tick;
                        else
                            speed -= tick;
                        }
                    send("|1|speed|1|" + speed);
                }
            }
        }).start();
    }

    private void sendSteering(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (steering){
                    send("|1|steer|1|" + steerAmount);
                }
            }
        }).start();
    }

    private void send(String message){
        if(!MainActivity.connected)
            return;

        try {
            MainActivity.out.writeUTF(MainActivity.playerid + message);
            Thread.sleep(50);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateWheel(final Matrix matrix, final float finalDeg){
        this.runOnUiThread(new Runnable() {
            public void run() {
                ivSteer.setScaleType(ImageView.ScaleType.MATRIX);
                matrix.postRotate(finalDeg, ivSteer.getWidth()/2, ivSteer.getHeight()/2);
                ivSteer.setImageMatrix(matrix);
            }
        });
    }

    interface RoundKnobButtonListener {
        void onRotate(int percentage);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        RoundKnobButtonListener m_listener;

        public boolean maxLeft = false, maxRight = false;
        // TODO: 28-9-2017 Fix position when down 
       
        public void SetListener(RoundKnobButtonListener l) {
            m_listener = l;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,float distanceY) {
            float x = e2.getX() / ((float) ivSteer.getWidth());
            float y = e2.getY() / ((float) ivSteer.getHeight());
            float rotDegrees = cartesianToPolar(1 - x, 1 - y);

            if (!Float.isNaN(rotDegrees)) {

                float posDegrees = rotDegrees;
                if (rotDegrees < 0)
                    posDegrees = 360 + rotDegrees;

                if (posDegrees > 210 || posDegrees < 150) {
                    if(maxLeft && posDegrees > 210 && posDegrees < 215) {
                        maxLeft = false;
                    }

                    if(maxRight && posDegrees < 150 && posDegrees > 145) {
                        maxRight = false;
                    }

                    if(maxRight || maxLeft)
                        return false;

                    setRotorPosAngle(posDegrees);
                    float scaleDegrees = rotDegrees + 150;

                    int percent = Math.round(scaleDegrees / 3);
                    if (m_listener != null) m_listener.onRotate(percent);
                    return true;
                }else if (posDegrees <= 210 && posDegrees >= 180 && !maxRight){
                    maxLeft = true;
                }else if (posDegrees >= 150 && posDegrees < 180 && !maxLeft){
                    maxRight = true;
                }
            }

            return false;
        }

        @Override
        public boolean onDown(MotionEvent event) {

            return true;
        }

        private float cartesianToPolar(float x, float y) {
            return (float) -Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
        }

        public void setRotorPosAngle(float deg) {
            if (deg >= 210 || deg <= 150) {
                if (deg > 180) deg = deg - 360;

                final Matrix matrix = new Matrix();
                final float finalDeg = deg;
                updateWheel(matrix, finalDeg);
            }
        }

        public void setRotorPercentage(int percentage) {
            int posDegree = percentage * 3 - 150;
            if (posDegree < 0) posDegree = 360 + posDegree;
            setRotorPosAngle(posDegree);
        }
    }
}
