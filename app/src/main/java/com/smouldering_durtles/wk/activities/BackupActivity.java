package com.smouldering_durtles.wk.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smouldering_durtles.wk.GlobalSettings;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class BackupActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int SAVE_FILE_REQUEST_CODE = 2;
    private static final String TAG = "BackupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Depending on the intent action, start either backup or restore.
        String action = getIntent().getAction();
        if ("com.smouldering_durtles.wk.BACKUP".equals(action)) {
            startBackup();
        } else if ("com.smouldering_durtles.wk.RESTORE".equals(action)) {
            startRestore();
        } else {
            Log.e(TAG, "Unknown action: " + action);
            finish();
        }
    }

    private void startBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "settings_backup.json");
        startActivityForResult(intent, SAVE_FILE_REQUEST_CODE);
    }

    private void startRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Gson gson = new Gson();

            if (requestCode == SAVE_FILE_REQUEST_CODE) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    Map<String, ?> allEntries = GlobalSettings.getAllSettings();
                    Map<String, Map<String, String>> allEntriesWithTypes = new HashMap<>();

                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        Map<String, String> item = new HashMap<>();
                        item.put("value", entry.getValue().toString());
                        item.put("type", entry.getValue().getClass().getSimpleName());
                        allEntriesWithTypes.put(entry.getKey(), item);
                    }

                    // Convert the map into JSON
                    String jsonSettings = gson.toJson(allEntriesWithTypes);

                    // Write JSON to the file
                    writer.write(jsonSettings);

                    writer.close();
                    outputStream.close();

                    Toast.makeText(this, "Settings backed up successfully.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == PICK_FILE_REQUEST_CODE) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    String jsonString = stringBuilder.toString();

                    Type type = new TypeToken<HashMap<String, Map<String, String>>>(){}.getType();
                    Map<String, Map<String, String>> settingsWithTypes = gson.fromJson(jsonString, type);

                    Map<String, Object> settings = new HashMap<>();
                    for (Map.Entry<String, Map<String, String>> entry : settingsWithTypes.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue().get("value");
                        String valueType = entry.getValue().get("type");

                        Object objectValue;
                        switch (valueType) {
                            case "Integer":
                                objectValue = Integer.valueOf(value);
                                break;
                            case "Boolean":
                                objectValue = Boolean.valueOf(value);
                                break;
                            case "Float":
                                objectValue = Float.valueOf(value);
                                break;
                            case "Long":
                                objectValue = Long.valueOf(value);
                                break;
                            case "String":
                                objectValue = value;
                                break;
                            default:
                                throw new UnsupportedOperationException("Unsupported type " + valueType);
                        }

                        settings.put(key, objectValue);
                    }

                    GlobalSettings.setAllSettings(settings); // Set all settings at once.
                    reader.close();
                    inputStream.close();
                    Toast.makeText(this, "Settings restored successfully.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
