package com.example.barcoop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {
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

        String file_line = strDate.concat(" - ");
        file_line = file_line.concat("[INFO::com.barcoop.localplayer.MainActivity]" +
                "::com.barcoop.localplayer.MainActivity$1]");
        file_line = file_line.concat(" - ");

        String device_name = helper.getName();
        file_line = file_line.concat("[ " + device_name + " ]");
        file_line = file_line.concat("  재생됨 : ");
        file_line = file_line.concat(vw_dict.get(uri_path));

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

    }

    private void updateFile(String file_line) {
        // Get details for file name
        String device_name = helper.getName();
        Date date = helper.checkTime();
        DateFormat dateFormat = new SimpleDateFormat("yy_MM_dd");
        String strDate = dateFormat.format(date);

        String root = Environment.getExternalStorageDirectory().toString();

        if (isStoragePermissionGranted()) { // check or ask permission
            File myDir = new File(root, "/Documents");
            if (!myDir.exists()) {
                myDir.mkdirs();
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
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation

            return true;
        }
    }

}