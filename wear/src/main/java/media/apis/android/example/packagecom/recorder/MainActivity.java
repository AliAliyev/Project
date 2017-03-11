package media.apis.android.example.packagecom.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private boolean mPause;
    private ImageButton record;
    private Button save, cancel;
    private Chronometer mChronometer;
    private Animation animation;
    //private short[] mBuffer;
    private boolean mIsRecording = false;
    public ProgressBar mProgressBar;
    //private AudioRecord mRecorder;
    //private File tempRaw, tempWav, tempCopyWav, mergedWav;
    private long timeWhenStopped = 0;
    private static final String TAG = MainActivity.class.getName();
    private GoogleApiClient mGoogleApiClient;
    //public static final int SAMPLE_RATE = 44100;
    private static final int MY_PERMISSIONS_REQUEST_MULTIPLE = 1;
    private WavAudioRecorder mRecord;
    private String mRecordFilePath;
    public static long fileLength;
//    private ProgressDialog pDialog;

    public PendingResult<DataApi.DataItemResult> pendingResult;
    public PutDataRequest request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_MULTIPLE);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        mRecordFilePath = Environment.getExternalStorageDirectory() + "/temp.wav";
        //Log.d(TAG, "activity started");
        //Toast.makeText(MainActivity.this, "Started!", Toast.LENGTH_SHORT).show();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                initRecorder();
                mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                record = (ImageButton) findViewById(R.id.imageButton);
                save = (Button) findViewById(R.id.saveButton);
                cancel = (Button) findViewById(R.id.cancelButton);
                cancel.setVisibility(View.INVISIBLE);
                save.setVisibility(View.INVISIBLE);
                mChronometer = (Chronometer) findViewById(R.id.chronometer);
                animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
                animation.setDuration(500); // duration - half a second
                animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
                animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
                animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end
                mRecord = WavAudioRecorder.getInstance();
                record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleAudioRecord();
                        cancel.setVisibility(View.VISIBLE);
                        save.setVisibility(View.VISIBLE);
                    }

                });

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveRecording();
                        syncRecording();
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelRecording();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Destroyed", Toast.LENGTH_LONG).show();
        mGoogleApiClient.disconnect();

//        pendingResult.cancel();
//        request.removeAsset("asset");
//        request.removeAsset("timestamp");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MULTIPLE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                } else {
                    // permission denied, boo!
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

//    /**
//     * Showing Dialog
//     * */
//    @Override
//    protected Dialog onCreateDialog(int id) {
//        switch (id) {
//            case 0: // we set this to 0
//                pDialog = new ProgressDialog(this);
//                pDialog.setMessage("Downloading file. Please wait...");
//                pDialog.setIndeterminate(false);
//                pDialog.setMax(100);
//                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                pDialog.setCancelable(true);
//                pDialog.show();
//                return pDialog;
//            default:
//                return null;
//        }
//    }

    private void toggleAudioRecord() {
        if (!mIsRecording) {
            startRecording();
        } else {
            if (mPause) {
                resumeRecording();
            }
            else {
                pauseRecording();
            }
        }
    }

    private void startRecording() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        record.startAnimation(animation);
        mRecord.setOutputFile(mRecordFilePath);
        mRecord.prepare();
        mRecord.start();
        startBufferedWrite();
        mIsRecording = true;
        mPause = false;
    }

    private void resumeRecording() {
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        record.startAnimation(animation);
        mPause = false;
        mRecord.resume();
        startBufferedWrite();
    }

    private void pauseRecording() {
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        record.clearAnimation();
        mPause = true;
        mRecord.pause();
    }

    private void cancelRecording() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancel.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        record.clearAnimation();
        mIsRecording = false;
        mPause = true;
        mRecord.stop();
        mRecord.reset();
        deleteTempFile();
    }

    private void saveRecording() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancel.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        record.clearAnimation();
        mIsRecording = false;
        mPause = true;
        mRecord.stop();
        mRecord.reset();
    }

    private void syncRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Node> connectedNodes =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
                if (connectedNodes.size() > 0) {
                    FileInputStream fileInputStream;
                    File file = new File(Environment.getExternalStorageDirectory().
                            getAbsolutePath() + "/temp.wav");
                    byte[] bFile = new byte[(int) file.length()];
                    fileLength = file.length();
                    //convert file into array of bytes
                    try {
                        fileInputStream = new FileInputStream(file);
                        fileInputStream.read(bFile);
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Asset asset = Asset.createFromBytes(bFile);
                    PutDataMapRequest dataMap = PutDataMapRequest.create(Environment.
                            getExternalStorageDirectory().getAbsolutePath() + "/recording.wav");
                    dataMap.getDataMap().putAsset("asset", asset);
                    dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
                    request = dataMap.asPutDataRequest();
                    request.setUrgent();
                    pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                                startActivity(new Intent(getApplicationContext(),
                                        SyncActivity.class));
                            } else {
                                Log.d(TAG, "ERROR: Data item lost");
                            }
                            //Toast.makeText(getApplicationContext(), "Syncing data",
                            // Toast.LENGTH_LONG).show();
                        }
                    });
                    //Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    deleteTempFile();
                }
                else {
                    Intent intent = new Intent(MainActivity.this, ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.FAILURE_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Sending unsuccessful. " +
                            "Please connect to a handheld device.");
                    startActivity(intent);
                }
            }
        }).start();
    }

    private void initRecorder() {
//        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT);
//        mBuffer = new short[bufferSize];
//        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mIsRecording = false;
        mPause = true;
    }

    private void deleteTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.wav");
        file.delete();
    }

    private void startBufferedWrite() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!mPause) {
//                        double sum = 0;
//                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
//                        for (int i = 0; i < readSize; i++) { sum += mBuffer[i] * mBuffer[i]; }
//                        if (readSize > 0) {
//                            final double amplitude = sum / readSize;
                            mProgressBar.setProgress((int) Math.sqrt(20 * mRecord.amplitude));
//                        }
                    }
                } finally {
                    mProgressBar.setProgress(0);
                }
            }
        }).start();
    }
/*
    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    public void CombineWaveFile(String file1, String file2) {
        FileInputStream in1, in2;
        final int RECORDER_BPP = 16;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        int channels = 1;
        long byteRate = RECORDER_BPP * SAMPLE_RATE * channels / 8;
        int bufferSize = 1024;
        byte[] data = new byte[bufferSize];
        try {
            in1 = new FileInputStream(file1);
            in2 = new FileInputStream(file2);
            out = new FileOutputStream(mergedWav);
            totalAudioLen = in1.getChannel().size() + in2.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    (long) SAMPLE_RATE, channels, byteRate, RECORDER_BPP);
            while (in1.read(data) != -1)
                out.write(data);
            while (in2.read(data) != -1)
                out.write(data);
            out.close();
            in1.close();
            in2.close();
            out.close();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels, long byteRate,
                                     int RECORDER_BPP) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        header[34] = (byte) RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
*/
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
