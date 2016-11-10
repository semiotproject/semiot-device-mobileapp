package ru.semiot.semiotdeviceapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eclipse.californium.core.CoapClient;
import org.json.JSONException;
import org.json.JSONObject;

import ru.semiot.semiotdeviceapp.R;

public class TestimonyActivity extends AppCompatActivity {

    private static final String temp = "temperature";
    private static final String hum = "humidity";
    private static final String relay = "relayAction";

    LinearLayout root;
    private LayoutInflater inflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testimony);
        inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = (LinearLayout) findViewById(R.id.root);
        String URI = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        final CoapClient client = new CoapClient();
        //URI = "coap:/" + URI;
        client.setURI(URI);

        try {
            JSONObject json = new JSONObject(client.get().getResponseText());
            if (json.has(temp)) {
                String subURI = URI + json.getString(temp);
                client.setURI(subURI);
                JSONObject testimony = new JSONObject(client.get().getResponseText());
                String value = String.valueOf(testimony.get("value"));
                String code = String.valueOf(testimony.get("unitCode"));

                LinearLayout l = (LinearLayout) inflater.inflate(R.layout.card, null, false);
                ((TextView) l.findViewById(R.id.t_label)).setText("Temperature");
                ((TextView) l.findViewById(R.id.t_value)).setText("Current value is " + value);
                ((TextView) l.findViewById(R.id.t_code)).setText(code);
                root.addView(l);

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
                //((CardView) l.getParent()).setBackgroundColor(R.color.colorAccent);
/*
                if(client.setURI(subURI).get().getResponseText().contains("TurnOn")){
                    //((CardView)turnOn.getParent()).setBackgroundColor(R.color.colorAccent);
                    ((CardView)turnOn.getParent()).setEnabled(false);

                }
                else{
                    //((CardView)turnOff.getParent()).setBackgroundColor(R.color.colorAccent);
                    ((CardView)turnOff.getParent()).setEnabled(false);
                }
                */
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
            if (json.has(hum)) {
                String subURI = URI + json.getString(hum);
                client.setURI(subURI);
                JSONObject testimony = new JSONObject(client.get().getResponseText());
                String value = String.valueOf(testimony.get("value"));
                String code = String.valueOf(testimony.get("unitCode"));

                LinearLayout l = (LinearLayout) inflater.inflate(R.layout.card, null, false);
                ((TextView) l.findViewById(R.id.t_label)).setText("Humidity");
                ((TextView) l.findViewById(R.id.t_value)).setText("Current value is " + value);
                ((TextView) l.findViewById(R.id.t_code)).setText(code);
                root.addView(l);
            }
            if (root.getChildCount() == 0) {
                finish();
            }

        } catch (JSONException e) {
            finish();
        }
    }
}
