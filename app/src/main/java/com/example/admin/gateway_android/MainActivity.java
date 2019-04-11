package com.example.admin.gateway_android;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    LinearLayout l1;
    Button btnCapture;
    ImageView imgViewCrop;
    Paint paint;
    TextView txtStt,txtImgTime;
    String url_img = "https://thesis-suitcase.000webhostapp.com/Receive/image.jpg";

    boolean lost;
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    boolean captured=false;
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
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 1000);
        ReadTextFileFromUrl();

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadImageFromUrl(url_img);
//                captured=true;
                imgViewCrop.setVisibility(View.VISIBLE);
                txtImgTime.setVisibility(View.VISIBLE);
                txtImgTime.setText("Captured Time: " + captime);
            }
        });
    }

    private void AnhXa() {
        l1 = findViewById(R.id.linearLayout_1);
        btnCapture = findViewById(R.id.buttonCapture);
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
//                if (captured) {
//
//                }
            }
            else{
//                captured=false;
//                startTime = SystemClock.uptimeMillis();
                btnCapture.setVisibility(View.INVISIBLE);
                txtImgTime.setVisibility(View.INVISIBLE);
                imgViewCrop.setVisibility(View.INVISIBLE);
            }
            customHandler.postDelayed(this, 1000);
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
}

