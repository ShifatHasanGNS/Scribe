package com.plasma.scribe;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment {
    public HomeFragment() {
    }

    static final String TAG = "ScribeHomeActivity";

    AppCompatActivity activity;

    Uri fileUri;
    final ArrayList<Uri> fileUris = new ArrayList<>();
    final ArrayList<String> fileNames = new ArrayList<>();
    final HashMap<String, Uri> fileUriMap = new HashMap<>();

    FloatingActionButton buttonAdd;
    FloatingActionButton buttonScan;
    FloatingActionButton buttonFileImport;

    Animation rotateOpen;
    Animation rotateClose;
    Animation fromBottom;
    Animation toBottom;

    boolean clicked;
    ImageButton buttonProcess;

    RecyclerView recyclerView;
    HomeRecyclerViewAdapter adapter;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        buttonAdd = view.findViewById(R.id.button_add);
        buttonScan = view.findViewById(R.id.button_scan);
        buttonFileImport = view.findViewById(R.id.button_file_import);

        rotateOpen = AnimationUtils.loadAnimation(activity, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(activity, R.anim.rotate_close_anim);
        fromBottom = AnimationUtils.loadAnimation(activity, R.anim.from_bottom_anim);
        toBottom = AnimationUtils.loadAnimation(activity, R.anim.to_bottom_anim);

        clicked = false;
        buttonProcess = view.findViewById(R.id.button_process);

        recyclerView = view.findViewById(R.id.home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new HomeRecyclerViewAdapter(activity, fileNames);
        recyclerView.setAdapter(adapter);

        buttonAdd.setOnClickListener(v -> onButtonAddClicked());
        buttonScan.setOnClickListener(v -> scanDocumentWithMLKit());
        buttonFileImport.setOnClickListener(v -> openFilePicker());

//        buttonProcess.setOnClickListener(v -> sendFilesToAPI());

        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    Log.d(TAG, "File picker was cancelled or returned no data");
                    return;
                }
                try {
                    String fileName;
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri fileUri = result.getData().getClipData().getItemAt(i).getUri();
                            if (!fileUris.contains(fileUri)) {
                                fileUris.add(fileUri);
                                fileName = getFileName(fileUri);
                                Log.d(TAG, "Filename: " + fileName);
                                Log.d(TAG, "Uri: " + fileUri);
                                fileNames.add(fileName);
                                fileUriMap.put(getFileName(fileUri), fileUri);
                            }
                        }
                    } else if (result.getData().getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (!fileUris.contains(fileUri)) {
                            fileUris.add(fileUri);
                            fileName = getFileName(fileUri);
                            Log.d(TAG, "Filename: " + fileName);
                            Log.d(TAG, "Uri: " + fileUri);
                            fileNames.add(fileName);
                            fileUriMap.put(fileName, fileUri);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing selected files", e);
                    Toast.makeText(activity, "Error selecting files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), activityResult -> {
                int resultCode = activityResult.getResultCode();
                if (resultCode == RESULT_OK) {
                    GmsDocumentScanningResult scannedResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
                    if (scannedResult != null) {
                        handleScannedResult(scannedResult); // Process the scanned result
                    }
                } else {
                    Toast.makeText(activity, "Scanner Cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void scanDocumentWithMLKit() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setPageLimit(10)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

        scanner.getStartScanIntent(activity)
                .addOnSuccessListener(intentSender -> {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    scannerLauncher.launch(request);
                })
                .addOnFailureListener(e -> Toast.makeText(activity, "Failed to start document scanner: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleScannedResult(GmsDocumentScanningResult scannedResult) {
        GmsDocumentScanningResult.Pdf pdf = scannedResult.getPdf();
        if (pdf != null) {
            fileUri = pdf.getUri();
            int pageCount = pdf.getPageCount();
            Toast.makeText(activity, "PDF with " + pageCount + " pages created: " + fileUri, Toast.LENGTH_LONG).show();

            fileUris.add(fileUri);

            View dialogView = getLayoutInflater().inflate(R.layout.scanned_doc_name_dialog, null);

            EditText docName = dialogView.findViewById(R.id.doc_name);
            ImageButton buttonDocName = dialogView.findViewById(R.id.button_doc_name);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomDialogTheme);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            dialog.show();

            buttonDocName.setOnClickListener(u -> {
                String docNameStr = docName.getText().toString().trim();
                if (docNameStr.isEmpty()) {
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
                    String formattedDateTime = now.format(formatter);
                    docNameStr = "ScannedDocument_" + formattedDateTime + ".pdf";
                }
                fileNames.add(docNameStr);
                adapter.notifyDataSetChanged();
                fileUriMap.put(docNameStr, fileUri);
                dialog.dismiss();
            });
        }
    }

    private void onButtonAddClicked() {
        clicked = !clicked;
        setVisibility();
        setAnimation();
        setClickable();
    }

    private void setVisibility() {
        if (clicked) {
            buttonScan.setVisibility(View.VISIBLE);
            buttonFileImport.setVisibility(View.VISIBLE);
        } else {
            buttonScan.setVisibility(View.INVISIBLE);
            buttonFileImport.setVisibility(View.INVISIBLE);
        }
    }

    private void setAnimation() {
        if (clicked) {
            buttonScan.startAnimation(fromBottom);
            buttonFileImport.startAnimation(fromBottom);
            buttonAdd.startAnimation(rotateOpen);
        } else {
            buttonScan.startAnimation(toBottom);
            buttonFileImport.startAnimation(toBottom);
            buttonAdd.startAnimation(rotateClose);
        }
    }

    private void setClickable() {
        if (clicked) {
            buttonScan.setClickable(true);
            buttonFileImport.setClickable(true);
        } else {
            buttonScan.setClickable(false);
            buttonFileImport.setClickable(false);
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown File";

        if (uri.getScheme() != null) {
            if (uri.getScheme().equals("content")) {
                // Use ContentResolver to fetch metadata for content URIs
                try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving file name for content URI", e);
                }
            } else if (uri.getScheme().equals("file")) {
                // Extract file name from the file path
                String path = uri.getPath();
                if (path != null) {
                    int lastSlashIndex = path.lastIndexOf('/');
                    if (lastSlashIndex != -1) {
                        fileName = path.substring(lastSlashIndex + 1);
                    }
                }
            }
        }

        return fileName;
    }

//    private void sendFilesToAPI() {
//        if (!fileUris.isEmpty()) {
//            List<Uri> fileUrisList = new ArrayList<>();
//
//            LinearLayout container = findViewById(R.id.container);
//
//            for (int i = 0; i < container.getChildCount(); i++) {
//                View child = container.getChildAt(i);
//
//                if (child instanceof LinearLayout cardView) {
//                    CheckBox checkBox = cardView.findViewById(R.id.checkbox_select);
//                    if (checkBox.isChecked()) {
//                        TextView fileNameTextView = cardView.findViewById(R.id.text_file_name);
//                        String fileName = fileNameTextView.getText().toString();
//                        Uri fileUri = fileUriMap.get(fileName);
//                        if (fileUri != null) {
//                            fileUrisList.add(fileUri);
//                        }
//                    }
//                }
//            }
//
//            ApiHandler.addFiles(HomeActivity.this, fileUrisList);
//        } else {
//            Toast.makeText(HomeActivity.this, "No files selected", Toast.LENGTH_SHORT).show();
//        }
//    }
}