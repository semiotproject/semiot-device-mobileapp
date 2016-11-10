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

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import ru.semiot.semiotdeviceapp.BroadcastCallback;
import ru.semiot.semiotdeviceapp.CustomMessageObserver;
import ru.semiot.semiotdeviceapp.R;
import ru.semiot.semiotdeviceapp.RDService;
import ru.semiot.semiotdeviceapp.ScrollDetectingListView;

public class MainActivity extends AppCompatActivity implements BroadcastCallback {

    private static final String LOG_TAG = "MainActivity";
    private HashMap<String, Pair<String, String>> devices;
    private ScrollDetectingListView listDevices;
    ArrayAdapter<String> adapter;
    //ResourceDirectory resourceDirectory;
    private GoogleApiClient client;
    private Intent background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        background = new Intent(this, RDService.class);
        startService(background);
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
                /*JSONObject json = Utils.getSettingsByDeviceId(Utils.readFile(that), id);
                if (json == null)
                    ;// TODO: We should ask user about username and password and offer to save it
                user = json.optString("username");
                pass = json.optString("password");
                */
                startActivity(new Intent(that, TestimonyActivity.class).putExtra(Intent.EXTRA_TEXT, uri));
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendSleepingDevice();
            }
        }).run();

    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.clear();
        devices.clear();
        stopService(background);
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://ru.semiot.semiotdeviceapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private void appendSleepingDevice() {
        String URI = Formatter.formatIpAddress(((WifiManager) getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().ipAddress);
        URI = "coap://" + URI /*+ "/.well-known/core?rt=core.rd-cache"*/;
        final int count = 10;
        int i = 0;
        CoapClient cl = new CoapClient();

        CoapResponse resp;
        cl.setURI(URI + "/.well-known/core?rt=core.rd-cache");
        do {
            if (i >= count) {
                return;
            }
            i++;
            resp = cl.get();
        } while (resp == null);
        String rdCachePath = LinkFormat.parse(resp.getResponseText())
                .toArray(new WebLink[]{})[0].getURI();
        i = 0;
        cl.setURI(URI + rdCachePath);
        do {
            if (i >= count) {
                return;
            }
            i++;
            resp = cl.get();
        } while (resp == null);

        Set<WebLink> res = LinkFormat.parse(resp.getResponseText());
        for (WebLink link : res) {
            JSONObject json = null;
            try {
                json = new JSONObject(cl.setURI(URI + rdCachePath + link.getURI()).get().getResponseText());
                this.addDevice(URI + rdCachePath + link.getURI(),
                        json.getString("identifier"),
                        json.getJSONObject("label").getString("@value"));
            } catch (JSONException e) {
            }
        }
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
