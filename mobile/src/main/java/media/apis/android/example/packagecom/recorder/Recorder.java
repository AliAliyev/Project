package media.apis.android.example.packagecom.recorder;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.shuyu.waveview.AudioWaveView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Recorder.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Recorder#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Recorder extends Fragment {

    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private RemoteViews remoteViews;

    public AudioWaveView audioWave;
    public static ArrayList<Short> dataList;
    public static int mMaxSize;
    private boolean mPause;
    private WavAudioRecorder mRecord;
    private String mRecordFilePath;
    private Animation animation;
    //private ProgressBar mProgressBar;
    private Chronometer mChronometer;
    private ImageButton mRecordButton, mSaveButton, mCancelButton;
    private boolean mIsRecording;
    private TextView mPauseText;
    private long timeWhenStopped = 0;
    private String fileSize;
    //private File tempRaw, tempWav, mergedWav, tempCopyWav;
    private int size;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;

    private OnFragmentInteractionListener mListener;

    public Recorder() {
        // Required empty public constructor
//        if(getActivity()!=null) {
//            getActivity().getSupportFragmentManager()
//                    .beginTransaction()
//                    .detach(this)
//                    .attach(this)
//                    .commit();
//        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     * @return A new instance of fragment Recorder.
     */
    // TODO: Rename and change types and number of parameters
    public static Recorder newInstance() {
        return new Recorder();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recorder, container, false);
        //visualizer = ((Visualizer) view.findViewById(R.id.visualizer)); //to stop: .stopListening()

        ///////////////////////////////////////////////////////////////////////////////////////////
        Bundle extras = getActivity().getIntent().getExtras();
        if(extras!=null)
            Toast.makeText(getActivity(), extras.getString("record"), Toast.LENGTH_SHORT).show();
        ///////////////////////////////////////////////////////////////////////////////////////////

        audioWave = (AudioWaveView) view.findViewById(R.id.audioWave);
        mRecordFilePath = Environment.getExternalStorageDirectory() + "/Recorder/temp.wav";
        initializeRecorder();
        mRecordButton = (ImageButton) view.findViewById(R.id.record);
        mRecord = WavAudioRecorder.getInstance(0);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                toggleAudioRecord();
              //if(getView()!= null)
                //  getActivity().startService(new Intent(getActivity().getBaseContext(), RecordingService.class));
            }
        });

        setRecordAnimation();
        //mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mChronometer = (Chronometer) view.findViewById(R.id.chronometer);
        mPauseText = (TextView) view.findViewById(R.id.recordingText);
        mSaveButton = (ImageButton) view.findViewById(R.id.save);
        mSaveButton.setVisibility(View.INVISIBLE);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finishRecording();
            }
        });

        mCancelButton = (ImageButton) view.findViewById(R.id.cancel);
        mCancelButton.setVisibility(View.INVISIBLE);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    cancelDialog();
            }
        });

        return view;
    }

    private void initializeRecorder() {
        //int sampleRate = 44100;
        //int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                //AudioFormat.ENCODING_PCM_16BIT);
        //mBuffer = new short[bufferSize];
        //mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
          //      AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mIsRecording = false;
        mPause = true;
    }

    public void toggleAudioRecord() {
        mSaveButton.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
        setDataList(audioWave.getRecList(), size);
        if(!mIsRecording) {
            startRecording();
        } else {
            if (mPause) {
                resumeRecording();
            } else {
                pauseRecording();
            }
        }
    }

    private void startRecording() {
        mPauseText.setVisibility(View.INVISIBLE);
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        mRecordButton.startAnimation(animation);
        //int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
        //setDataList(audioWave.getRecList(), size);

        mRecord.setOutputFile(mRecordFilePath);
        mRecord.prepare();
        mRecord.start();
        setupNotification();

        //int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
        //mRecord.setDataList(audioWave.getRecList(), size);
        //audioWave.setWaveCount(10);
        //audioWave.setWaveColor(5);
        audioWave.startView();
        mIsRecording = true;
        mPause = false;
    }

    private void resumeRecording(){
        //int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
        //setDataList(audioWave.getRecList(), size);
        mPauseText.setVisibility(View.INVISIBLE);
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        mRecordButton.startAnimation(animation);
        mPause = false;
        mRecord.resume();
        switchToNotificationPlayMode();
    }

    private void pauseRecording(){
        mPauseText.setVisibility(View.VISIBLE);
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        mRecordButton.clearAnimation();
        mPause = true;
        mRecord.pause();
        switchToNotificationPauseMode();
        //visualizer.stopListening();
    }
/*
    private void sendData(short[] shorts, int readSize) {
        if (dataList != null) {
            int length = readSize / 300;
            short resultMax = 0, resultMin = 0;
            for (short i = 0, k = 0; i < length; i++, k += 300) {
                for (short j = k, max = 0, min = 1000; j < k + 300; j++) {
                    if (shorts[j] > max) {
                        max = shorts[j];
                        resultMax = max;
                    } else if (shorts[j] < min) {
                        min = shorts[j];
                        resultMin = min;
                    }
                }
                if (dataList.size() > mMaxSize) {
                    dataList.remove(0);
                }
                dataList.add(resultMax);
            }
        }
    }
*/

    public void setDataList(ArrayList<Short> dataList, int maxSize) {
        Recorder.dataList = dataList;
        mMaxSize = maxSize;
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            //audioWave.stopView();
//            //audioWave = (AudioWaveView) getView().findViewById(R.id.audioWave);
//            int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
//            setDataList(audioWave.getRecList(), size);
//            //mMaxSize = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
//            //audioWave.startView();
//            Toast.makeText(getActivity(), "landscape", Toast.LENGTH_SHORT).show();
//        } else {
//            int size = getScreenWidth(getActivity()) / dip2px(getActivity(), 1);
//            setDataList(audioWave.getRecList(), size);
//            //audioWave.startView();
//            Toast.makeText(getActivity(), "portrait", Toast.LENGTH_SHORT).show();
//        }
//    }

    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public static int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }

    private void setupNotification() {
        remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.notification);
        remoteViews.setImageViewResource(R.id.notification_image, R.mipmap.ic_launcher);
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.ic_media_pause);
        remoteViews.setImageViewResource(R.id.notif_stop, R.drawable.cast_ic_expanded_controller_stop);
        remoteViews.setTextViewText(R.id.notification_status, "Recording...");
        remoteViews.setTextViewText(R.id.notification_pause, "PAUSE");
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                timeWhenStopped, null, true);

        manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        builder = new NotificationCompat.Builder(getActivity());
        builder.setSmallIcon(R.drawable.microphone)
                .setContent(remoteViews)
                .setOngoing(true)
        //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
        //.setContentTitle("Recorder")
        //.setContentText("Recording...")
        //.setWhen(timeWhenStopped)
        //.setUsesChronometer(true)
        //.addAction(R.drawable.cast_ic_expanded_controller_pause, "Pause", contentIntent)
        //.addAction(R.drawable.cast_ic_expanded_controller_stop, "Save", contentIntent);
        ;

        Intent notificationIntent = new Intent(getActivity().getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getActivity(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_pause_resume, contentIntent);
        remoteViews.setOnClickPendingIntent(R.id.notif_stop, contentIntent);
        builder.setContentIntent(contentIntent);

        manager.notify(0, builder.build());
    }

    private void switchToNotificationPlayMode() {
        remoteViews.setTextViewText(R.id.notification_status, "Recording...");
        remoteViews.setTextViewText(R.id.notification_pause, "PAUSE");
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.ic_media_pause);
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                timeWhenStopped, null, true);

        manager.notify(0, builder.build());
    }

    private void switchToNotificationPauseMode() {
        remoteViews.setTextViewText(R.id.notification_status, "Paused");
        remoteViews.setTextViewText(R.id.notification_pause, "RESUME");
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.microphone);
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                timeWhenStopped, null, false);

        manager.notify(0, builder.build());
    }

    private void setRecordAnimation() {
        animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button
    }

    private void cancelDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Delete current recording?");
        dialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelRecording();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialogCancel = dialogBuilder.create();
        dialogCancel.show();
    }

    private void finishRecording() {
        if(!mPause) {
            mRecord.pause();
            //mRecorder.stop();
            //visualizer.stopListening();
            timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
            mChronometer.stop();
            mRecordButton.clearAnimation();
            mPause = true;
        }
        inputDialog();
    }

    private void cancelRecording() {
        mCancelButton.setVisibility(View.INVISIBLE);
        mPauseText.setVisibility(View.INVISIBLE);
        mSaveButton.setVisibility(View.INVISIBLE);
        mChronometer.stop();
        mChronometer.setText("00:00");
        timeWhenStopped = 0;
        mRecordButton.clearAnimation();
        mRecord.stop();
        mRecord.reset();
        audioWave.stopView();
        mIsRecording = false;
        mPause = true;
        manager.cancel(0);
        deleteTempFile();
        //visualizer.stopListening();
    }

    @Override
    public void onDestroy() {
        if(manager!=null) { manager.cancel(0); }
        mRecord.reset();
        deleteTempFile();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        //deleteTempFile();
        super.onStop();
    }

    private void inputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Save as..");

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inputDialogView = inflater.inflate(R.layout.record_input_dialog, null);
        builder.setView(inputDialogView);

        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
        final String current = sdf.format(new Date());
        Date date = new Date();
        String time         = (String) DateFormat.format("HH:mm", date); // 17:05
        String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
        String day          = (String) DateFormat.format("dd", date); // 20
        String monthString  = (String) DateFormat.format("MMM",  date); // Jun
        String monthNumber  = (String) DateFormat.format("MM",   date); // 06
        String year         = (String) DateFormat.format("yyyy", date); // 2013

        final String fileName = "Handheld " + day + " " + monthString + " " + year + ", " + current;
        final String dateRecorded = dayOfTheWeek + ", " + day + " " + monthString + " " + year + ", " + time;
        final EditText filenameInput = (EditText) inputDialogView.findViewById(R.id.filename);
        //filenameInput.setText("Recording " + current);
        filenameInput.setText(fileName);
        filenameInput.setSelectAllOnFocus(true);
        filenameInput.requestFocus();
        filenameInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(filenameInput, 0);
            }
        }, 10);

        final EditText descriptionInput = (EditText) inputDialogView.findViewById(R.id.description);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRecord.stop();
                mRecord.reset();
                mIsRecording = false;
                audioWave.stopView();
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder/temp.wav");
                float filesize = file.length();

                if(filesize >= 1024 * 1024 * 1024) {
                    filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024 * 1024)));
                    fileSize = String.valueOf(filesize) + " GB";
                }
                else if (filesize >= 1024 * 1024) {
                    filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024)));
                    fileSize = String.valueOf(filesize) + " MB";
                }
                else if (filesize >= 1024) {
                    filesize = filesize/(1024);
                    fileSize = String.valueOf((long) filesize) + " KB";
                }
                else fileSize = String.valueOf(filesize) + "B";

                String trackName = filenameInput.getText().toString();
                String trackDescription = descriptionInput.getText().toString();
                DataManager dataManager = new DataManager();
                try {
                    //if (dataManager.addTrack1(tempCopyWav, trackName, trackDescription,
                      //      String.valueOf(fileSize), mChronometer.getText().toString(), dateRecorded, "Handheld")) {
                    if (dataManager.addTrack1(file, trackName, trackDescription,
                                  String.valueOf(fileSize), mChronometer.getText().toString(), dateRecorded, "Handheld")) {
                        mListener.onFragmentInteraction();
                        showToast("File saved");
                        //AutoUpload(filenameInput.getText().toString());
                    } else {
                        //dataManager.addTrack1(tempCopyWav, fileName, trackDescription, String.valueOf(fileSize),
                          //              mChronometer.getText().toString(), dateRecorded, "Handheld");
                        dataManager.addTrack1(file, fileName, trackDescription, String.valueOf(fileSize),
                                mChronometer.getText().toString(), dateRecorded, "Handheld");
                        mListener.onFragmentInteraction();
                        showToast("A file with the same name already exists. File saved with default name.");
                        //AutoUpload("Recording " + current);
                    }

                } catch (IOException e) {
                    //Log.e(LOG_TAG, "copy() failed");
                    showToast("ERROR: cannot save file");
                }
                timeWhenStopped = 0;
                mChronometer.setText("00:00");
                mCancelButton.setVisibility(View.INVISIBLE);
                mPauseText.setVisibility(View.INVISIBLE);
                mSaveButton.setVisibility(View.INVISIBLE);
                manager.cancel(0);
            }
        });
        builder.show().setCanceledOnTouchOutside(false);
    }

    private void showToast(String text) {
        Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void deleteTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder/temp.wav");
        file.delete();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

/*
     private void startBufferedWrite() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (mIsRecording) {
                    //double sum = 0;
                    int numOfBytes = WavAudioRecorder.audioRecorder.read(mBuffer, 0, mBuffer.length);
//                    for (int i = 0; i < numOfBytes; i++) {
//                        output.writeShort(mBuffer[i]);
//                        sum += mBuffer[i] * mBuffer[i];
//                    }

                    if (numOfBytes > 0) {
                        //final double amplitude = sum / readSize;
                        //mProgressBar.setProgress((int) Math.sqrt(amplitude));
                        sendData(mBuffer, numOfBytes);
                    }
                }
            }
        }).start();
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
            writeInt(output, sampleRate); // sample rate
            writeInt(output, sampleRate * 2); // byte rate
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

    private void deleteTempFiles() {
        //if (tempRaw != null) tempRaw.delete();
        //if (tempWav != null) tempWav.delete();
        //if (tempCopyWav != null) tempCopyWav.delete();
        //if (mergedWav != null) mergedWav.delete();
    }

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

        public void CombineWaveFile(String file1, String file2) {
            FileInputStream in1, in2;
            final int RECORDER_BPP = 16;
            FileOutputStream out;
            long totalAudioLen;
            long totalDataLen;
            int channels = 1;
            long byteRate = RECORDER_BPP * sampleRate * channels / 8;
            int bufferSize = 1024;
            byte[] data = new byte[bufferSize];

            try {
                in1 = new FileInputStream(file1);
                in2 = new FileInputStream(file2);
                out = new FileOutputStream(mergedWav);
                totalAudioLen = in1.getChannel().size() + in2.getChannel().size();
                totalDataLen = totalAudioLen + 36;

                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                        (long) sampleRate, channels, byteRate, RECORDER_BPP);

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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction();
    }
}
