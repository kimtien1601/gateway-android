package com.example.admin.gateway_android;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
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
    String url_heroku="https://suitcase-server.herokuapp.com";

    NotificationCompat.Builder notification;
    private  static  final int uniqueID=45612;

    boolean observe=false;
    boolean suitcaseOFF=false;
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



        /*----WHEN PUSH BUTTON START/STOP OBSERVING----*/
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (observe==false){
                    Context context=view.getContext();
                    if (isConnectedToNetwork(context)) {
                        observe=true;
                        //Kết nối đến Server
                        Connect2Server();
//                        mSocket.on("server-send-ok", onReceiveStatus);
                        mSocket.emit("android-on",true);

                        btnStartStop.setText("  Stop Observing  ");
                        btnStartStop.setBackgroundColor(Color.rgb(230,0,0));

                        txtStt.setText("Status: Suitcase off");
                        txtStt.setVisibility(View.VISIBLE);

                        customHandler.postDelayed(updateTimerThread, 1000); //Start Handler

                    } else {
                        Toast.makeText(MainActivity.this, "Please check network connection!", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    mSocket.disconnect();
                    observe = false;
                    customHandler.removeCallbacks(updateTimerThread); //Start Handler

                    btnStartStop.setText(" Start Observing ");
                    btnStartStop.setBackgroundColor(Color.rgb(41, 163, 41));

                    txtStt.setVisibility(View.INVISIBLE);
                    btnCapture.setVisibility(View.INVISIBLE);
                    txtImgTime.setVisibility(View.INVISIBLE);
                    imgViewCrop.setVisibility(View.INVISIBLE);
                }
            }
        });

        /*----BUTTON CAPTURE----*/
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.emit("client-request-img");
                mSocket.on("server-send-img", mydata_img);
            }
        });
    }

    /*----HANDLER FOR UPDATING----*/
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            mSocket.on("suitcase-off",suitcase);
            mSocket.emit("client-request-info");

            mSocket.on("server-send-info", mydata);

            customHandler.postDelayed(this, 2000);
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
//            mSocket = IO.socket("http://192.168.0.103:3000/");
            mSocket = IO.socket(url_heroku);
            mSocket.connect();
            Toast.makeText(this, "Connected to Server!", Toast.LENGTH_SHORT).show();
        } catch (URISyntaxException e) {
            Toast.makeText(this, "Server fail to start...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    /*----READ DATA FROM HEROKU----*/
    private Emitter.Listener mydata = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    String lostTime;
                    int isTracking;
                    try {
                        if (suitcaseOFF==false) {
                            if (object != null) {
                                isTracking = (int) object.get("isTracking");
                                lostTime = object.getString("lostTime");


                                if (isTracking == 1) {
                                    txtStt.setText("Tracking");
                                    imgViewCrop.setVisibility(View.INVISIBLE);
                                    txtImgTime.setVisibility(View.INVISIBLE);
                                    btnCapture.setVisibility(View.INVISIBLE);
                                } else {
                                    txtStt.setText("Lost " + lostTime + "s");
                                    btnCapture.setVisibility(View.VISIBLE);
                                    if (!isForeground(getApplicationContext())) {
                                        notifier();
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener mydata_img = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    String img_text, captime;

                    try {
                        img_text = object.getString("Image");
                        captime=object.getString("CapTime");
                        txtImgTime.setText("Captured Time: " +captime);

                        String encodedString=img_text.substring(img_text.indexOf(",")+1,img_text.length());
                        byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imgViewCrop.setImageBitmap(decodedByte);

                        imgViewCrop.setVisibility(View.VISIBLE);
                        txtImgTime.setVisibility(View.VISIBLE);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };
    private Emitter.Listener suitcase = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    try {
                        suitcaseOFF =object.getBoolean("status");
//                        Toast.makeText(MainActivity.this, "Received suitcase off "+suitcaseOFF, Toast.LENGTH_SHORT).show();
                        if (suitcaseOFF){

                            txtStt.setText("Status: Suitcase off");
                            btnCapture.setVisibility(View.INVISIBLE);
                            txtImgTime.setVisibility(View.INVISIBLE);
                            imgViewCrop.setVisibility(View.INVISIBLE);
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

