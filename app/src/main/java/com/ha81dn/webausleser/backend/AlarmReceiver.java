package com.ha81dn.webausleser.backend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.widget.Toast;

/**
 * Created by har on 21.04.2015.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "AlarmTriggered", Toast.LENGTH_LONG).show();

        Intent i = new Intent(context, WebService.class);
        context.startService(i);
    }
}
