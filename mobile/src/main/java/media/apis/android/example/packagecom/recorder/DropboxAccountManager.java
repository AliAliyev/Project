package media.apis.android.example.packagecom.recorder;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Ali on 01/03/2017.
 */

public class DropboxAccountManager {
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String APP_KEY = "m6l3tdkthlvgxdz";
    private final static String APP_SECRET = "9xbaiwgqlh4nbeo";

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_NAME, 0);
        return prefs.getString(APP_KEY, null) != null && prefs.getString(APP_SECRET, null) != null;
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(APP_KEY, null);
        editor.putString(APP_SECRET, null);
        editor.apply();
    }
}
