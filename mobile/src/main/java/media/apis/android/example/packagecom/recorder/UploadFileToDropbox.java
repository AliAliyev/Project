package media.apis.android.example.packagecom.recorder;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Ali on 01/03/2017.
 */

public class UploadFileToDropbox extends AsyncTask<Void, Void, Boolean> {

    private final static String APP_KEY = "m6l3tdkthlvgxdz";
    private final static String APP_SECRET = "9xbaiwgqlh4nbeo";

    private DropboxAPI<?> dropbox;
    private File file;
    private Context context;

    public UploadFileToDropbox(Context context, DropboxAPI<?> dropbox, File file) {
        this.context = context;
        this.file = file;
        this.dropbox = dropbox;
    }

    @Override
    protected void onPreExecute(){
//        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
//        AndroidAuthSession session = new AndroidAuthSession(appKeys);
//        dropbox = new DropboxAPI<>(session);
//        session.startOAuth2Authentication(context);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            dropbox.putFile("/Recorder/" + file.getName(), fileInputStream, file.length(), null, null);
            return true;
        } catch (IOException | DropboxException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "File saved to Dropbox", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to upload file", Toast.LENGTH_LONG).show();
        }
    }

}