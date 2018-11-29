package edu.upc.citm.android.speakerfeedback;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;
    private ListenerRegistration roomregistration, usersregistration, pollsregistration;
    private List<Poll> polls = new ArrayList<>();
    private TextView num_users;
    private RecyclerView polls_view;
    private Adapter adapter;
    private Button vote_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter=new Adapter();

        num_users=findViewById(R.id.num_usersview);
        polls_view=findViewById(R.id.pollsview);
        polls_view.setLayoutManager(new LinearLayoutManager(this));
        polls_view.setAdapter(adapter);

        vote_button=findViewById(R.id.votebutton);
        getOrRegisterUser();
        startFirestoreListenerService();
    }
    private void startFirestoreListenerService(){
        Intent intent = new Intent(this,FirestoreListenerService.class);
        intent.putExtra("room", "testroom");
        startService(intent);
    }
    private void stopFirestoreListenerService(){
        Intent intent= new Intent(this, FirestoreListenerService.class);
        stopService(intent);
    }
   private EventListener<DocumentSnapshot> roomListener=new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
            if(e!=null){
                Log.e("SpeakerFeedback","Error al rebre rooms/testroom",e);
                return;
            }
            String name=documentSnapshot.getString("name");
            setTitle(name);
        }
    };
    private EventListener<QuerySnapshot> usersListener=new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if(e!=null){
                Log.e("SpeakerFeedback","Error al rebre usuaris dins d'un room",e);
                return;
            }
            //textview.setText(String.format("Numuser: %d",documentSnapshots.size()));
            String nomsUsuaris="";
            for(DocumentSnapshot doc:documentSnapshots){
                nomsUsuaris+=doc.getString("name")+"\n";
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
        super.onStart();

        roomregistration=db.collection("rooms").document("testroom").addSnapshotListener(roomListener);
        usersregistration=db.collection("users").whereEqualTo("room","testroom").addSnapshotListener(usersListener);
        pollsregistration=db.collection("rooms").document("testroom").collection("polls").orderBy("start", Query.Direction.DESCENDING).addSnapshotListener(pollListener);
    }
    protected void onStop(){
        super.onStop();
        roomregistration.remove();
        usersregistration.remove();
        pollsregistration.remove();
    }

    @Override
    protected void onDestroy() {
        exitRoom();
        super.onDestroy();
    }

    private void getOrRegisterUser() {
        // Busquem a les preferències de l'app l'ID de l'usuari per saber si ja s'havia registrat
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
            enterRoom();
        }
    }

    private void enterRoom() {
        db.collection("users").document(userId).update("room", "testroom");
    }

    private void exitRoom() {
        db.collection("users").document(userId).update("room", FieldValue.delete());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_USER:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    registerUser(name);
                } else {
                    Toast.makeText(this, "Has de registrar un nom", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
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
                    vote_button.setTextColor(0xFF00AA00);
                    vote_button.setClickable(true);
                } else {
                    holder.labelview.setText("Previous");
                    vote_button.setTextColor(0xFFAA0000);
                    vote_button.setClickable(false);

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
    public void onClickVoteButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String[] options = new String[polls.get(0).getOptions().size()];
        int i = 0;
        for (String string : polls.get(0).getOptions()) {
            options[i] = string;
            ++i;
        }
        builder.setTitle(polls.get(0).getQuestion()).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, Object> map = new HashMap<>();
                map.put("poll id", polls.get(0).getHash_question());
                map.put("option", which);
                //TODO: revise this
                db.collection("rooms").document("testroom").collection("votes").document(userId).set(map);

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

}


