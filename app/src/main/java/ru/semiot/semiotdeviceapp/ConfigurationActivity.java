package ru.semiot.semiotdeviceapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class ConfigurationActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "ConfigurationActivity";
    private LinearLayout rootLayout;
    private String message;
    private JSONObject json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        this.rootLayout = (LinearLayout) findViewById(R.id.layout);
        this.message = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        try {
            this.json = new JSONObject(message);

            Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                if(!key.equalsIgnoreCase("@context")) {
                    LinearLayout childLayout = new LinearLayout(this);

                    TextView fieldName = new TextView(this);
                    fieldName.setText(key);

                    EditText fieldValue = new EditText(this);
                    fieldValue.setTag(key);
                    fieldValue.setText(json.getString(key));

                    childLayout.addView(fieldName);
                    childLayout.addView(fieldValue);

                    rootLayout.addView(childLayout);
                }
            }

            Button button = new Button(this);
            button.setText(R.string.save_configuration);
            button.setOnClickListener(this);

            rootLayout.addView(button);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "Save button pressed!");

        for(int i=0; i < rootLayout.getChildCount(); i++) {
            View child = rootLayout.getChildAt(i);
            if(child instanceof LinearLayout) {
                LinearLayout field = (LinearLayout) child;
                EditText fieldValue = (EditText) field.getChildAt(1);
                String key = (String) fieldValue.getTag();
                String value = fieldValue.getText().toString();
                Log.d(LOG_TAG, "Found: " + key + " value: " + value);
                try {
                    json.put(key, value);
                } catch (JSONException ex) {
                    Log.d(LOG_TAG, ex.getMessage(), ex);
                }
            }
        }

        Log.d(LOG_TAG, "Configuration: " + json.toString());

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isConnected()) {
            new CoapRequest(new CoapRequestCallback() {
                @Override
                public void onResponse(CoapResponse response) {
                    Log.d(LOG_TAG, "Saved!");
                }

                @Override
                public void onError() {
                    Log.d(LOG_TAG, "Error!");
                }

                @Override
                public void onComplete() {
                    finish();
                }
            }).execute("coap://192.168.4.1/config", CoAP.Code.PUT.name(), json.toString());
        } else {
            showMessage(R.string.wifi_not_connected);
        }
    }

    private void showMessage(int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();
    }
}
