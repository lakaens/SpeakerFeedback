package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class FirestoreListenerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("SpeakerFeedback","FirestoreListenerService.onCreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CreateForegroundNotification();
       return START_NOT_STICKY;
    }

    private void CreateForegroundNotification() {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,0);
        //TODO: Crear una notificacio i cridar startforeground perque el servei segueixi funcionant
        Notification notification= new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(String.format("Connectat a testroom"))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);
    }

    @Override
    public void onDestroy() {
        Log.i("SpeakerFeedback","FirestoreListenerService.onDestroy");

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
