package com.example.admin.gateway_android;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    LinearLayout l1;
    Button btnCapture,btnStartStop;
    ImageView imgViewCrop;
    Paint paint;
    TextView txtStt,txtImgTime;
    String url_img = "https://thesis-suitcase.000webhostapp.com/Receive/image.jpg";
    String url_txt = "https://thesis-suitcase.000webhostapp.com/Receive/period.txt";
    String url_heroku="https://tien-xinhdep-pro-server.herokuapp.com";
    //http://192.168.1.151:3000/

    NotificationCompat.Builder notification;
    private  static  final int uniqueID=45612;

    boolean lost=false;;
    boolean observe=false;
    private Socket mSocket;
    private Handler customHandler = new Handler();
    String captime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnhXa();

        /*----INIT----*/
        btnCapture.setVisibility(View.INVISIBLE);
        txtImgTime.setVisibility(View.INVISIBLE);
        txtStt.setVisibility(View.INVISIBLE);
        btnStartStop.setBackgroundColor(Color.rgb(41,163,41));
        btnStartStop.setTextColor(Color.WHITE);
//        startTime = SystemClock.uptimeMillis();

        notification=new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        //Kết nối đến Server
        Connect2Server();
        mSocket.on("server-send-ok", onReceiveStatus);

        /*----WHEN PUSH BUTTON START/STOP OBSERVING----*/
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (observe==false){
                    Context context=view.getContext();
                    if (isConnectedToNetwork(context)) {
                        observe=true;
                        btnStartStop.setText("  Stop Observing  ");
                        btnStartStop.setBackgroundColor(Color.rgb(230,0,0));

                        txtStt.setVisibility(View.VISIBLE);

                        customHandler.postDelayed(updateTimerThread, 1); //Start Handler

                    } else {
                        Toast.makeText(MainActivity.this, "Please check network connection!", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    observe=false;
                    btnStartStop.setText(" Start Observing ");
                    btnStartStop.setBackgroundColor(Color.rgb(41,163,41));

                    txtStt.setVisibility(View.INVISIBLE);
                    btnCapture.setVisibility(View.INVISIBLE);
                    txtImgTime.setVisibility(View.INVISIBLE);
                    imgViewCrop.setVisibility(View.INVISIBLE);

                    customHandler.removeCallbacks(updateTimerThread); //Start Handler
                }
            }
        });

        /*----WHEN PUSH BUTTON CAPTURE----*/
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

    /*----HANDLER FOR UPDATING----*/
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
//            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            mSocket.emit("client-request-info");
            mSocket.on("server-send-info", mydata);
            ReadTextFileFromUrl();
            customHandler.postDelayed(this, 30000);
        }
    };

    /*----CREATE VARIABLE FROM LAYOUT----*/
    private void AnhXa() {
        l1 = findViewById(R.id.linearLayout_1);
        btnCapture = findViewById(R.id.buttonCapture);
        btnStartStop=findViewById(R.id.buttonStartStop);
        imgViewCrop = findViewById(R.id.imageViewCrop);
        txtStt = findViewById(R.id.txtStatus);
        txtImgTime=findViewById(R.id.txtCaptime);
    }

    /*----CHECK SUITCASE STATUS----*/
    private void ReadTextFileFromUrl() {
        new Thread(new Runnable(){
            public void run(){
                String result="";
                try {
                    // Create a URL for the desired page
                    URL url = new URL(url_txt); //My text file location
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
                            btnCapture.setVisibility(View.INVISIBLE);
                            txtImgTime.setVisibility(View.INVISIBLE);
                            imgViewCrop.setVisibility(View.INVISIBLE);
                        }
                        else
                        {
                            txtStt.setText("Status: Lost " + finalResult + "s");
                            lost=true;
                            btnCapture.setVisibility(View.VISIBLE);
                            if (!isForeground(getApplicationContext())) {
                                notifier();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    /*----LOAD IMAGE TO IMAGE VIEW----*/
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

    /*----CREATE NOTIFICATION WHEN LOST----*/
    private void notifier(){
        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setSmallIcon(R.drawable.ic_warning_black_24dp);
        notification.setTicker("This is the suitcase");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("This is the suitcase");
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

    /*----CHECK WHETHER APP IS IN FOREGROUND (FOR DISCARDING NOTIFICATION)----*/
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

    /*----CHECK NETWORK CONNECTION----*/
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

    /*----HEROKU CONNECTION----*/
    private void Connect2Server(){
        try {
//            mSocket = IO.socket("http://192.168.1.151:3000/");
            mSocket = IO.socket(url_heroku);
        } catch (URISyntaxException e) {
            Toast.makeText(this, "Server fail to start...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        mSocket.connect();
    }

    /*----READ DATA FROM HEROKU----*/
    private Emitter.Listener mydata = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    try {
                        int result = object.getInt("suitcase");
                        if (result>0) {
                            txtCurrentPosition.setText("lost "+result+"s");
                        } else {
                            txtCurrentPosition.setText("still tracking");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    /*----WHEN SERVER HAS RECEIVED DATA FROM RASPBERRY----*/
    private Emitter.Listener onReceiveStatus = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    try {
                        boolean status =object.getBoolean("status");
                        if (status){
                            Toast.makeText(MainActivity.this, "Info has been sent to Server", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Info Lost", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };
}

