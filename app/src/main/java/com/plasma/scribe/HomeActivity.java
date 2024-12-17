package com.plasma.scribe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "ScribeHomeActivity";

    private Uri fileUri;
    private ArrayList<Uri> fileUris = new ArrayList<>();
    private ArrayList<String> fileNames = new ArrayList<>();
    private HashMap<String, Uri> fileUriMap = new HashMap<>();

    private ImageButton navButtonHome;
    private ImageButton navButtonLibrary;
    private ImageButton navButtonProfile;

    private FloatingActionButton buttonAdd;
    private FloatingActionButton buttonScan;
    private FloatingActionButton buttonFileImport;

    private Animation rotateOpen;
    private Animation rotateClose;
    private Animation fromBottom;
    private Animation toBottom;

    private boolean clicked;
    private ImageButton buttonProcess;

    private RecyclerView recyclerView;
    private HomeRecyclerViewAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance("https://scribe-v01-default-rtdb.firebaseio.com/");

        navButtonHome = findViewById(R.id.button_home);
        navButtonLibrary = findViewById(R.id.button_library);
        navButtonProfile = findViewById(R.id.button_profile);

        buttonAdd = findViewById(R.id.button_add);
        buttonScan = findViewById(R.id.button_scan);
        buttonFileImport = findViewById(R.id.button_file_import);

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim);
        toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);

        clicked = false;
        buttonProcess = findViewById(R.id.button_process);

        recyclerView = findViewById(R.id.home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this));
        adapter = new HomeRecyclerViewAdapter(HomeActivity.this, fileNames);
        recyclerView.setAdapter(adapter);

        navButtonHome.setClickable(false);
        navButtonLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LibraryActivity.class);
            intent.putExtra("fileUriMap", fileUriMap);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navButtonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            intent.putExtra("fileUriMap", fileUriMap);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        buttonAdd.setOnClickListener(v -> onButtonAddClicked());
        buttonScan.setOnClickListener(v -> scanDocumentWithMLKit());
        buttonFileImport.setOnClickListener(v -> openFilePicker());

        buttonProcess.setOnClickListener(v -> processFiles());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("fileUriMap", fileUriMap);
        outState.putSerializable("fileUris", fileUris);
        outState.putSerializable("fileNames", fileNames);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileUriMap = (HashMap<String, Uri>) savedInstanceState.getSerializable("fileUriMap");
        fileUris = (ArrayList<Uri>) savedInstanceState.getSerializable("fileUris");
        fileNames = (ArrayList<String>) savedInstanceState.getSerializable("fileNames");
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
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri fileUri = result.getData().getClipData().getItemAt(i).getUri();
                            if (!fileUris.contains(fileUri)) {
                                fileUris.add(fileUri);
                                String fileName = getFileName(fileUri);
                                fileNames.add(fileName);
                                fileUriMap.put(getFileName(fileUri), fileUri);
                            }
                        }
                    } else if (result.getData().getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (!fileUris.contains(fileUri)) {
                            fileUris.add(fileUri);
                            String fileName = getFileName(fileUri);
                            fileNames.add(fileName);
                            fileUriMap.put(fileName, fileUri);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing selected files", e);
                    Toast.makeText(this, "Error selecting files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), activityResult -> {
                int resultCode = activityResult.getResultCode();
                if (resultCode == RESULT_OK) {
                    GmsDocumentScanningResult scannedResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
                    if (scannedResult != null) {
                        handleScannedResult(scannedResult);
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Scanner Cancelled", Toast.LENGTH_SHORT).show();
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
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

        scanner.getStartScanIntent(this)
                .addOnSuccessListener(intentSender -> {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    scannerLauncher.launch(request);
                })
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "Failed to start document scanner: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleScannedResult(GmsDocumentScanningResult scannedResult) {
        GmsDocumentScanningResult.Pdf pdf = scannedResult.getPdf();
        if (pdf != null) {
            fileUri = pdf.getUri();
            int pageCount = pdf.getPageCount();
            Toast.makeText(HomeActivity.this, "PDF with " + pageCount + " pages created: " + fileUri, Toast.LENGTH_LONG).show();

            fileUris.add(fileUri);

            View dialogView = getLayoutInflater().inflate(R.layout.scanned_doc_name_dialog, null);

            EditText docName = dialogView.findViewById(R.id.doc_name);
            ImageButton buttonDocName = dialogView.findViewById(R.id.button_doc_name);

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this, R.style.CustomDialogTheme);
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
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
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

    String text = "";

    @SuppressLint("NotifyDataSetChanged")
    private void processFiles() {
        text = "";
        int c = 0;

        if (!fileUris.isEmpty()) {
//            ApiHandler.addFiles(HomeActivity.this, fileUris, () -> {
//                Toast.makeText(HomeActivity.this, "All files uploaded successfully.", Toast.LENGTH_SHORT).show();

            ArrayList<Bitmap> images = new ArrayList<>();

            for (Uri uri : fileUris) {
                File file = ApiHandler.resolveFileFromUri(this, uri);
                images.addAll(getBitmapsFromPDF(file));
//                fileUris.remove(uri);
//                fileNames.remove(getFileName(uri));
//                fileUriMap.remove(getFileName(uri));
//                adapter.notifyDataSetChanged();
            }

            for (Bitmap image : images) {
                recognizeTextFromImage(image, task -> {
                    if (task.isSuccessful()) {
                        Text result = task.getResult();
                        String recognizedText = result.getText();
                        text += "[" + c + "]\n" + recognizedText + "\n\n";

                        Log.d(TAG, "Recognized Text: " + text);
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Error recognizing text", e);
                    }
                });
            }

//            dbRef.child("documents").child("title").setValue(text);

//                ApiHandler.removeTheFiles(fileNames, () -> Toast.makeText(HomeActivity.this, "File removal completed. All operations successful.", Toast.LENGTH_SHORT).show());
//            });

        } else {
            Toast.makeText(HomeActivity.this, "Scan or Import some files.", Toast.LENGTH_SHORT).show();
        }
    }

    public void recognizeTextFromImage(Bitmap image, OnCompleteListener<Text> onCompleteListener) {
        InputImage inputImage = InputImage.fromBitmap(image, 0);
        TextRecognizerOptions options = new TextRecognizerOptions.Builder().build();
        TextRecognizer recognizer = TextRecognition.getClient(options);
        recognizer.process(inputImage).addOnCompleteListener(onCompleteListener);
    }

    public ArrayList<Bitmap> getBitmapsFromPDF(File pdfFile) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();

        try (ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
            PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            int pageCount = pdfRenderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        displayMetrics.widthPixels, displayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                bitmaps.add(bitmap);

                page.close();
            }

            pdfRenderer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmaps from PDF", e);
        }

        return bitmaps;
    }
}