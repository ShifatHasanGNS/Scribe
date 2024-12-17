package com.plasma.scribe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

public class LibraryFragment extends Fragment {
    public LibraryFragment() {
    }

    private static final String TAG = "ScribeLibraryActivity";

    AppCompatActivity activity;

    private DatabaseHandler dbHandler;
    private HashMap<String, String> documents = new HashMap<>();
    private RecyclerView recyclerView;
    private LibraryRecyclerViewAdapter adapter;

    @Override
    public void onStart() {
        super.onStart();
        activity = (AppCompatActivity) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        dbHandler = new DatabaseHandler(activity);

        dbHandler.getDocuments(new DatabaseHandler.OnDataRetrievedListener<>() {
            public void onSuccess(HashMap<String, String> docs) {
                documents = docs;
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DatabaseHandler", "Failed to retrieve user name", e);
            }
        });

        recyclerView = view.findViewById(R.id.library_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new LibraryRecyclerViewAdapter(activity, documents);
        recyclerView.setAdapter(adapter);

        return view;
    }
}