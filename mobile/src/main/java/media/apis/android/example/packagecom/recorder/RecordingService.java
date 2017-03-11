package media.apis.android.example.packagecom.recorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RecordingService extends Service {
    public RecordingService() {
    }

    @Override
    public void onCreate() {
        // The service is being created
        //Recorder.newInstance().toggleAudioRecord();
//        WavAudioRecorder mRecord = WavAudioRecorder.getInstance();
//        String mRecordFilePath = Environment.getExternalStorageDirectory() + "/Recorder/temp.wav";
//        mRecord.setOutputFile(mRecordFilePath);
//        mRecord.prepare();
//        mRecord.start();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("recorder service")
                .setContentText("blablabla")
                .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                .setContentIntent(pendingIntent)
                .setTicker("alala")
                .build();

        startForeground(2, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
