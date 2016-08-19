package ru.semiot.semiotdeviceapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "MainActivity";
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.find_devices);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        final MainActivity that = this;

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isConnected()) {
            new CoapRequest(new CoapRequestCallback() {
                @Override
                public void onResponse(CoapResponse response) {
                    if (response.isSuccess()) {
                        String message = response.getResponseText();
                        Log.d(LOG_TAG, response.getResponseText());
                        try {
                            JSONObject json = new JSONObject(message);

                            Intent intent = new Intent(that, ConfigurationActivity.class);
                            intent.putExtra(Intent.EXTRA_TEXT, message);
                            startActivity(intent);
                        } catch (JSONException ex) {
                            Log.e(LOG_TAG, ex.getMessage(), ex);

                            showMessage(R.string.failed_to_get_configuration);
                        }
                    } else {
                        Log.d(LOG_TAG, "Response code: " + response.getCode().toString());
                        showMessage(R.string.failed_to_get_configuration);
                    }
                }

                @Override
                public void onError() {
                    showMessage(R.string.failed_to_get_configuration);
                }
            }).execute("coap://192.168.4.1/config");
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
