package com.plasma.scribe;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class ApiHandler {

    private static final String BASE_URL = "https://shifathasangns.pythonanywhere.com";
    private static final String TAG = "ScribeApiHandler";
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB per chunk

//    public interface ProcessFilesService {
//        @Headers("Content-Type: application/json")
//        @POST("/api/process-files")
//        Call<ResponseBody> processFiles(@Body RequestBody body);
//    }

    public interface RemoveFilesService {
        @Headers("Content-Type: application/json")
        @POST("/api/remove-files")
        Call<ResponseBody> removeFiles(@Body RequestBody body);
    }

    public interface UploadFileService {
        @Multipart
        @POST("/api/upload-files")
        Call<ResponseBody> uploadChunk(
                @Part MultipartBody.Part chunk,
                @Header("File-Name") String fileName,
                @Header("Part-Number") int partNumber,
                @Header("Is-First") boolean isFirst,
                @Header("Is-Last") boolean isLast
        );
    }

    public static File resolveFileFromUri(Context context, Uri uri) {
        String scheme = uri.getScheme();
        File file = null;
        try {
            if ("content".equalsIgnoreCase(scheme)) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            String fileName = cursor.getString(nameIndex);
                            File localFile = new File(context.getFilesDir(), fileName);
                            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                                 OutputStream outputStream = new java.io.FileOutputStream(localFile)) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                            file = localFile;
                        }
                    }
                }
            } else if ("file".equalsIgnoreCase(scheme)) {
                file = new File(Objects.requireNonNull(uri.getPath()));
            }
        } catch (Exception e) {
            Log.e("ApiHandler", "Error resolving file from URI: " + e.getMessage(), e);
        }

        return file;
    }

    public static void addFiles(Context context, List<Uri> fileUris, Runnable onComplete) {
        if (fileUris == null || fileUris.isEmpty()) {
            Log.e(TAG, "No files to upload");
            onComplete.run(); // Notify completion if no files to upload
            return;
        }

        Queue<Uri> uploadQueue = new LinkedList<>(fileUris); // Queue to handle uploads
        uploadNextFile(context, uploadQueue, onComplete);
    }

    private static void uploadNextFile(Context context, Queue<Uri> uploadQueue, Runnable onComplete) {
        if (uploadQueue.isEmpty()) {
            Log.d(TAG, "All files uploaded successfully.");
            onComplete.run();
            return;
        }

        Uri fileUri = uploadQueue.poll();
        if (fileUri == null) {
            Log.e(TAG, "File URI is null, skipping upload.");
            uploadNextFile(context, uploadQueue, onComplete);
            return;
        }

        Log.d(TAG, "Uploading file: " + fileUri);

        File file = resolveFileFromUri(context, fileUri);
        if (file == null || !file.exists()) {
            Log.e(TAG, "Resolved file does not exist: " + fileUri);
            uploadNextFile(context, uploadQueue, onComplete); // Continue to the next file
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        UploadFileService service = retrofit.create(UploadFileService.class);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int partNumber = 1;
                boolean isFirst = true;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    boolean isLast = fis.available() == 0;

                    byte[] chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                    RequestBody requestBody = RequestBody.create(chunkData, MediaType.parse("application/octet-stream"));
                    MultipartBody.Part chunk = MultipartBody.Part.createFormData("chunk", file.getName(), requestBody);

                    try {
                        retrofit2.Response<ResponseBody> response = service.uploadChunk(chunk, file.getName(), partNumber, isFirst, isLast).execute();

                        Log.d(TAG, "Response from server: " + response);

                        if (response.isSuccessful()) {
                            Log.i(TAG, "Chunk " + partNumber + " uploaded successfully for file: " + file.getName());
                        } else {
                            Log.e(TAG, "Failed to upload chunk " + partNumber + ": " + response.message());
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error uploading chunk " + partNumber, e);
                        break;
                    }

                    partNumber++;
                    isFirst = false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading file for upload", e);
            } finally {
                executor.shutdown();
                uploadNextFile(context, uploadQueue, onComplete);
            }
        });
    }

    public static void removeTheFiles(ArrayList<String> fileNames, Runnable onComplete) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient())
                .build();

        RemoveFilesService service = retrofit.create(RemoveFilesService.class);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                JSONArray filesArray = new JSONArray(fileNames);
                jsonBody.put("files", filesArray);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.parse("application/json")
                );

                service.removeFiles(body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                Log.i(TAG, "File removal successful: " + response.body().string());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Log.e(TAG, "File removal failed: " + response.message());
                        }
                        onComplete.run(); // Final step completion
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error during file removal API call", t);
                        onComplete.run(); // Final step completion
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating JSON body", e);
                onComplete.run();
            } finally {
                executor.shutdown();
            }
        });
    }

//    public static void processUploadedFiles(ArrayList<String> fileNames, Runnable onComplete) {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(new OkHttpClient())
//                .build();
//
//        ProcessFilesService service = retrofit.create(ProcessFilesService.class);
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//
//        executor.execute(() -> {
//            try {
//                JSONObject jsonBody = new JSONObject();
//                JSONArray filesArray = new JSONArray(fileNames);
//                jsonBody.put("files", filesArray);
//
//                RequestBody body = RequestBody.create(
//                        jsonBody.toString(),
//                        MediaType.parse("application/json")
//                );
//
//                service.processFiles(body).enqueue(new Callback<>() {
//                    @Override
//                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
//                        if (response.isSuccessful() && response.body() != null) {
//                            try {
//                                Log.i(TAG, "Processing API Response: " + response.body().string());
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        } else {
//                            Log.e(TAG, "Processing API failed: " + response.message());
//                        }
//                        onComplete.run(); // Proceed to next step regardless of success/failure
//                    }
//
//                    @Override
//                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
//                        Log.e(TAG, "Error during processing API call", t);
//                        onComplete.run(); // Proceed to next step in case of failure
//                    }
//                });
//
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error creating JSON body", e);
//                onComplete.run();
//            } finally {
//                executor.shutdown();
//            }
//        });
//    }
}
