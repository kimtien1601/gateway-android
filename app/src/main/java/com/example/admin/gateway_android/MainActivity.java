package com.example.admin.gateway_android;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.List;

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
        btnStartStop.setBackgroundColor(Color.rgb(41,163,41));
        btnStartStop.setTextColor(Color.WHITE);

        startTime = SystemClock.uptimeMillis();

        notification=new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (observe==false){
                    Context context=view.getContext();
                    if (isConnectedToNetwork(context)) {
                        observe=true;
                        btnStartStop.setText("  Stop Observing  ");
                        btnStartStop.setBackgroundColor(Color.rgb(230,0,0));

                        customHandler.postDelayed(updateTimerThread, 1000);
                        txtStt.setVisibility(View.VISIBLE);

                    } else {
                        Toast.makeText(MainActivity.this, "Please check your wifi connectivity!", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    observe=false;
                    btnStartStop.setText(" Start Observing ");
                    btnStartStop.setBackgroundColor(Color.rgb(41,163,41));
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
                if (!isForeground(getApplicationContext())) {
                    notifyuser();
                }

            }
            else{
                btnCapture.setVisibility(View.INVISIBLE);
                txtImgTime.setVisibility(View.INVISIBLE);
                imgViewCrop.setVisibility(View.INVISIBLE);
            }
            customHandler.postDelayed(this, 10000);
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
                            txtStt.setText("Status: Still tracking");
                            lost=false;
                        }
                        else
                        {
                            txtStt.setText("Status: Lost " + finalResult + "s");
                            lost=true;
                        }
                    }
                });
            }
        }).start();
    }

    private void notifyuser(){
        String CHANNEL_ID;
        CharSequence name;

        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setSmallIcon(R.drawable.ic_warning_black_24dp);
        notification.setTicker("This is the suitcase");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Here is the suitcase");
        notification.setContentText("OMG I'm lost. Please find me!!!");
        notification.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        notification.setPriority(NotificationCompat.PRIORITY_MAX);
        notification.setContentIntent(pendingIntent);
        notification.setDefaults(Notification.DEFAULT_ALL);
//        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//        notification.addAction(android.R.drawable.ic_menu_view,"View details",pendingIntent);
        NotificationManager nm= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(uniqueID,notification.build());
    }


    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isConnected = false;
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());
        }

        return isConnected;
    }

    private static boolean isForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : tasks) {
            if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == appProcess.importance && packageName.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }
}

