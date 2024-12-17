package com.plasma.scribe;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

public class LibraryRecyclerViewAdapter extends RecyclerView.Adapter<LibraryRecyclerViewAdapter.ViewHolder> {

    Context context;
    HashMap<String, String> documents;

    public LibraryRecyclerViewAdapter(Context context, HashMap<String, String> documents) {
        super();

        this.context = context;
        this.documents = documents;
    }

    @NonNull
    @Override
    public LibraryRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.documentTitle.setText(documents.keySet().toArray()[position].toString());
        holder.documentPreview.setText(documents.values().toArray()[position].toString());

        holder.documentPreview.setOnClickListener(v -> {
            Intent intent = new Intent(context, DocumentPreviewActivity.class);
            intent.putExtra("title", holder.documentTitle.getText());
            intent.putExtra("document", holder.documentPreview.getText());
            context.startActivity(intent);
        });

        holder.buttonRemove.setOnClickListener(v -> {
            documents.remove(holder.documentTitle.getText());
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, documents.size());
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView documentTitle;
        TextView documentPreview;
        ImageButton buttonRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            documentTitle = itemView.findViewById(R.id.card_document_title);
            documentPreview = itemView.findViewById(R.id.card_document_preview);
            buttonRemove = itemView.findViewById(R.id.button_remove_document);
        }
    }
}
