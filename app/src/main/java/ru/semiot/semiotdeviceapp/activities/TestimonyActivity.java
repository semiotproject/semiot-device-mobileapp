package ru.semiot.semiotdeviceapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ru.semiot.semiotdeviceapp.R;
import ru.semiot.semiotdeviceapp.Utils;

public class TestimonyActivity extends AppCompatActivity {

    private static final String temp = "temperature";
    private static final String hum = "humidity";
    private static final String relay = "relayAction";
    private final int MAX_REPEAT_COUNT = 10;
    LinearLayout root;
    private LayoutInflater inflater;
    final CoapClient client = new CoapClient();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testimony);
        inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = (LinearLayout) findViewById(R.id.testimonies);
        root.setPadding(0, 0, 0, 50);
        final String URI = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("No title");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        //URI = "coap:/" + URI;
        client.setURI(URI);

        //Constrain a device info
        ((TextView) findViewById(R.id.listId)).setText("Device information");
        ((TextView) findViewById(R.id.listId)).setTextSize(20);
        ((TextView) findViewById(R.id.listId)).setTypeface(null, Typeface.BOLD);
        ;
        LinearLayout deviceInfo = (LinearLayout) findViewById(R.id.childLayout);
        deviceInfo.addView(createReadableRow("URI", URI));
        try {
            String payload = getResponseAsString();
            if(payload == null){
                Utils.showMessage(R.string.failed_to_connect_with_device, this);
                finish();
            }
            final JSONObject json = new JSONObject(payload);
            if (json.has("label") && json.getJSONObject("label").has("@value")) {
                toolbar.setTitle(json.getJSONObject("label").getString("@value"));
            }
            deviceInfo.addView(createReadableRow("Identifier", json.getString("identifier")));
            deviceInfo.addView(createReadableRow("Location", json.getJSONObject("location").getString("label")));//TODO: Change to @value or something else
            //TODO: Also add a field with username and password

            if (json.has(temp)) {
                //for(int i = 0; i < 10; i++) {
                    CardView l = (CardView) inflater.inflate(R.layout.sensor_output, null, false);
                boolean f = true;
                    l.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                String subURI = URI + json.getString(temp);
                                client.setURI(subURI);
                                JSONObject testimony = new JSONObject(getResponseAsString());
                                String value = String.valueOf(testimony.get("value"));
                                String code = String.valueOf(testimony.get("unitCode"));
                                setObservation((CardView) v, "Temperature", value, code.equalsIgnoreCase("cel") ? "°C" : "F");
                            } catch (JSONException ex) {

                            }
                        }
                    });
                    l.callOnClick();
                    root.addView(l);
                //}
            }

            if (json.has(relay)) {
                final String subURI = URI + json.getString(relay);
                final LinearLayout turnOn = (LinearLayout) inflater.inflate(R.layout.card, null, false).findViewById(R.id.t_layout);
                turnOn.removeAllViews();
                TextView turnOnText = new TextView(this);
                turnOnText.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.MATCH_PARENT));
                turnOnText.setTextSize(20);
                turnOnText.setPadding(4, 10, 4, 10);
                turnOnText.setText("Turn on the light");
                turnOn.addView(turnOnText);


                final LinearLayout turnOff = (LinearLayout) inflater.inflate(R.layout.card, null, false).findViewById(R.id.t_layout);
                turnOff.removeAllViews();
                TextView turnOffText = new TextView(this);
                turnOffText.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.MATCH_PARENT));
                turnOffText.setTextSize(20);
                turnOffText.setPadding(4, 10, 4, 10);
                turnOffText.setText("Turn off the light");
                turnOff.addView(turnOffText);
                ((CardView) turnOn.getParent()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        client.setURI(subURI).put("{ \"@context\": \"http://external/doc#\", \"@type\": \"TurnOnAction\" }", 50);
                    }
                });
                ((CardView) turnOff.getParent()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        client.setURI(subURI).put("{ \"@context\": \"http://external/doc#\", \"@type\": \"TurnOffAction\" }", 50);
                    }
                });
                root.addView((LinearLayout) turnOn.getParent().getParent());
                root.addView((LinearLayout) turnOff.getParent().getParent());
            }
            //if (json.has(hum)) {
                CardView l = (CardView) inflater.inflate(R.layout.sensor_output, null, false);
                l.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            String subURI = URI + "/humidityValue";
                            //String subURI = URI + json.getString(hum);
                            client.setURI(subURI);
                            JSONObject testimony = new JSONObject(getResponseAsString());
                            String value = String.valueOf(testimony.get("value"));
                            String code = String.valueOf(" %");
                            setObservation((CardView) v, "Humidity", value, code);
                        } catch (JSONException ex) {

                        }
                    }
                });
                l.callOnClick();
                root.addView(l);
            //}
            if (root.getChildCount() == 0) {
                finish();
            }

        } catch (JSONException e) {
            finish();
        }
    }

    private LinearLayout createReadableRow(String key, String value) {
        if (inflater == null) {
            inflater = (LayoutInflater) this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.readable_row, null, false);
        ((TextView) row.findViewById(R.id.key)).setText(key);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        return row;
    }

    private void setObservation(CardView view, String label, String value, String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        ((TextView) view.findViewById(R.id.obs_label)).setText(label);
        ((TextView) view.findViewById(R.id.obs_val)).setText(value);
        ((TextView) view.findViewById(R.id.obs_val_type)).setText(type);
        ((TextView) view.findViewById(R.id.obs_time)).setText(sdf.format(new Date()));
    }

    private void addView(View v){

    }

    @Nullable
    private String getResponseAsString(){
        CoapResponse response = null;
        for(int i=0; i < MAX_REPEAT_COUNT && response == null; i++){
            response = client.get();
        }
        return response==null?null:response.getResponseText();
    }
}
