package io.zjw.testblelib.dfu;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;

import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {
    public DfuService() {
    }

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return DfuNotificationActivity.class;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
