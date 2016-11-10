package ru.semiot.semiotdeviceapp;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.eclipse.californium.tools.ResourceDirectory;

public class RDService extends IntentService{
    private ResourceDirectory resourceDirectory;
    public RDService(){
        super(RDService.class.getName());
        Log.d("RDService","Constructor is run");
        resourceDirectory = new ResourceDirectory();
        resourceDirectory.start();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //while(true);
    }
}
