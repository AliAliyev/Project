package media.apis.android.example.packagecom.recorder;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by Ali on 31/01/2017.
 */

public class TrackItem {
    private String trackName;
    private String description;
    private String fileSize;
    private String dateRecorded;
    private String duration;
    private String device;
    private File trackPath;
    private boolean selected;

    public TrackItem(File temp, final String trackName, final String description, final String fileSize,
                     final String duration, final String dateRecorded, final String device) throws IOException {
        this.trackName = trackName;
        if(description.equals("") ) {
            this.description = "No description";
        } else {
            this.description = description;
        }
        this.fileSize = fileSize;
        this.trackPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Recorder/" + trackName + ".wav");
        temp.renameTo(trackPath);
        //copy(temp, this.trackPath);
        this.duration = duration;
        this.dateRecorded = dateRecorded;
        this.device = device;
        this.selected = false;
    }

//    private void copy(File src, File dst) throws IOException {
//        InputStream in = new FileInputStream(src);
//        OutputStream out = new FileOutputStream(dst);
//        // Transfer bytes from in to out
//        byte[] buf = new byte[1024];
//        int len;
//        while ((len = in.read(buf)) > 0) {
//            out.write(buf, 0, len);
//        }
//        in.close();
//        out.close();
//    }

    private String setFilename(String name) {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        return fileName + "/Recorder/" + name + ".wav";
    }

    public String getTrackName() {
        return trackName;
    }

    public String getTrackDescription() { return description; }

    public File getTrackPath() {return trackPath;}

    public String getTrackSize() { return fileSize; }

    public String getDateRecorded() { return dateRecorded; }

    public String getTrackDuration() { return duration; }

    public String getDevice() { return device; }

    public void setTrackDescription(final String description) { this.description = description; }

    public boolean fileExists() {
        return trackPath.exists();
    }

    public void remove() {
        trackPath.delete();
    }

    public void renameTrack(String newName) {
        File newPath = new File(setFilename(newName));
        trackPath.renameTo(newPath);
        trackPath = newPath;
        trackName = newName;
    }

    public boolean trackSelected(boolean selected) {
        return this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

}
