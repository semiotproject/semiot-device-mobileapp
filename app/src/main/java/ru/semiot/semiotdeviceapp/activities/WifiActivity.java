package ru.semiot.semiotdeviceapp.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import ru.semiot.semiotdeviceapp.R;

public class WifiActivity extends AppCompatActivity {

    boolean isFirst;
    AlertDialog dialog;
    int prevNetworkId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_page);
        isFirst = getIntent().getStringExtra(Intent.EXTRA_TEXT).equalsIgnoreCase("isFirst");
        final WifiActivity that = this;
        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (isFirst && wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(isFirst ? "Wifi is not connected" : "Choose device Network")
                .setMessage("Connect to WiFi Network")
                .setCancelable(!isFirst)
                .setPositiveButton("WiFi settings", null);
        if (!isFirst) {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    d.dismiss();
                    finish();
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface d) {
                    d.dismiss();
                    finish();
                }
            });
        }
        final AlertDialog dia = builder.create();
        dialog = dia;
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
                                    if (!isFirst)
                                        prevNetworkId = wifiManager.getConnectionInfo().getNetworkId();
                                    wifiManager.disconnect();
                                }
                                startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                            }
                        });
            }
        });
        dia.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
                dialog.dismiss();
                if (!isFirst) {
                    startActivity(new Intent(this, ConfigurationActivity.class).putExtra(Intent.EXTRA_TEXT, prevNetworkId));
                }
                finish();
            }
        }
    }
}
