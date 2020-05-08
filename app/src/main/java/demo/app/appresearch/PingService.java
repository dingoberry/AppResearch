package demo.app.appresearch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class PingService extends Service {

    private static final String TAG = "PingServer";

    private boolean mIsPingRunning;

    private SimpleDateFormat mSdf = new SimpleDateFormat("hh:mm:ss.SSS");

    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (null != nm) {
                String channelId = "demo.app.appresearch.guard";
                if (null == nm.getNotificationChannel(channelId)) {
                    nm.createNotificationChannel(new NotificationChannel(channelId,
                            "searchGuard", NotificationManager.IMPORTANCE_DEFAULT));
                }
                notification = new Notification.Builder(this, channelId).build();
            } else {
                notification = null;
            }
        } else {
            notification = new Notification();
        }

        if (null != notification) {
            startForeground(new Random().nextInt(100000) + 1, notification);
        }

        // Add Active Protecting!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                String pkgName = getPackageName();
                if (!pm.isIgnoringBatteryOptimizations(pkgName)) {
                    Intent it = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    it.setData(Uri.parse("package:" + pkgName));
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(it);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsPingRunning) {
            mIsPingRunning = true;

            new Thread() {
                @Override
                public void run() {
                    Process process = null;
                    try {
                        process = Runtime.getRuntime().exec("ping publish1.tga.qq.com");
                        final CountDownLatch latch = new CountDownLatch(2);

                        final Process p = process;
                        new Thread() {
                            @Override
                            public void run() {
                                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                    String line;
                                    while (mIsPingRunning && null != (line = r.readLine())) {
                                        print(line);
                                    }
                                } catch (final IOException e) {
                                    Log.w(TAG, e);
                                }
                                latch.countDown();
                            }
                        }.start();

                        new Thread() {
                            @Override
                            public void run() {
                                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                                    String line;
                                    while (mIsPingRunning && null != (line = r.readLine())) {
                                        print(line);
                                    }
                                } catch (final IOException e) {
                                    Log.w(TAG, e);
                                }
                                latch.countDown();
                            }
                        }.start();

                        latch.await();
                    } catch (final IOException | InterruptedException e) {
                        Log.w(TAG, e);
                    } finally {
                        if (null != process) {
                            process.destroy();
                        }
                    }
                }
            }.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void print(String info) {
        Log.i(TAG, String.format("%s:%s", mSdf.format(System.currentTimeMillis()), info));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }
}
