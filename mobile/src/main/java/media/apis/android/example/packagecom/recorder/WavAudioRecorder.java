package media.apis.android.example.packagecom.recorder;

/**
 * Created by Ali on 07/02/2017.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

import com.shuyu.waveview.AudioWaveView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class WavAudioRecorder {
    private final static int[] sampleRates = {44100, 22050,  11025, 8000};

    public static WavAudioRecorder getInstance(int sampleRate) {
        WavAudioRecorder result;
        int i=0;
        do {
            result = new WavAudioRecorder(AudioSource.MIC,
                    sampleRates[i],
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
        } while((++i<sampleRates.length) & !(result.getState() == WavAudioRecorder.State.INITIALIZING));
//        result = new WavAudioRecorder(AudioSource.MIC,
//                    sampleRates[sampleRate],
//                    AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT);
        return result;
    }

    /**
     * INITIALIZING : recorder is initializing;
     * READY : recorder has been initialized, recorder not yet started
     * RECORDING : recording
     * ERROR : reconstruction needed
     * STOPPED: reset needed
     */
    public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED}

    public static final boolean RECORDING_UNCOMPRESSED = true;
    public static final boolean RECORDING_COMPRESSED = false;

    private ArrayList<Short> dataList;
    private int mMaxSize;
    private AudioWaveView audioWave;

    // The interval in which the recorded samples are output to the file
    // Used only in uncompressed mode
    private static final int TIMER_INTERVAL = 120;

    // Recorder used for uncompressed recording
    public static AudioRecord     audioRecorder = null;

    // Output file path
    private String          filePath = null;

    // Recorder state; see State
    private State          	state;

    // File writer (only in uncompressed mode)
    private RandomAccessFile randomAccessWriter;

    // Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
    private short                    nChannels;
    private int                      sRate;
    private short                    mBitsPerSample;
    private int                      mBufferSize;
    private int                      mAudioSource;
    private int                      aFormat;

    // Number of frames/samples written to file on each output(only in uncompressed mode)
    private int                      mPeriodInFrames;

    // Buffer for output(only in uncompressed mode)
    private byte[]                   buffer;
    private byte[]                   totalBuffer;

    // Number of bytes written to file after header(only in uncompressed mode)
    // after stop() is called, this size is written to the header/data chunk in the wave file
    private int                      payloadSize;

    // Is the recording in progress
    private boolean mIsRecording = false;

    /**
     *
     * Returns the state of the recorder in a WavAudioRecorder.State typed object.
     * Useful, as no exceptions are thrown.
     *
     * @return recorder state
     */
    public State getState() {
        return state;
    }

    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        //	periodic updates on the progress of the record head
        public void onPeriodicNotification(AudioRecord recorder) {
            if (State.STOPPED == state) {
                Log.d(WavAudioRecorder.this.getClass().getName(), "recorder stopped");
                return;
            }
            int numOfBytes = audioRecorder.read(buffer, 0, buffer.length); // read audio data to buffer

            //byte[] bytes = {};
            short[] shorts = new short[buffer.length/2];
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            if(numOfBytes > 0 && mIsRecording) sendData(shorts, numOfBytes/5);
//			Log.d(WavAudioRecorder.this.getClass().getName(), state + ":" + numOfBytes);
//            outputStream = new ByteArrayOutputStream();
//            try {
//                if(totalBuffer!=null)
//                outputStream.write(totalBuffer);
//                outputStream.write(buffer);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            totalBuffer = outputStream.toByteArray( );
//
//            else {
//                ByteBuffer target = ByteBuffer.wrap(totalBuffer);
//                target.put(totalBuffer);
//                target.put(buffer);
//            }

//                if (totalBuffer == null) totalBuffer = buffer;
//                else {
//                    byte[] a = new byte[buffer.length + totalBuffer.length];
//                    System.arraycopy(buffer, 0, a, totalBuffer.length, buffer.length);
//                    totalBuffer = a;
//                }

                try {
                    if (mIsRecording) {
                        randomAccessWriter.write(buffer);   // write audio data to file
                        payloadSize += buffer.length;
                    }

                } catch (IOException e) {
                    Log.e(WavAudioRecorder.class.getName(), "Error occurred in updateListener, recording is aborted");
                    e.printStackTrace();
                }
            }
        //	reached a notification marker set by setNotificationMarkerPosition(int)
        public void onMarkerReached(AudioRecord recorder) {
        }
    };

    private void sendData(short[] shorts, int readSize) {
        if (Recorder.dataList != null) {
            int length = readSize / 300;
            short resultMax = 0; //resultMin = 0;
            for (short i = 0, k = 0; i < length; i++, k += 300) {
                for (short j = k, max = 0, min = 1000; j < k + 300; j++) {
                    if (shorts[j] > max) {
                        max = shorts[j];
                        resultMax = max;
                    } else if (shorts[j] < min) {
                        min = shorts[j];
                        //resultMin = min;
                    }
                }
                if (Recorder.dataList.size() > Recorder.mMaxSize) {
                    Recorder.dataList.remove(0);
                }
                  Recorder.dataList.add(resultMax);
            }
        }
    }

    public void setDataList(ArrayList<Short> dataList, int maxSize) {
        this.dataList = dataList;
        this.mMaxSize = maxSize;
    }
//
//    public static int getScreenWidth(Context context) {
//        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        windowManager.getDefaultDisplay().getMetrics(outMetrics);
//        return outMetrics.widthPixels;
//    }
//
//    public static int dip2px(Context context, float dipValue) {
//        float fontScale = context.getResources().getDisplayMetrics().density;
//        return (int) (dipValue * fontScale + 0.5f);
//    }

    /**
     *
     *
     * Default constructor
     *
     * Instantiates a new recorder
     * In case of errors, no exception is thrown, but the state is set to ERROR
     *
     */
    public WavAudioRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat) {
        try {
            if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                mBitsPerSample = 16;
            } else {
                mBitsPerSample = 8;
            }

            if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                nChannels = 1;
            } else {
                nChannels = 2;
            }

            mAudioSource = audioSource;
            sRate   = sampleRate;
            aFormat = audioFormat;

            mPeriodInFrames = sampleRate * TIMER_INTERVAL / 1000;		//?
            mBufferSize = mPeriodInFrames * 2  * nChannels * mBitsPerSample / 8;		//?
            if (mBufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)) {
                // Check to make sure buffer size is not smaller than the smallest allowed one
                mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                // Set frame period and timer interval accordingly
                mPeriodInFrames = mBufferSize / ( 2 * mBitsPerSample * nChannels / 8 );
                Log.w(WavAudioRecorder.class.getName(), "Increasing buffer size to " + Integer.toString(mBufferSize));
            }

            audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, mBufferSize);

            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new Exception("AudioRecord initialization failed");
            }
            audioRecorder.setRecordPositionUpdateListener(updateListener);
            audioRecorder.setPositionNotificationPeriod(mPeriodInFrames);
            filePath = null;
            state = State.INITIALIZING;
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(WavAudioRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(WavAudioRecorder.class.getName(), "Unknown error occurred while initializing recording");
            }
            state = State.ERROR;
        }
    }

    /**
     * Sets output file path, call directly after construction/reset.
     *
     * @param
     *
     */
    public void setOutputFile(String argPath) {
        try {
            if (state == State.INITIALIZING) {
                filePath = argPath;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(WavAudioRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(WavAudioRecorder.class.getName(), "Unknown error occurred while setting output path");
            }
            state = State.ERROR;
        }
    }


    /**
     *
     * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
     * the recorder is set to the ERROR state, which makes a reconstruction necessary.
     * In case uncompressed recording is toggled, the header of the wave file is written.
     * In case of an exception, the state is changed to ERROR
     *
     */
    public void prepare() {
        try {
            if (state == State.INITIALIZING) {
                if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (filePath != null)) {
                    // write file header
                    randomAccessWriter = new RandomAccessFile(filePath, "rw");
                    randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
                    randomAccessWriter.writeBytes("RIFF");
                    randomAccessWriter.writeInt(0); // Final file size not known yet, write 0
                    randomAccessWriter.writeBytes("WAVE");
                    randomAccessWriter.writeBytes("fmt ");
                    randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
                    randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
                    randomAccessWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
                    randomAccessWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
                    randomAccessWriter.writeInt(Integer.reverseBytes(sRate*nChannels*mBitsPerSample/8)); // Byte rate, SampleRate*NumberOfChannels*mBitsPerSample/8
                    randomAccessWriter.writeShort(Short.reverseBytes((short)(nChannels*mBitsPerSample/8))); // Block align, NumberOfChannels*mBitsPerSample/8
                    randomAccessWriter.writeShort(Short.reverseBytes(mBitsPerSample)); // Bits per sample
                    randomAccessWriter.writeBytes("data");
                    randomAccessWriter.writeInt(0); // Data chunk size not known yet, write 0
                    buffer = new byte[mPeriodInFrames*mBitsPerSample/8*nChannels];
                    state = State.READY;
                } else {
                    Log.e(WavAudioRecorder.class.getName(), "prepare() method called on uninitialized recorder");
                    state = State.ERROR;
                }
            } else {
                Log.e(WavAudioRecorder.class.getName(), "prepare() method called on illegal state");
                release();
                state = State.ERROR;
            }
        } catch(Exception e) {
            if (e.getMessage() != null) {
                Log.e(WavAudioRecorder.class.getName(), e.getMessage());
            } else {
                Log.e(WavAudioRecorder.class.getName(), "Unknown error occurred in prepare()");
            }
            state = State.ERROR;
        }
    }

    /**
     *
     *
     *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
     *
     */
    public void release() {
        if (state == State.RECORDING) {
            stop();
        } else {
            if (state == State.READY){
                try {
                    randomAccessWriter.close(); // Remove prepared file
                } catch (IOException e) {
                    Log.e(WavAudioRecorder.class.getName(), "I/O exception occurred while closing output fileoooo");
                }
                (new File(filePath)).delete();
            }
        }
        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    /**
     *
     *
     * Resets the recorder to the INITIALIZING state, as if it was just created.
     * In case the class was in RECORDING state, the recording is stopped.
     * In case of exceptions the class is set to the ERROR state.
     *
     */
    public void reset() {
        try {
            if (state != State.ERROR) {
                release();
                filePath = null; // Reset file path
                audioRecorder = new AudioRecord(mAudioSource, sRate, nChannels, aFormat, mBufferSize);
                if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new Exception("AudioRecord initialization failed");
                }
                audioRecorder.setRecordPositionUpdateListener(updateListener);
                audioRecorder.setPositionNotificationPeriod(mPeriodInFrames);
                state = State.INITIALIZING;
            }
        } catch (Exception e) {
            Log.e(WavAudioRecorder.class.getName(), e.getMessage());
            state = State.ERROR;
        }
    }

    /**
     *
     *
     * Starts the recording, and sets the state to RECORDING.
     * Call after prepare().
     *
     */
    public void start() {
        if (state == State.READY) {
            payloadSize = 0;
            //int size = getScreenWidth(MainActivity.) / dip2px(Recorder.newInstance().getActivity(), 1);
            //setDataList(Recorder.audioWave.getRecList(), size);
            audioRecorder.startRecording();
            //audioRecorder.read(buffer, 0, buffer.length); //[TODO: is this necessary]read the existing data in audio hardware, but don't do anything
            state = State.RECORDING;
            mIsRecording = true;
            //startBufferedWrite();
        }
        else {
            Log.e(WavAudioRecorder.class.getName(), "start() called on illegal state");
            state = State.ERROR;
        }
    }

    public void resume() {
        mIsRecording = true;
    }


    public void pause() {
        mIsRecording = false;
    }


    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     *
     *
     *  Stops the recording, and sets the state to STOPPED.
     * In case of further usage, a reset is needed.
     * Also finalizes the wave file in case of uncompressed recording.
     *
     */

    public void stop() {
        if (state == State.RECORDING) {
            audioRecorder.stop();
            try {
                randomAccessWriter.seek(4); // Write size to RIFF header
                randomAccessWriter.writeInt(Integer.reverseBytes(36+payloadSize));

                randomAccessWriter.seek(40); // Write size to Subchunk2Size field
                randomAccessWriter.writeInt(Integer.reverseBytes(payloadSize));

                randomAccessWriter.close();
            } catch(IOException e) {
                Log.e(WavAudioRecorder.class.getName(), "I/O exception occurred while closing output file");
                state = State.ERROR;
            }
            state = State.STOPPED;
        } else {
            Log.e(WavAudioRecorder.class.getName(), "stop() called on illegal state");
            state = State.ERROR;
        }
    }
}