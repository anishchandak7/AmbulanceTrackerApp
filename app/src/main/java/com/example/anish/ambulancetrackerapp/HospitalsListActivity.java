package com.example.anish.ambulancetrackerapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import javax.annotation.Nullable;

public class HospitalsListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private CollectionReference regionReference=FirebaseFirestore.getInstance().collection("Regions");
    private ListView hospitals_list;
    String recieved_region;
    String []hospitals_array;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospitals_list);
        hospitals_list = (ListView) findViewById(R.id.hospital_list);
        Intent intent = getIntent();
        Log.d("REGION NAME RECIEVED :",intent.getStringExtra("region_name"));
        recieved_region = intent.getStringExtra("region_name");
        regionReference.document(recieved_region).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot documentSnapshot = task.getResult();
                List<String> hospitals = (List<String>) documentSnapshot.get("Hospitals");
                Log.d("HOSPITALS SIZE:", String.valueOf(hospitals.size()));
                hospitals_array = new String[hospitals.size()];
                for(int i=0;i<hospitals.size();i++)
                {
                    hospitals_array[i] = hospitals.get(i);
                }
                ArrayAdapter adapter = new ArrayAdapter<String>(HospitalsListActivity.this, android.R.layout.simple_list_item_1,hospitals_array);
                hospitals_list.setAdapter(adapter);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

        hospitals_list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent mainActivity = new Intent(this,MainActivity.class);
        mainActivity.putExtra("hospital_name",hospitals_array[position]);
        startActivity(mainActivity);
    }
}
