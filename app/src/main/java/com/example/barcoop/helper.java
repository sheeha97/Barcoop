package com.example.barcoop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.*;
import java.util.*;

import android.bluetooth.BluetoothAdapter;
import android.net.Uri;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class helper {
    public static Date checkTime()
    {
        Date currentTime = Calendar.getInstance().getTime();
        return currentTime;
    }

    public static String getName() {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        if(myDevice==null){
            // device does not support bluetooth
            return "";
        }

        String deviceName = myDevice.getName();
        Log.i("Device Name: ", deviceName);
        return deviceName;
    }
}
