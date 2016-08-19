package ru.semiot.semiotdeviceapp;

import android.os.AsyncTask;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

public class CoapRequest extends AsyncTask<String, Void, CoapResponse> {

    private final CoapRequestCallback callback;

    public CoapRequest(CoapRequestCallback callback) {
        this.callback = callback;
    }

    @Override
    protected CoapResponse doInBackground(String... args) {
        CoapClient client = new CoapClient(args[0]);
        if(args.length > 1) {
            CoAP.Code method = CoAP.Code.valueOf(args[1]);
            switch (method) {
                case GET:
                    return client.get();
                case PUT:
                    return client.put(args[2], MediaTypeRegistry.APPLICATION_JSON);
                default:
                    return client.get();
            }
        } else {
            return client.get();
        }
    }

    @Override
    protected void onPostExecute(CoapResponse result) {
        if (result != null) {
            callback.onResponse(result);
        } else {
            callback.onError();
        }
        callback.onComplete();
    }
}
