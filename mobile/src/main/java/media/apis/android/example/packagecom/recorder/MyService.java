package media.apis.android.example.packagecom.recorder;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyService extends Service implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Player.OnFragmentInteractionListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "Recorder";

    public MyService() {

    }

    @Override
    public void onCreate() {
        // The service is being created
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        Notification notification = new Notification.Builder(this)
//                .setContentTitle("asdf")
//                .setContentText("blablabla")
//                .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
//                .setContentIntent(pendingIntent)
//                .setTicker("alala")
//                .build();
//
//        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Toast.makeText(getApplicationContext(), "Receiving data", Toast.LENGTH_LONG).show();
        //Log.d(TAG, "data changed spotted");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(Environment.getExternalStorageDirectory().
                            getAbsolutePath() + "/recording.wav")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset myAsset = dataMapItem.getDataMap().getAsset("asset");
                Wearable.DataApi.getFdForAsset(mGoogleApiClient, myAsset).setResultCallback(
                        new ResultCallback<DataApi.GetFdForAssetResult>() {
                            @Override
                            public void onResult(@NonNull DataApi.GetFdForAssetResult getFdForAssetResult) {
                                InputStream assetInputStream = getFdForAssetResult.getInputStream();
                                SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
                                final String current = sdf.format(new Date());
                                Date date = new Date();
                                //String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
                                String day          = (String) DateFormat.format("dd", date); // 20
                                String monthString  = (String) DateFormat.format("MMM",  date); // Jun
                                //String monthNumber  = (String) DateFormat.format("MM",   date); // 06
                                String year         = (String) DateFormat.format("yyyy", date); // 2013
                                File file = new File(Environment.getExternalStorageDirectory().
                                        getAbsolutePath()+ "/Recorder/" + "Wear " + day + " " +
                                        monthString  + " " + year + ", " + current + ".wav");
                                try {
                                    FileOutputStream fOut = new FileOutputStream(file);
                                    int nRead;
                                    byte[] data = new byte[16384];
                                    while ((nRead = assetInputStream.read(data, 0, data.length)) != -1) {
                                        fOut.write(data, 0, nRead);
                                    }
                                    fOut.flush();
                                    fOut.close();
                                    syncFiles();
                                    onFragmentInteraction();
                                } catch (IOException e) {
                                    //System.out.println("ERROR File write failed: " + e.toString());
                                }
                            }
                        }
                );
            }
        }
    }

    private void syncFiles() {
        DataManager dataManager = new DataManager();
        int fileFound=0;
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder");
        File[] files = f.listFiles();
        for (File inFile : files) {
            if (inFile.isFile() && inFile.getName().startsWith("Wear")) {
                String filename = null;
                int pos = inFile.getName().lastIndexOf(".");
                if (pos > 0) {
                    filename = inFile.getName().substring(0, pos);
                }
                for (TrackItem track : dataManager.getTracksList()) {
                    if (filename != null && filename.equalsIgnoreCase(track.getTrackName().trim())) {
                        fileFound = 1;
                        break;
                    }
                }
                if(fileFound==0) {
                    MediaPlayer mp = new MediaPlayer();
                    FileInputStream fs;
                    FileDescriptor fd;
                    try {
                        fs = new FileInputStream(inFile);
                        fd = fs.getFD();
                        mp.setDataSource(fd);
                        mp.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int length = mp.getDuration();
                    mp.release();
                    String duration = String.format(Locale.ENGLISH, "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(length) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(length)),
                            TimeUnit.MILLISECONDS.toSeconds(length) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length)));
                    SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
                    final String current = sdf.format(new Date());
                    Date date = new Date();
                    String time         = (String) DateFormat.format("HH:mm", date); // 17:05
                    String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
                    String day          = (String) DateFormat.format("dd", date); // 20
                    String monthString  = (String) DateFormat.format("MMM",  date); // Jun
                    //String monthNumber  = (String) DateFormat.format("MM",   date); // 06
                    String year         = (String) DateFormat.format("yyyy", date); // 2013

                    String fileSize;
                    float filesize = inFile.length();
                    if(filesize >= 1024 * 1024 * 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024 * 1024)));
                        fileSize = String.valueOf(filesize) + " GB";
                    }
                    else if (filesize >= 1024 * 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024)));
                        fileSize = String.valueOf(filesize) + " MB";
                    }
                    else if (filesize >= 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%f", filesize/(1024)));
                        fileSize = String.valueOf((long) filesize) + " KB";
                    }
                    else fileSize = String.valueOf(filesize) + "B";

                    String fileName = "Watch " + day + " " + monthString + " " + year + ", " + current;
                    String dateRecorded = dayOfTheWeek + ", " + day + " " + monthString + " " + year + ", " + time;
                    try {
                        dataManager.addTrack1(inFile, fileName, "No description", fileSize, duration,
                                                dateRecorded, "Watch");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    inFile.delete();
                }
                else fileFound=0;
            }
        }
    }

//    public void onClickCreateFile(){
//        //fileOperation = true;
//        // create new contents resource
//        Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                .setResultCallback(newDriveContentsCallback);
//
//    }
//
//    /**
//     * This is Result result handler of Drive contents.
//     * this callback method call CreateFileOnGoogleDrive() method
//     * and also call OpenFileFromGoogleDrive() method,
//     * send intent onActivityResult() method to handle result.
//     */
//
//    ResultCallback<DriveApi.DriveContentsResult> newDriveContentsCallback = new
//            ResultCallback<DriveApi.DriveContentsResult>() {
//                @Override
//                public void onResult(DriveApi.DriveContentsResult result) {
//                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
//                            .setMimeType("text/html").build();
//                    IntentSender intentSender = Drive.DriveApi
//                            .newCreateFileActivityBuilder()
//                            .setInitialMetadata(metadataChangeSet)
//                            .setInitialDriveContents(result.getDriveContents())
//                            .build(mGoogleApiClient);
//                    try {
//                        startIntentSenderForResult(
//                                intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
//                    } catch (IntentSender.SendIntentException e) {
//                        Log.w(TAG, "Unable to send intent", e);
//                    }
//                }
//            };
//
//    public void CreateFileOnGoogleDrive(DriveContentsResult result){
//
//        final DriveContents driveContents = result.getDriveContents();
//
//        // Perform I/O off the UI thread.
//        new Thread() {
//            @Override
//            public void run() {
//                // write content to DriveContents
//                OutputStream outputStream = driveContents.getOutputStream();
//                Writer writer = new OutputStreamWriter(outputStream);
//                try {
//                    writer.write(&quot;Hello abhay!&quot;);
//                    writer.close();
//                } catch (IOException e) {
//                    Log.e(TAG, e.getMessage());
//                }
//
//                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
//                        .setTitle(&quot;abhaytest2&quot;)
//                .setMimeType(&quot;text/plain&quot;)
//                .setStarred(true).build();
//
//                // create a file in root folder
//                Drive.DriveApi.getRootFolder(mGoogleApiClient)
//                        .createFile(mGoogleApiClient, changeSet, driveContents)
//                        .setResultCallback(fileCallback);
//            }
//        }.start();
//    }
//
//    private void saveFileToDrive() {
//        // Start by creating a new contents, and setting a callback.
//        Log.i(TAG, "Creating new contents.");
//        final Bitmap image = mBitmapToSave;
//        Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                .setResultCallback(new ResultCallback<DriveContentsResult>() {
//
//                    @Override
//                    public void onResult(DriveContentsResult result) {
//                        // If the operation was not successful, we cannot do anything
//                        // and must
//                        // fail.
//                        if (!result.getStatus().isSuccess()) {
//                            Log.i(TAG, "Failed to create new contents.");
//                            return;
//                        }
//                        // Otherwise, we can write our data to the new contents.
//                        Log.i(TAG, "New contents created.");
//                        // Get an output stream for the contents.
//                        OutputStream outputStream = result.getDriveContents().getOutputStream();
//                        // Write the bitmap data from it.
//                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
//                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
//                        try {
//                            outputStream.write(bitmapStream.toByteArray());
//                        } catch (IOException e1) {
//                            Log.i(TAG, "Unable to write file contents.");
//                        }
//                        // Create the initial metadata - MIME type and title.
//                        // Note that the user will be able to change the title later.
//                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
//                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
//                        // Create an intent for the file chooser, and start it.
//                        IntentSender intentSender = Drive.DriveApi
//                                .newCreateFileActivityBuilder()
//                                .setInitialMetadata(metadataChangeSet)
//                                .setInitialDriveContents(result.getDriveContents())
//                                .build(mGoogleApiClient);
//                        try {
//                            startIntentSenderForResult(
//                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
//                        } catch (SendIntentException e) {
//                            Log.i(TAG, "Failed to launch file chooser.");
//                        }
//                    }
//                });
//    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();

//        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
//        if (!connectionResult.hasResolution()) {
//            // show the localized error dialog.
//            GoogleApiAvailability.getInstance().getErrorDialog(, connectionResult.getErrorCode(), 0).show();
//            return;
//        }
//        // The failure has a resolution. Resolve it.
//        // Called typically when the app is not yet authorized, and an
//        // authorization
//        // dialog is displayed to the user.
//        try {
//            connectionResult.startResolutionForResult(MainActivity.this, 3);
//        } catch (IntentSender.SendIntentException e) {
//            Log.e(TAG, "Exception while starting resolution activity", e);
//        }
    }

    @Override
    public void onFragmentInteraction() {
        if(MainActivity.getFragmentRefreshListener()!= null){
            MainActivity.getFragmentRefreshListener().onRefresh();
        }
    }
}
