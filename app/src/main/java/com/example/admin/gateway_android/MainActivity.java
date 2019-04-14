package com.example.admin.gateway_android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    LinearLayout l1;
    Button btnCapture,btnStartStop;
    ImageView imgViewCrop;
    Paint paint;
    TextView txtStt,txtImgTime;
    String url_img = "https://thesis-suitcase.000webhostapp.com/Receive/image.jpg";

    NotificationCompat.Builder notification;
    private  static  final int uniqueID=45612;

    boolean lost;
    boolean observe=false;
    private long startTime = 0L;
    long timeInMilliseconds = 0L;

    private Handler customHandler = new Handler();
    String captime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnhXa();
        lost=false;
        btnCapture.setVisibility(View.INVISIBLE);
        txtImgTime.setVisibility(View.INVISIBLE);
        txtStt.setVisibility(View.INVISIBLE);

        startTime = SystemClock.uptimeMillis();

        notification=new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (observe==false){
                    observe=true;
                    btnStartStop.setText("Stop Observing");
                    customHandler.postDelayed(updateTimerThread, 1000);
                    txtStt.setVisibility(View.VISIBLE);
                }
                else{
                    observe=false;
                    btnStartStop.setText("Start Observing");
                    customHandler.removeCallbacks(updateTimerThread);
                    txtStt.setVisibility(View.INVISIBLE);
                    btnCapture.setVisibility(View.INVISIBLE);
                    txtImgTime.setVisibility(View.INVISIBLE);
                    imgViewCrop.setVisibility(View.INVISIBLE);
                }
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadImageFromUrl(url_img);
                imgViewCrop.setVisibility(View.VISIBLE);
                txtImgTime.setVisibility(View.VISIBLE);
                txtImgTime.setText("Captured Time: " + captime);
            }
        });
    }

    private void AnhXa() {
        l1 = findViewById(R.id.linearLayout_1);
        btnCapture = findViewById(R.id.buttonCapture);
        btnStartStop=findViewById(R.id.buttonStartStop);
        imgViewCrop = findViewById(R.id.imageViewCrop);
        txtStt = findViewById(R.id.txtStatus);
        txtImgTime=findViewById(R.id.txtCaptime);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
//            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            ReadTextFileFromUrl();
            if (lost) {
                btnCapture.setVisibility(View.VISIBLE);
                notifyuser();
            }
            else{
                btnCapture.setVisibility(View.INVISIBLE);
                txtImgTime.setVisibility(View.INVISIBLE);
                imgViewCrop.setVisibility(View.INVISIBLE);
            }
            customHandler.postDelayed(this, 5000);
        }
    };


    private void LoadImageFromUrl(String link) {
        Picasso.with(this).load(link).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).memoryPolicy(MemoryPolicy.NO_STORE,MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)
                .into(imgViewCrop, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
//                        Toast.makeText(MainActivity.this, "Load Successfully!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError() {
                        Toast.makeText(MainActivity.this, "Database not available", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ReadTextFileFromUrl() {
        new Thread(new Runnable(){
            public void run(){
                String result="";
                try {
                    // Create a URL for the desired page
                    URL url = new URL("https://thesis-suitcase.000webhostapp.com/Receive/period.txt"); //My text file location
                    //First open the connection
                    HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6000); // timing out in a minute

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    result=in.readLine();
                    captime=in.readLine();
                    in.close();
                } catch (Exception e) {
                    Log.d("MyTag", e.toString());
                }
                final String finalResult = result;
                runOnUiThread(new Runnable(){
                    public void run() {
                        int period = Integer.parseInt(finalResult);
                        if (period==0) {
                            txtStt.setText("Still tracking");
                            lost=false;
                        }
                        else
                        {
                            txtStt.setText("Lost " + finalResult + "s");
                            lost=true;
                        }
                    }
                });
            }
        }).start();
    }

    private void notifyuser(){
        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setSmallIcon(R.mipmap.ic_launcher_round);
        notification.setTicker("This is the suitcase");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Here is the suitcase");
        notification.setContentText("OMG I'm lost. Please find me!!!");
        notification.setCategory(Notification.CATEGORY_PROMO);
//        notification.addAction(android.R.drawable.ic_menu_view,"View details",pendingIntent);
        notification.setPriority(Notification.PRIORITY_HIGH);
        notification.setContentIntent(pendingIntent);
//        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(uniqueID,notification.build());
    }
}

