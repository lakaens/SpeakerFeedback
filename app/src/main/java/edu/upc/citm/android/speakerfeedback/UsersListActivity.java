package edu.upc.citm.android.speakerfeedback;

import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends AppCompatActivity {

    private Adapter adapter;
    private RecyclerView recyclerView;
    private List<String> users;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    ListenerRegistration listenerRegistration;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        adapter = new Adapter();
        users=new ArrayList<>();

        recyclerView=findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    @Override
    protected void onStart(){
        super.onStart();
        listenerRegistration = db.collection("users").whereEqualTo("room","testroom").addSnapshotListener(usersListener);
    }
    @Override
    protected void onStop(){
        super.onStop();
        listenerRegistration.remove();
    }



    class ViewHolder extends RecyclerView.ViewHolder{
        TextView users_view;
        public ViewHolder(View itemView){
            super(itemView);
            this.users_view=itemView.findViewById(R.id.user_view);
        }
    }
    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al ensenyar els usuaris", e);
                return;
            }
            users.clear();
            for (DocumentSnapshot doc : documentSnapshots) {
                users.add(doc.getString("name"));
            }

            adapter.notifyDataSetChanged();

        }
    };

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.userview, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.users_view.setText(users.get(position));

        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}
