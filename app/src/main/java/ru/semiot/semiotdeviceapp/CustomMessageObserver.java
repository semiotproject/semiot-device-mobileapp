package ru.semiot.semiotdeviceapp;

import android.util.Log;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomMessageObserver implements MessageObserver {

    BroadcastCallback callback;
    private static final String LOG_TAG = "CustomMessageObserver";

    public CustomMessageObserver(BroadcastCallback callback) {
        super();
        this.callback = callback;
    }

    @Override
    public void onRetransmission() {
        Log.d(LOG_TAG, "onRetransmission");
    }

    @Override
    public void onResponse(Response response) {
        Log.d(LOG_TAG, "OnResponse");
        if (response.getCode().equals(CoAP.ResponseCode.CONTENT)) {
            try {
                Log.d(LOG_TAG, response.getPayloadString());
                JSONObject json = new JSONObject(response.getPayloadString());
                callback.addDevice(buildURI(response.getSource().toString(), response.getSourcePort()),
                        json.getString("identifier"),
                        json.getJSONObject("label").getString("@value"));
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Can't parse response to json");
            }
        } else {
            Log.w(LOG_TAG, "Bad request code. It is " + response.getCode().toString());
        }
    }

    @Override
    public void onAcknowledgement() {
        Log.d(LOG_TAG, "onAcknowledgement");
    }

    @Override
    public void onReject() {
        Log.d(LOG_TAG, "onReject");
    }

    @Override
    public void onTimeout() {
        Log.d(LOG_TAG, "onTimeout");
    }

    @Override
    public void onCancel() {
        Log.d(LOG_TAG, "onCancel");
    }

    public void setCallback(BroadcastCallback callback) {
        this.callback = callback;
    }

    private String buildURI(String source, int port) {
        return "coap://" + source.substring(1) + ":" + port;
    }
}
