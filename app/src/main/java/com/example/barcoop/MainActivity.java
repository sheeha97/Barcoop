package com.example.barcoop;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.*;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.example.barcoop.R;
import com.example.barcoop.helper;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {
    //TODO GLOBAL HASH_TABLE
    Hashtable<Uri, String> vw_dict = new Hashtable<Uri, String>();
    VideoView vw;
    ArrayList<Uri> videolist = new ArrayList<>();
    int currvideo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // permission
        isStoragePermissionGranted();

        try {

            // storage
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.configure(getApplicationContext());

            // Log
            Log.i("MyAmplifyApp", "Initialized Amplify");
            Log.i("Tester", "Play");

        } catch (AmplifyException error) {
            Log.e("MyAmplifyApp", "Could not initialize Amplify", error);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("영상 다운로드");
        builder.setMessage("업데이트 하시겠습니까?");

        builder.setPositiveButton("아니요", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                play_video();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("예", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // TODO TESTER
                downloadFile();
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }


    public void play_video() {
        setContentView(R.layout.activity_main);
        vw = (VideoView)findViewById(R.id.vidvw);
        vw.setMediaController(new MediaController(this));
        vw.setOnCompletionListener(this);

        File directory = new File(getApplicationContext().getFilesDir() + "/");
        Log.i("File_TEST", "Directory: " + getApplicationContext().getFilesDir() + "/");
        File[] files = directory.listFiles();
        Log.i("File_TEST", "File number: " + files.length);
        for (int i = 0; i < files.length; i++)
        {
            String file_name = files[i].getName();
            Log.i("File_TEST", "File name:" + file_name);

            if (!file_name.contains(".txt")) {
                Uri uri_path = Uri.parse(files[i].getAbsolutePath());
                vw_dict.put(uri_path, file_name);
                videolist.add(uri_path);
                Log.i("File_TEST", "File is added:" + file_name);
            }
        }

        setVideo(videolist.get(0));
    }

    public void setVideo(Uri uri_path)
    {
        Date date = helper.checkTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
        String strDate = dateFormat.format(date);

        Log.i("Date: ", strDate);
        Log.i("Date: ", "Date Time : "+ dateFormat.format(date));
        String file_line = strDate.concat(" - ");
        file_line = file_line.concat("[INFO::com.barcoop.localplayer.MainActivity]" +
                "::com.barcoop.localplayer.MainActivity$1]");
        file_line = file_line.concat(" - ");

        // TODO: Name of the
        String device_name = helper.getName();
        file_line = file_line.concat("[ " + device_name + " ]");
        file_line = file_line.concat("  재생됨 : ");
        file_line = file_line.concat(vw_dict.get(uri_path));
        Log.i("FILE_LINE: ", file_line);

        // TODO STORE FILE
        updateFile(file_line);

        vw.setVideoURI(uri_path);
        vw.start();
    }


    public void onCompletion(MediaPlayer mediapalyer)
    {
        ++currvideo;
        if (currvideo == videolist.size())
            currvideo = 0;
        setVideo(videolist.get(currvideo));
    }

    class MyListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which)
        {
            if (which == -1) {
                vw.seekTo(0);
                vw.start();
            }
            else {
                ++currvideo;
                if (currvideo == videolist.size())
                    currvideo = 0;
                setVideo(videolist.get(currvideo));
            }
        }
    }

    private void downloadFile() {

        Amplify.Storage.list("Barcooptv/Downloads/",
                result -> {
                    for (StorageItem item : result.getItems()) {
                        Log.i("DOWNLOAD: ", "Item: " + item.getKey());
                        if (item.getSize() != 0) {
                            Log.i("DOWNLOAD: ", "Start download: " + item.getKey());
                            amplify_download(item.getKey());
                        }
                    }
                },
                error -> Log.e("DOWNLOAD: ", "List failure", error)
        );


        Log.i("DOWNLOAD: ", "Download finished.");


    }

    private void amplify_download(String key) {
        int index = key.lastIndexOf("/");
        String fileName = key.substring(index + 1);

        Amplify.Storage.downloadFile(
                key,
                new File(getApplicationContext().getFilesDir() + "/" + fileName),
                StorageDownloadFileOptions.defaultInstance(),
                progress -> Log.i("MyAmplifyApp", "Fraction completed: " + progress.getFractionCompleted()),
                result -> Log.i("MyAmplifyApp", "Successfully downloaded: " + result.getFile().getName()),
                error -> Log.e("MyAmplifyApp",  "Download Failure", error)
        );
        Log.i("Tester", "Ready to go");


        File directory = new File(getApplicationContext().getFilesDir() + "/");
        Log.i("File_TEST", "Directory: " + getApplicationContext().getFilesDir() + "/");
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            Log.i("File_TEST", "FileName:" + files[i].getName());
            long bytes = files[i].length();
            long kilobytes = (bytes / 1024);
            long megabytes = (kilobytes / 1024);
        }
    }

    private void updateFile(String file_line) {
        // Get details for file name
        String device_name = helper.getName();
        Date date = helper.checkTime();
        DateFormat dateFormat = new SimpleDateFormat("yy_MM_dd");
        String strDate = dateFormat.format(date);

        Log.i("DATE_TEST", "Date: " + strDate);


        String root = Environment.getExternalStorageDirectory().toString();

        Log.i("FILE_TEST", "File_root: " + root);

        if (isStoragePermissionGranted()) { // check or ask permission
            File myDir = new File(root, "/Documents");
            if (!myDir.exists()) {
                myDir.mkdirs();
                Log.i("FILE_TEST", "Directory created: " + root + "/saved_logs");
            }

            // open file
            try {
                File file = new File(myDir, device_name + strDate + ".txt");
                FileOutputStream fos;
                if (file.exists()) {
                    fos = new FileOutputStream(file, true);
                }
                //File does not exist
                else{
                    fos = new FileOutputStream(file, false);

                }
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);
                outputStreamWriter.write(file_line);
                outputStreamWriter.write("\n\r");
                outputStreamWriter.close();

                Log.i("FILE_TEST", file_line);
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        } else {
            Log.i("FILE_TEST", "failed.");
        }


    }

    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission is granted");
                Log.i(TAG, "Permission version higher");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.i(TAG, "Permission version lower");
            Log.i(TAG,"Permission is granted");
            return true;
        }
    }

}