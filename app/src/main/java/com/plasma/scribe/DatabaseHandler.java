package com.plasma.scribe;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DatabaseHandler {
    private static final String TAG = "ScribeDatabaseHandler";

    private final FirebaseAuth auth;
    private final DatabaseReference dbRef;
    private final Context context;

    public DatabaseHandler(Context context) {
        this.auth = FirebaseAuth.getInstance();
        this.context = context;

        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User is not authenticated!");
        }

        this.dbRef = FirebaseDatabase.getInstance("https://scribe-v01-default-rtdb.firebaseio.com/")
                .getReference(auth.getCurrentUser().getUid());
    }

    public void getName(OnDataRetrievedListener<String> listener) {
        dbRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess(snapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void getEmail(OnDataRetrievedListener<String> listener) {
        dbRef.child("email").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess(snapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void getBio(OnDataRetrievedListener<String> listener) {
        dbRef.child("bio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess(snapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void getAppRating(OnDataRetrievedListener<String> listener) {
        dbRef.child("app_rating").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess(snapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void getFeedbacks(OnDataRetrievedListener<ArrayList<String>> listener) {
        dbRef.child("documents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> feedbacks = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    feedbacks.add(doc.getValue(String.class));
                }
                listener.onSuccess(feedbacks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void getDocuments(OnDataRetrievedListener<HashMap<String, String>> listener) {
        dbRef.child("documents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> documents = new HashMap<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    documents.put(doc.getKey(), Objects.toString(doc.getValue(), ""));
                }
                listener.onSuccess(documents);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
                listener.onFailure(error.toException());
            }
        });
    }

    public void updateName(String name) {
        updateData("name", name);
    }

    public void updateBio(String bio) {
        updateData("bio", bio);
    }

    public void updateAppRating(String appRating) {
        updateData("app_rating", appRating);
    }

    public void addFeedback(String feedback) {
        dbRef.child("feedbacks").push().setValue(feedback)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to add feedback.");
                    }
                });
    }

    public void addDocument(String title, String documentMarkdown) {
        dbRef.child("documents").child(title).setValue(documentMarkdown)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to add document.");
                    }
                });
    }

    public void removeDocument(String title) {
        dbRef.child("documents").child(title).removeValue()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Failed to remove document.");
                    }
                });
    }

    private void updateData(String key, String value) {
        dbRef.child(key).setValue(value).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showToast("Failed to update data for: " + key);
            }
        });
    }

    private void handleDatabaseError(DatabaseError error) {
        Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
        showToast("An error occurred. Please try again.");
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public interface OnDataRetrievedListener<T> {
        void onSuccess(T data);

        void onFailure(Exception e);
    }
}
