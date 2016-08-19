package ru.semiot.semiotdeviceapp;

import org.eclipse.californium.core.CoapResponse;

public abstract class CoapRequestCallback {

    public void onResponse(CoapResponse response) {}

    public void onError() {}

    public void onComplete() {}
}
