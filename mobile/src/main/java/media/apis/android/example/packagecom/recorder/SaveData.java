package media.apis.android.example.packagecom.recorder;

import java.util.ArrayList;

/**
 * Created by Ali on 02/02/2017.
 */

public class SaveData {
    private ArrayList<TrackItem> listTracks;

    public SaveData(final ArrayList<TrackItem> listTracks) {
        this.listTracks = listTracks;
    }

    public ArrayList<TrackItem> getListTracks() { return listTracks; }
}
