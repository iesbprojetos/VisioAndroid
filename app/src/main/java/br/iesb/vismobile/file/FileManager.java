package br.iesb.vismobile.file;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by dfcarvalho on 4/3/16.
 */
public class FileManager {
    public static final String ARG_RESULT_CODE = "RESULT_CODE";
    public static final String ARG_BACKUP_URI = "BACKUP_URI";
    public static final int RESULT_OK = 0;
    public static final int RESULT_STORAGE_NOT_READY = 1;
    public static final int RESULT_FAILED_TO_CREATE_DIRECTORY = 2;
    public static final int RESULT_FAILED_TO_CREATE_FILE = 3;
    public static final int RESULT_FAILED_WRITE = 4;
    public static final int RESULT_WAITING_USER = 5;

    private static FileManager SINGLETON;

    public static synchronized FileManager getSingleton(Context context) {
        if (SINGLETON == null) {
            SINGLETON = new FileManager(context.getApplicationContext());
        }

        return SINGLETON;
    }

    private Context context;
    private ChartCollection collection;
    private boolean miliVolts;

    private FileManager(Context context) {
        this.context = context;
        collection = new ChartCollection("solucao1");
        miliVolts = false;
    }

    public ChartCollection getCollection() {
        return collection;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    public boolean askForStoragePermission(Activity activity, int requestCode, String permission) {
        // check permission
        if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {
            // check if needs to explain
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    permission)) {
                // TODO: show explanation
            } else {
                // ask permission
                ActivityCompat.requestPermissions(activity,
                        new String[] {permission},
                        requestCode);
            }

            return false;
        }

        return true;
    }

    public File getDefaultFolder() {
        File visioFolder = new File(Environment.getExternalStorageDirectory() + "/Visio");
        if (!visioFolder.exists() && !visioFolder.mkdirs()) {
            return null;
        }

        return visioFolder;
    }

    public String getDefaultFilename() {
        String defaultName = null;

        File folder = getDefaultFolder();

        if (folder != null) {
            String[] filenames = folder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.startsWith("solucao");
                }
            });

            int maior = 0;
            for (String f : filenames) {
                String strNum = f.replace("solucao", "");

                try {
                    int num = Integer.parseInt(strNum);
                    if (num > maior) {
                        maior = num;
                    }
                } catch (NumberFormatException e) {
                    // ignora
                }
            }

            defaultName = String.format("solucao%d", ++maior);
        }

        return defaultName;
    }

    public Bundle createFile(String filename, boolean overwrite) {
        Bundle result = new Bundle();
        // check if storage is available
        if (!isExternalStorageWritable()) {
            result.putInt(ARG_RESULT_CODE, RESULT_STORAGE_NOT_READY);
            return result;
        }

        // create folder, if needed
        File jurisFolder = getDefaultFolder();
        if (jurisFolder == null) {
            result.putInt(ARG_RESULT_CODE, RESULT_FAILED_TO_CREATE_DIRECTORY);
            return result;
        }

        // create file
        collection.setName(filename);
        final File backupFile = new File(jurisFolder, filename);

        BufferedOutputStream out = null;
        try {
            if (!backupFile.exists()) {
                if (!backupFile.createNewFile()) {
                    result.putInt(ARG_RESULT_CODE, RESULT_FAILED_TO_CREATE_FILE);
                    return result;
                }
            } else {
                if (!overwrite) {
                    result.putInt(ARG_RESULT_CODE, RESULT_WAITING_USER);
                    return result;
                }
            }

            out = new BufferedOutputStream(new FileOutputStream(backupFile));

            Gson gson = new Gson();
            String strCollection = gson.toJson(collection);

            out.write(strCollection.getBytes());
            out.flush();
            out.close();

            result.putInt(ARG_RESULT_CODE, RESULT_OK);
            Uri backupUri = Uri.fromFile(backupFile);
            result.putParcelable(ARG_BACKUP_URI, backupUri);
            return result;
        } catch (IOException e) {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            result.putInt(ARG_RESULT_CODE, RESULT_FAILED_WRITE);
        }

        return result;
    }

    // RESTORE
    public boolean readFile(Uri fileUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            StringBuilder strBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                strBuilder.append(line);
            }

            inputStream.close();
            reader.close();

            Gson gson = new Gson();
            collection = gson.fromJson(strBuilder.toString(), ChartCollection.class);

            return true;
        } catch (FileNotFoundException e) {
            Log.d("Juris", "File not found: " + fileUri);
        } catch (IOException e) {
            Log.d("Juris", "Failed to read file: " + fileUri);
        }

        return false;
    }

    public boolean isMiliVolts() {
        return miliVolts;
    }

    public void setMiliVolts(boolean miliVolts) {
        this.miliVolts = miliVolts;
    }

    //    private class GsonProcesso implements JsonDeserializer<SFJProcess>, JsonSerializer<SFJProcess> {
//        @Override
//        public SFJProcess deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
//            JsonObject jsonProc = jsonElement.getAsJsonObject();
//
//            SFJProcess proc = new SFJProcess();
//
//            proc.setJusId(jsonProc.get("jusId").getAsLong());
//            proc.setCourtID(jsonProc.get("courtId").getAsLong());
//            proc.setInstance(jsonProc.get("instance").getAsInt());
//            proc.setNumber(jsonProc.get("number").getAsString());
//            JsonElement elementTitle;
//            if ((elementTitle = jsonProc.get("title")) != null) {
//                proc.setTitle(elementTitle.getAsString());
//            }
//            JsonElement elementNotes;
//            if ((elementNotes = jsonProc.get("notes")) != null) {
//                proc.setNotes(elementNotes.getAsString());
//            }
//
//            return proc;
//        }
//
//        @Override
//        public JsonElement serialize(SFJProcess proc, Type type, JsonSerializationContext context) {
//            JsonObject jsonProc = new JsonObject();
//
//            jsonProc.addProperty("jusId", proc.getJusId());
//            jsonProc.addProperty("courtId", proc.getCourtID());
//            jsonProc.addProperty("instance", proc.getInstance());
//            jsonProc.addProperty("number", proc.getNumber());
//            String title = proc.getTitle();
//            if (title != null) {
//                jsonProc.addProperty("title", title);
//            }
//            String notes = proc.getNotes();
//            if (notes != null) {
//                jsonProc.addProperty("notes", notes);
//            }
//
//            return jsonProc;
//        }
//    }
}
