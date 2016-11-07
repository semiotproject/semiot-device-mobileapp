package ru.semiot.semiotdeviceapp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.IllegalFormatException;
import java.util.Iterator;

import ru.semiot.semiotdeviceapp.CoapRequest;
import ru.semiot.semiotdeviceapp.CoapRequestCallback;
import ru.semiot.semiotdeviceapp.R;
import ru.semiot.semiotdeviceapp.Utils;

public class ConfigurationActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = "ConfigurationActivity";
    private LinearLayout rootLayout;
    private ExpandableListView expandableListView;
    private JSONObject json;
    private LayoutInflater inflater;
    int prevNetworkId;

    String user, pass, id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        json = new JSONObject();
        prevNetworkId = getIntent().getIntExtra(Intent.EXTRA_TEXT, -1);
        this.rootLayout = (LinearLayout) findViewById(R.id.layout);
        final Activity that = this;
        new CoapRequest(new CoapRequestCallback() {
            @Override
            public void onResponse(CoapResponse response) {
                if (response.isSuccess()) {
                    String message = response.getResponseText();
                    Log.d(LOG_TAG, response.getResponseText());
                    try {

                        fillLayout(new JSONObject(message), rootLayout);
                    } catch (JSONException e) {
                        Utils.showMessage(R.string.failed_to_get_configuration, that);
                    }

                } else {
                    Log.d(LOG_TAG, "Response code: " + response.getCode().toString());
                    Utils.showMessage(R.string.failed_to_get_configuration, that);
                }
            }

            @Override
            public void onError() {
                Utils.showMessage(R.string.failed_to_get_configuration, that);
            }
        }).execute(Utils.DEFAULT_CONFIG_URI);


    }


    private void fillLayout(JSONObject json, LinearLayout root) throws JSONException {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.contains("@")) {
                if (json.get(key) instanceof JSONObject) {
                    inflater.inflate(R.layout.entry_list, root, true);
                    ((TextView) findViewById(R.id.listId)).setText(key);
                    LinearLayout child = (LinearLayout) findViewById(R.id.childLayout);
                    child.setTag(key);
                    fillLayout(json.getJSONObject(key), child);
                } else {
                    LinearLayout row = (LinearLayout) inflater.inflate(R.layout.row, null, false);
                    ((TextView) row.findViewById(R.id.key)).setText(key);
                    ((EditText) row.findViewById(R.id.value)).setTag(key);
                    ((EditText) row.findViewById(R.id.value)).setText(json.get(key).toString());
                    root.addView(row);
                }
            } else {
                this.json.put(key, json.get(key));
            }
        }
    }

    private JSONObject parseLayout(LinearLayout root) throws JSONException, IllegalStateException {
        JSONObject json = new JSONObject();
        for (int i = 0; i < root.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) root.getChildAt(i);
            if (child.findViewById(R.id.value) != null) {
                EditText field = (EditText) child.findViewById(R.id.value);
                String key, value;
                key = field.getTag().toString();
                value = field.getText().toString();
                if (key.equalsIgnoreCase(Utils.USERNAME_KEY))
                    user = value;
                if (key.equalsIgnoreCase(Utils.PASSWORD_KEY))
                    pass = value;
                json.put(key, value);
            } else if (child.findViewById(R.id.childLayout) != null) {
                LinearLayout childLayout = (LinearLayout) child.findViewById(R.id.childLayout);
                json.put(childLayout.getTag().toString(), parseLayout(childLayout));
            } else {
                throw new IllegalStateException();
            }
        }
        return json;
    }

    private void parseMessage(String message) {
        try {
            this.json = new JSONObject(message);

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!key.contains("@")) {
                    LinearLayout childLayout = new LinearLayout(this);
                    childLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    childLayout.setOrientation(LinearLayout.HORIZONTAL);

                    TextView fieldName = new TextView(this);
                    fieldName.setText(key);
                    fieldName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    EditText fieldValue = new EditText(this);
                    fieldValue.setTag(key);
                    fieldValue.setText(json.getString(key));
                    fieldValue.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    childLayout.addView(fieldName);
                    childLayout.addView(fieldValue);

                    rootLayout.addView(childLayout);
                }
            }
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
            Utils.showMessage(R.string.failed_to_get_configuration, this);
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "Save button pressed!");
        try {
            JSONObject config = parseLayout(rootLayout);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                config.put(key, json.get(key));
            }
            Log.d(LOG_TAG, "Configuration: " + json.toString());

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected()) {
                CoapClient client = new CoapClient(Utils.DEFAULT_URI);
                Request req = new Request(CoAP.Code.GET);
                CoapResponse response = client.get();
                if (response != null && response.isSuccess()) {
                    try {
                        JSONObject json = new JSONObject(response.getResponseText());
                        id = json.getString(Utils.DEVICE_IDENTIFIER);
                    } catch (JSONException e) {
                        Log.w(LOG_TAG, "Can't parse json from /doc!");
                        finish();
                    }
                } else {
                    Log.d(LOG_TAG, "Request is not success when requesting /doc!");
                    finish();
                }

                new CoapRequest(new CoapRequestCallback() {
                    @Override
                    public void onResponse(CoapResponse response) {
                        if (response.isSuccess()) {
                            Log.d(LOG_TAG, "Saved!");
                        } else {
                            Log.w(LOG_TAG, "Error when saving configuration! " + response.getCode().toString());
                            finish();
                        }
                    }

                    @Override
                    public void onError() {
                        Log.w(LOG_TAG, "Error when saving configuration!");
                        finish();
                    }

                    @Override
                    public void onComplete() {
                    }
                }).execute(Utils.DEFAULT_CONFIG_URI, CoAP.Code.PUT.name(), config.toString());
                JSONObject data = new JSONObject();
                try {
                    data.put("id", id)
                            .put("settings",
                                    new JSONObject().put("username", user)
                                            .put("password", pass));
                    Utils.saveData(data, this);
                } catch (JSONException e) {
                    Log.w(LOG_TAG, "Can't convert data!");
                }
                finish();
            } else {
                Utils.showMessage(R.string.wifi_not_connected, this);
            }
        } catch (JSONException e) {
            Utils.showMessage(R.string.failed_to_get_configuration, this);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.getConnectionInfo().getNetworkId() != prevNetworkId) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(prevNetworkId, true);
                wifiManager.reconnect();
            }
        }
    }


}
