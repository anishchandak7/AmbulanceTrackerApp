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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegionActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView regionlist;
    private CollectionReference regionReference = FirebaseFirestore.getInstance().collection("Regions");
    String []regions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_region);

        regionlist = (ListView) findViewById(R.id.region_listview);

        Task<QuerySnapshot> query = regionReference.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(!queryDocumentSnapshots.isEmpty())
                {
                    regions = new String[queryDocumentSnapshots.size()];
                    for (int i = 0;i<queryDocumentSnapshots.getDocuments().size();i++)
                    {
                        Log.d("REGION NAME : ",queryDocumentSnapshots.getDocuments().get(i).getId());
                        regions[i] = queryDocumentSnapshots.getDocuments().get(i).getId();
                    }

                    ArrayAdapter adapter = new ArrayAdapter<String>(RegionActivity.this, android.R.layout.simple_list_item_1,regions);
                    regionlist.setAdapter(adapter);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ERROR :","COULDN'T FETCH THE DETAILS");
            }
        });

        regionlist.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent hospitalActivity = new Intent(this,HospitalsListActivity.class);
        hospitalActivity.putExtra("region_name",regions[position]);
        startActivity(hospitalActivity);
    }
}
