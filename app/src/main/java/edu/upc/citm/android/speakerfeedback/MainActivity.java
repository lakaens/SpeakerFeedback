package edu.upc.citm.android.speakerfeedback;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;
    private static final int INSERT_ROOM_ID = 1;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;
    private ListenerRegistration roomregistration, usersregistration, pollsregistration;
    private List<Poll> polls = new ArrayList<>();
    private TextView num_users;
    private RecyclerView polls_view;
    private Adapter adapter;
    private String room_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter=new Adapter();

        num_users=findViewById(R.id.num_usersview);
        polls_view=findViewById(R.id.pollsview);
        polls_view.setLayoutManager(new LinearLayoutManager(this));
        polls_view.setAdapter(adapter);


        getOrRegisterUser();

    }
    private void startFirestoreListenerService(){
        Intent intent = new Intent(this,FirestoreListenerService.class);
        intent.putExtra("room", "room_id");
        startService(intent);
    }
    private void stopFirestoreListenerService(){
        Intent intent= new Intent(this, FirestoreListenerService.class);
        stopService(intent);
    }
   private EventListener<DocumentSnapshot> roomListener=new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre rooms/testroom", e);
                return;
            }
                if(!documentSnapshot.contains("open") || !documentSnapshot.getBoolean("open")) {
                    stopFirestoreListenerService();
                    db.collection("users").document(userId).update(
                            "room", FieldValue.delete());
                    SelectRoom();
                    finish();
                }
                else {
                    String name = documentSnapshot.getString("name");
                    setTitle(name);
                }
            }

    };
    private EventListener<QuerySnapshot> usersListener=new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if(e!=null){
                Log.e("SpeakerFeedback","Error al rebre usuaris dins d'un room",e);
                return;
            }
            num_users.setText(String.format("%d", documentSnapshots.size()));
        }
    };
    private EventListener<QuerySnapshot> pollListener=new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if(e!=null){
                Log.e("SpeakerFeedback","Error al rebre usuaris dins d'un room",e);
                return;
            }
            polls.clear();
            for(DocumentSnapshot doc: documentSnapshots){
                Poll poll = doc.toObject(Poll.class);
                poll.setHash_question(doc.getId());
                polls.add(poll);
            }
            adapter.notifyDataSetChanged();
        }
    };
    protected void onStart(){

        if(room_id!= null) {
            roomregistration = db.collection("rooms").document(room_id).addSnapshotListener(roomListener);
            usersregistration = db.collection("users").whereEqualTo("room", room_id).addSnapshotListener(usersListener);
            pollsregistration = db.collection("rooms").document(room_id).collection("polls").orderBy("start", Query.Direction.DESCENDING).addSnapshotListener(pollListener);
        }
        super.onStart();
    }



    private void getOrRegisterUser() {
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            // Hem de registrar l'usuari, demanem el nom
            Intent intent = new Intent(this, RegisterUserActivity.class);
            startActivityForResult(intent, REGISTER_USER);
            Toast.makeText(this, "Encara t'has de registrar", Toast.LENGTH_SHORT).show();
        } else {
            // Ja està registrat, mostrem el id al Log
            Log.i("SpeakerFeedback", "userId = " + userId);

        }
    }

    private void enterRoom() {db.collection("users").document(userId).update("room", "room_id","last_active", new Date());
        startFirestoreListenerService();
    }

    private void exitRoom() {
        stopFirestoreListenerService();
        db.collection("users").document(userId).update("room", FieldValue.delete());
        SelectRoom();
    }

    private void SelectRoom() {
        Intent intent = new Intent(this, Enter_Room.class);
        startActivityForResult(intent,INSERT_ROOM_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_USER:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    registerUser(name);
                    SelectRoom();
                } else {
                    Toast.makeText(this, "Has de registrar un nom", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case INSERT_ROOM_ID:
                room_id=data.getStringExtra("room_id");
                startFirestoreListenerService();
                enterRoom();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_out_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.users_item:
                Intent intent = new Intent(this, UsersListActivity.class);
                startActivity(intent);
                break;
            case R.id.log_out_item:
                exitRoom();
                db.collection("users").document(userId).delete();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    deleteSharedPreferences("config");
                }
                Intent intent2 = new Intent(this, RegisterUserActivity.class);
                startActivityForResult(intent2, REGISTER_USER);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void ShowUsers(View view) {
    Intent intent=new Intent(this,UsersListActivity.class);
    startActivity(intent);

    }

    private void registerUser(String name) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        db.collection("users").add(fields).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                // textview.setText(documentReference.getId());
                userId = documentReference.getId();
                SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                prefs.edit()
                        .putString("userId", userId)
                        .commit();
                enterRoom();
                Log.i("SpeakerFeedback", "New user: userId = " + userId);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("SpeakerFeedback", "Error creant objecte", e);
                Toast.makeText(MainActivity.this,
                        "No s'ha pogut registrar l'usuari, intenta-ho més tard", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    public void onPollClicked(int pos){

        Poll poll = polls.get(pos);
        if(!poll.isOpen()){
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(poll.getQuestion());
        String[] options = new String[poll.getOptions().size()];

        for(int i = 0; i < poll.getOptions().size(); i++){
            options[i] = poll.getOptions().get(i);
        }

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("pollid", polls.get(0).getId());
                map.put("option", which);
                db.collection("rooms").document("room_id").collection("votes").document(userId).set(map);

            }
        });

        builder.create().show();

    }
    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView labelview;
        private TextView questionview;
        private TextView optionsview;
        private CardView cardview;
        public ViewHolder(View itemView) {
            super(itemView);
            cardview = itemView.findViewById(R.id.card_view);
            labelview = itemView.findViewById(R.id.textview);
            questionview = itemView.findViewById(R.id.question);
            optionsview = itemView.findViewById(R.id.option);
            cardview.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    onPollClicked(pos);
                }
            });

        }
    }
    class Adapter extends RecyclerView.Adapter<ViewHolder>{
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.activity_polls, parent, false);
            return new ViewHolder(itemView);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Poll poll = polls.get(position);
            if (position == 0) {
                holder.labelview.setVisibility(View.VISIBLE);
                if (poll.isOpen()) {
                    holder.labelview.setText("Active");

                } else {
                    holder.labelview.setText("Previous");


                }
            } else {
                if (!poll.isOpen() && polls.get(position - 1).isOpen()) {
                    holder.labelview.setVisibility(View.VISIBLE);
                    holder.labelview.setText("Previous");
                } else {
                    holder.labelview.setVisibility(View.GONE);
                }
            }
            holder.cardview.setCardElevation(poll.isOpen() ? 10.0f : 0.0f);
            if (!poll.isOpen()) {
                holder.cardview.setCardBackgroundColor(0xFFE0E0E0);
            }
            holder.questionview.setText(poll.getQuestion());
            holder.optionsview.setText(poll.getOptionsAsString());
        }
        @Override
        public int getItemCount() {
            return polls.size();
        }
    }

}


