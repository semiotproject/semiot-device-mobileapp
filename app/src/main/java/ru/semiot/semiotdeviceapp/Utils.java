package ru.semiot.semiotdeviceapp;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {
    public static final String DEFAULT_URI = "coap://192.168.4.1/";
    public static final String DEFAULT_CONFIG_URI = DEFAULT_URI + "config";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String CONFIG_FILENAME = "settings";
    public static final String DEVICE_IDENTIFIER = "identifier";

    private static final String LOG_TAG = "Utils";

    public static void showMessage(int message, final Activity activity) {
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        activity.finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        activity.finish();
                    }
                })
                .create().show();
    }

    public static void saveData(JSONObject data, final Activity activity) {
        JSONArray prevData = readFile(activity);
        if (prevData != null) {
            activity.deleteFile(CONFIG_FILENAME);
        } else {
            prevData = new JSONArray();
        }
        JSONObject jo = getSettingsByDeviceId(prevData, data.optString("id"));
        if (jo != null) {
            jo.remove("settings");
            try {
                jo.put("settings", data.getJSONObject("settings"));
            } catch (JSONException ex) {
            }
        } else {
            prevData.put(data);
        }
        try {
            FileOutputStream outputStream = activity.openFileOutput(CONFIG_FILENAME, activity.MODE_PRIVATE);
            outputStream.write(prevData.toString().getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't write to file");
        } catch (IOException e) {
            Log.w(LOG_TAG, "Can't write to file");
        }

    }

    public static JSONObject getSettingsByDeviceId(JSONArray array, String id) {
        if (array == null) return null;
        JSONObject obj = null;
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONObject(i).optString("id").equalsIgnoreCase(id)) {
                return array.optJSONObject(i);
            }
        }
        return obj;
    }

    public static JSONArray readFile(Activity activity) {
        JSONArray fileData = null;
        FileInputStream fileInputStream;
        BufferedReader reader;
        StringBuilder stringBuilder;
        try {
            fileInputStream = activity.openFileInput(CONFIG_FILENAME);
            try {
                reader = new BufferedReader(new InputStreamReader(fileInputStream));
                stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } finally {
                fileInputStream.close();
            }
            fileData = new JSONArray(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return fileData;
        }
    }
}
