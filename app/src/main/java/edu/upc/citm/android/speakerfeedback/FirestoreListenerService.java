package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class FirestoreListenerService extends Service {

    private boolean active;
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("SpeakerFeedback","FirestoreListenerService.onCreate");
        db.collection("rooms").document("testroom").collection("polls").whereEqualTo("open", true)
                .addSnapshotListener(pollslistener);

    }

    private FirebaseFirestore db=FirebaseFirestore.getInstance();
    private EventListener<QuerySnapshot> pollslistener= new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre la llista de polls", e);
                return;
            }
            Log.i("SpeakerFeedback", "Service: received open polls");
            for (DocumentSnapshot doc : documentSnapshots) {
                Poll poll = doc.toObject(Poll.class);
                poll.setPoll_id(doc.getId());
                CreatePollNotification(poll);
            }
        }
    };
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!active) {
            active = true;
            CreateForegroundNotification();
        }
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
    private void CreatePollNotification(Poll poll){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("New Poll:" + String.format(poll.getQuestion()))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{100, 200, 100, 200, 100, 200})
                .setAutoCancel(true)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, notification);
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
