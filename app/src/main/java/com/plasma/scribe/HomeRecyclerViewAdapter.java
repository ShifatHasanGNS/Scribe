package com.plasma.scribe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HomeRecyclerViewAdapter extends RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder> {

    Context context;
    ArrayList<String> fileNames;

    public HomeRecyclerViewAdapter(Context context, ArrayList<String> fileNames) {
        super();
        this.context = context;
        this.fileNames = fileNames;
    }

    @NonNull
    @Override
    public HomeRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.fileName.setText(fileNames.get(position));

        holder.buttonRemove.setOnClickListener(v -> {
            fileNames.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, fileNames.size());
        });
    }

    @Override
    public int getItemCount() {
        return fileNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView fileName;
        ImageButton buttonRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.text_file_name);
            buttonRemove = itemView.findViewById(R.id.button_remove);
        }
    }
}
