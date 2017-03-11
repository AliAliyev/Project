package media.apis.android.example.packagecom.recorder;

import android.os.Environment;
import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Ali on 31/01/2017.
 */

public class DataManager {
    private static final String LOG_TAG = "DataManager";
    //private ArrayList<File> files, folders;
    private File saveFile;
    private ArrayList<TrackItem> listTracks, tracksList;

    public DataManager() {

        //tracksList = new ArrayList<>();
        listTracks = new ArrayList<>();

        String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        savePath += "/Recorder";
        new File(savePath).mkdirs();

        savePath += "/data";
        new File(savePath).mkdirs();

        savePath += "/data.json";
        saveFile = new File(savePath);

        if(saveFile.exists()) {
            load();
        }
        else {
            try { saveFile.createNewFile(); }
            catch (IOException e) {
                Log.e(LOG_TAG, "createNewFile() failed");
                e.printStackTrace();
            }
        }
        save();
    }

    private void save() {
        try {
            FileWriter file = new FileWriter(saveFile, false);
            SaveData toSave = new SaveData(listTracks);
            file.write(new Gson().toJson(toSave));
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "save() failed");
            e.printStackTrace();
        }
    }

    private void load() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(saveFile));
            SaveData getSave = new Gson().fromJson(br, SaveData.class);
            if(getSave!=null)
            listTracks = getSave.getListTracks();
        } catch (IOException e) {
            Log.e(LOG_TAG, "load() failed");
            e.printStackTrace();
        }
    }

    public void renameTrack(TrackItem track, String newName) {
        track.renameTrack(newName);
        save();
    }

    public void setTrackDescription(TrackItem track, String description) {
        track.setTrackDescription(description);
        save();
    }

   public boolean addTrack1(File temp, String trackName, String trackDescription,
                      String fileSize, String duration, String dateRecorded, String device) throws IOException {
        if (addTrack(temp, trackName, trackDescription, fileSize, duration, dateRecorded, device)) {
            save();
            return true;
        } else {
            return false;
        }
    }

    private boolean addTrack(File temp, String trackName, String trackDescription,
                             String fileSize, String duration, String dateRecorded, String device) throws IOException {
        try {
            getTrack(trackName);
            return false;
        } catch (IllegalArgumentException e) {
            listTracks.add(0, new TrackItem(temp, trackName, trackDescription, fileSize, duration, dateRecorded, device));
            return true;
        }
    }

    private TrackItem getTrack(String trackName) throws IllegalArgumentException {
        for (TrackItem track : listTracks) {
            if (track.getTrackName().toUpperCase().equals(trackName.toUpperCase())) {
                return track;
            }
        }
        throw new IllegalArgumentException("Track not found");
    }

    ArrayList<TrackItem> getTracksList() {
        return listTracks;
    }

   public void removeTrack(TrackItem track) {
        track.remove();
        listTracks.remove(track);
    }

    public void removeDeletedTracks1() {
        //removes tracks deleted externally from an album.
        removeDeletedTracks();
        save();
    }

    private void removeDeletedTracks() {
        //removes tracks deleted externally from the list of tracks.
        Iterator<TrackItem> iterator = listTracks.iterator();
        while (iterator.hasNext()) {
            TrackItem track = iterator.next();
            if(!track.fileExists()) {
                iterator.remove();
            }
        }
    }
}
