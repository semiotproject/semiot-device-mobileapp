package ru.semiot.semiotdeviceapp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import ru.semiot.semiotdeviceapp.BroadcastCallback;
import ru.semiot.semiotdeviceapp.CustomMessageObserver;
import ru.semiot.semiotdeviceapp.R;
import ru.semiot.semiotdeviceapp.ScrollDetectingListView;
import ru.semiot.semiotdeviceapp.Utils;

public class MainActivity extends AppCompatActivity implements BroadcastCallback {

    private static final String LOG_TAG = "MainActivity";
    private HashMap<String, Pair<String, String>> devices;
    private ScrollDetectingListView listDevices;
    ArrayAdapter<String> adapter;

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled() || !wifiManager.getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
            startActivity(new Intent(this, WifiActivity.class).putExtra(Intent.EXTRA_TEXT, "isFirst"));
        }
        devices = new HashMap<>();
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        listDevices = (ScrollDetectingListView) findViewById(R.id.listDevices);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final Activity that = this;
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long _id) {
                String label = (String) listDevices.getItemAtPosition(position);
                String user, pass, id, uri;
                Pair<String, String> pair = getDevice(label);
                id = pair.first;
                uri = pair.second;
                JSONObject json = Utils.getSettingsByDeviceId(Utils.readFile(that), id);
                if (json == null)
                    ;// TODO: We should ask user about username and password and offer to save it
                user = json.optString("username");
                pass = json.optString("password");
            }
        });
        listDevices.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int mInitialScroll = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //получаем текущую позицию скролла
                int scrolledOffset = listDevices.getVerticalScrollOffset();
                if (scrolledOffset != mInitialScroll) {
                    boolean scrollUp = (scrolledOffset - mInitialScroll) < 0;
                    mInitialScroll = scrolledOffset;
                    if (scrollUp) {
                        //показываем кнопку
                        fab.show();
                    } else {
                        //прячем
                        fab.hide();
                    }
                }
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(that, WifiActivity.class).putExtra(Intent.EXTRA_TEXT, "not"));
                //startActivity(new Intent(that, ConfigurationActivity.class));
            }
        });
        ArrayList<String> labels = new ArrayList<String>(devices.keySet());
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, labels);
        listDevices.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://ru.semiot.semiotdeviceapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onResume() {
        //TODO: allow manually update list of devices
        super.onResume();
        adapter.clear();
        devices.clear();
        Request request = new Request(CoAP.Code.GET);
        String broadcastUri = Formatter.formatIpAddress(((WifiManager) getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().gateway);
        broadcastUri = broadcastUri.substring(0, broadcastUri.lastIndexOf('.') + 1) + "255/";
        broadcastUri = "coap://" + broadcastUri /*+ "/.well-known/core"*/;

        Log.d(LOG_TAG, "BroadcastURI is " + broadcastUri);
        request.setURI(broadcastUri).setMulticast(true);
        request.addMessageObserver(new CustomMessageObserver(this));
        request.send();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.clear();
        devices.clear();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://ru.semiot.semiotdeviceapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private Pair<String, String> getDevice(String label) {
        String id, uri;
        for (String key : devices.keySet()) {
            if (devices.get(key).second.equalsIgnoreCase(label))
                return new Pair<String, String>(key, devices.get(key).first);
        }
        return null;
    }

    @Override
    public void addDevice(final String url, final String id, final String label) {
        if (!devices.containsKey(id)) {
            devices.put(id, new Pair<String, String>(url, label));
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.add(label);
                }
            });
        }
    }
}
