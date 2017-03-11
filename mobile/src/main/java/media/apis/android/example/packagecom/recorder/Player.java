package media.apis.android.example.packagecom.recorder;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Player.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Player#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Player extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private RemoteViews remoteViews;

    private static final String TAG = "Recorder";
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private GoogleApiClient mGoogleApiClient;

    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private MediaPlayer mediaPlayer;
    private Handler myHandler = new Handler();
    private int progressValue;
    private TextView track_text, current_duration, track_duration, empty_notice;
    private SeekBar trackSeekBar;
    public static TrackItem selected, playingTrack;
    private MyListAdapter adapter;
    private ListView listView;
    private DataManager dataManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<TrackItem> trackItems;
    private boolean contextualActionBar = false;
    public static ActionMode mActionMode;
    private int lastSelectedPosition, lastPlayedTrackPosition;

    private DropboxAPI<AndroidAuthSession> dropbox;
    private boolean dropboxAuthenticationTry;
    private final static String DROPBOX_PREF = "dropbox_prefs";
    private final static String APP_KEY = "m6l3tdkthlvgxdz";
    private final static String APP_SECRET = "9xbaiwgqlh4nbeo";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    // TODO: Rename and change types of parameters

    private OnFragmentInteractionListener mListener;

    public Player() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Player.
     */
    // TODO: Rename and change types and number of parameters
    public static Player newInstance() {
        return new Player();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        dataManager = new DataManager();

        dropboxAuthentication();

        empty_notice = (TextView) view.findViewById(R.id.empty);
        track_text = (TextView) view.findViewById(R.id.track_name);
        trackSeekBar = (SeekBar) view.findViewById(R.id.track_seekbar);
        trackSeekBar.setFocusableInTouchMode(false);
        trackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer!=null) {
                    if (mediaPlayer.isPlaying()) {
                        synchronized (this) {
                            mPlayButton.setVisibility(View.INVISIBLE);
                            mPauseButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        synchronized (this) {
                            mPlayButton.setVisibility(View.VISIBLE);
                            mPauseButton.setVisibility(View.INVISIBLE);
                        }
                    }
                }
                else {
                    synchronized (this) {
                        mPlayButton.setVisibility(View.VISIBLE);
                        mPauseButton.setVisibility(View.INVISIBLE);
                    }
                }
                if (fromUser && seekBar.isInTouchMode()) {
                    progressValue = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(progressValue);
                } else {
                    trackSeekBar.setProgress(progressValue);
                }
            }
        });
        current_duration = (TextView) view.findViewById(R.id.current_track_duration);
        track_duration = (TextView) view.findViewById(R.id.total_track_duration);
        listView = (ListView) view.findViewById(R.id.trackListView);
        populateListView();
        syncFiles();
        final Vibrator v = (Vibrator) getActivity().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        mPlayButton = (ImageButton) view.findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer != null) {
                    mediaPlayer.start();
                    //track_text.setText(selected.getTrackName());
                    switchToNotificationPlayMode();
                    dataManager.getTracksList().get(lastPlayedTrackPosition).trackSelected(true);
                    adapter.notifyDataSetChanged();
                }
                else if(playingTrack != null) {
                    startPlayer(playingTrack);
                    //dataManager.getTracksList().get(lastPlayedTrackPosition).trackSelected(true);
                    //adapter.notifyDataSetChanged();
                }
//                else {
//                    track_text.setText(((TrackItem) listView.getAdapter().getItem(0)).getTrackName());
//                    startPlayer((TrackItem) listView.getAdapter().getItem(0));
//                }
                synchronized (this) {
                    mPauseButton.setVisibility(View.VISIBLE);
                    mPlayButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        mPauseButton = (ImageButton) view.findViewById(R.id.pauseButton);
        mPauseButton.setVisibility(View.INVISIBLE);
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer!=null) {
                    mediaPlayer.pause();
                    switchToNotificationPauseMode();
                }
                synchronized (this) {
                    mPauseButton.setVisibility(View.INVISIBLE);
                    mPlayButton.setVisibility(View.VISIBLE);
                }
            }
        });

        ImageButton forward = (ImageButton) view.findViewById(R.id.next);
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayer();
                trackSeekBar.setProgress(trackSeekBar.getMax());
                current_duration.setText(track_duration.getText());
            }
        });

        ImageButton previous = (ImageButton) view.findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayer();
                trackSeekBar.setProgress(0);
                current_duration.setText("00:00");
            }
        });

        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked)           trackItems.add(dataManager.getTracksList().get(position));
                else                   trackItems.remove(dataManager.getTracksList().get(position));
                if (listView.getCheckedItemCount() <= 1)  mode.setTitle(listView.getCheckedItemCount() + " item");
                else                                      mode.setTitle(listView.getCheckedItemCount() + " items");
                mode.invalidate();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.context_menu, menu);
                mActionMode = mode;
//                for (int i = 0; i < listView.getCount(); i++) {
//                    getViewByPosition(i, listView).setBackgroundColor(ContextCompat.getColor
//                            (getActivity(), R.color.colorTransparent));
//                }
                //mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.share));
                //mShareActionProvider.setShareIntent(getDefaultShareIntent());
                trackItems = new ArrayList<>();
                contextualActionBar = true;
                v.vibrate(50);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                if (listView.getCheckedItemCount() <= 1){
                    MenuItem editItem = menu.findItem(R.id.edit);
                    editItem.setVisible(true);
                    return true;
                } else {
                    MenuItem editItem = menu.findItem(R.id.edit);
                    editItem.setVisible(false);
                    return true;
                }
                //return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        if(!trackItems.isEmpty()) {
                            deleteTickedTracks();
                        }
                        else {
                            contextualActionBar = false;
                            Toast.makeText(getActivity(), "No item(s) were selected", Toast.LENGTH_SHORT).show();
                            mode.finish();
                        }
                        break;
                    case R.id.share:
                        if(!trackItems.isEmpty()) {
                            startActivity(Intent.createChooser(getDefaultShareIntent(), "Share via"));
//                            startActivityForResult(Intent.createChooser(getDefaultShareIntent(), "Share via"), 1);
//                            startActivity(getDefaultShareIntent());
                            contextualActionBar = false;
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mode.finish();
                                }
                            }, 1000);
                        }
                        else {
                            contextualActionBar = false;
                            Toast.makeText(getActivity(), "No item(s) were selected", Toast.LENGTH_SHORT).show();
                            mode.finish();
                        }
                        break;
                    case R.id.edit:
                        selected = trackItems.get(0);
                        editDialog();
                        //mode.finish();
                        break;
                    default:
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                finishContextualActionBarMode();
//                for (int i = 0; i < listView.getCount(); i++) {
//                    getViewByPosition(i, listView).setBackgroundColor(ContextCompat.getColor
//                                    (getActivity(), R.color.colorTransparent));
//                }
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(!contextualActionBar) {
                    // showing refresh animation
                    swipeRefreshLayout.setRefreshing(true);
                    populateListView();
                    syncFiles();
                    // stopping swipe refresh
                    swipeRefreshLayout.setRefreshing(false);
                }
                else {
                    Toast.makeText(getActivity(), "Deselect item(s) to refresh list", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        ((MainActivity) getActivity()).setFragmentRefreshListener(new MainActivity.FragmentRefreshListener() {
            @Override
            public void onRefresh() {
                populateListView();
            }
        });

        return view;
    }

    private void finishContextualActionBarMode() {
        listView.clearChoices();
        for(TrackItem trackItem: dataManager.getTracksList()) { trackItem.trackSelected(false); }
        trackItems.clear();
        mActionMode.finish();
        mActionMode = null;
        contextualActionBar = false;
        toggleEmptyNotice();
        checkIfLastPlayedTrackDeleted();
    }

    private boolean dropboxAuthentication() {
        //dropboxAuthenticationTry = true;
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

//        SharedPreferences prefs = getSharedPreferences(DROPBOX_PREF, 0);
//        String key = prefs.getString(APP_KEY, null);
//        String secret = prefs.getString(APP_SECRET, null);

//        if (key != null && secret != null) {
//            AccessTokenPair token = new AccessTokenPair(getString(APP_KEY), getString(APP_SECRET));
//            session = new AndroidAuthSession(appKeys, token);
//            dropbox = new DropboxAPI<>(session);
//            return true;
//
//        } else {
            session = new AndroidAuthSession(appKeys);
            dropbox = new DropboxAPI<>(session);
            //dropbox.getSession().startOAuth2Authentication(getActivity());
            return false;
        //}
    }

    private void authenticateDropBoxAccount() {
        dropbox.getSession().startOAuth2Authentication(getActivity());
        dropboxAuthenticationTry = true;
    }

    private void uploadToDropbox() {
        //Toast.makeText(getActivity(), "Uploading " + selected.getTrackName() +
          //      " to Dropbox", Toast.LENGTH_SHORT).show();
        UploadFileToDropbox upload = new UploadFileToDropbox(getContext(), dropbox, selected.getTrackPath());
        upload.execute();
        dropboxAuthenticationTry = false;
    }

    @Override
    public void onDestroy() {
        if(manager!=null) { manager.cancel(1); }
        deleteTempFile();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        //deleteTempFile();
        super.onStop();
    }

    private void deleteTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder/temp.wav");
        file.delete();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
//            case REQUEST_CODE_CAPTURE_IMAGE:
//                // Called after a photo has been taken.
//                if (resultCode == Activity.RESULT_OK) {
//                    // Store the image data as a bitmap for writing later.
//                    mBitmapToSave = (Bitmap) data.getExtras().get("data");
//                }
//                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "File successfully saved.");
                    //mBitmapToSave = null;
                    // Just start the camera again for another photo.
//                    startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
//                            REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
        }

        // Check which request we're responding to
        if (requestCode == 1) {
            if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getActivity(), "Sorry, share aborted. Just try again", Toast.LENGTH_SHORT).show();
                    startActivity(Intent.createChooser(getDefaultShareIntent1(), "Share via"));
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInfilater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInfilater.inflate(R.menu.menu_main_player, menu);
//        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
//        SearchManager searchManager = (SearchManager) getActivity().getSystemService(SEARCH_SERVICE);
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
    }

    private Intent getDefaultShareIntent(){
        ArrayList<Uri> contentUris = new ArrayList<>();
        Uri contentUri = null;
        for(TrackItem trackItem: trackItems) {
            contentUri = Uri.fromFile(trackItem.getTrackPath());
//            contentUri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID +
//                    ".provider", trackItem.getTrackPath());
            contentUris.add(contentUri);
        }
         // Add your audio URIs here
        Intent shareIntent;
        if(trackItems.size() > 1) {
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris);
        }
        else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            if (contentUri != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri.normalizeScheme());
            }
        }
        shareIntent.setType("audio/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

//        Intent sharingIntent = ShareCompat.IntentBuilder.from(getActivity())
//                .setType("audio/*")
//                .setChooserTitle("Share via")
//                .setSubject("Recorder")
//                .setStream(contentUri)
//                .getIntent();
//        sharingIntent.setData(contentUri);
//        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return shareIntent;
    }

    private Intent getDefaultShareIntent1(){
        ArrayList<Uri> fileUris = new ArrayList<>();
        Uri fileUri = null;
        for(TrackItem trackItem: trackItems) {
            fileUri = Uri.fromFile(trackItem.getTrackPath());
//            fileUri = Uri.parse(trackItem.getTrackPath().toString());
            fileUris.add(fileUri);
        }
        // Add your audio URIs here
        Intent shareIntent;
        if(trackItems.size() > 1) {
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            //shareIntent.putExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
        }
        else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        }
        shareIntent.setType("audio/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return shareIntent;
    }

    private void deleteTickedTracks() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Delete selected recording(s) ?");
        dialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (TrackItem trackItem : trackItems) {
                    dataManager.removeTrack(trackItem);
                    dataManager.removeDeletedTracks1();
                }
                finishContextualActionBarMode();
                adapter.notifyDataSetChanged();
                Toast.makeText(getActivity(), "Recording(s) deleted", Toast.LENGTH_SHORT).show();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialogDelete = dialogBuilder.create();
        dialogDelete.show();
    }

    public void syncFiles() {
        int fileFound = 0;
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder");
        File[] files = f.listFiles();
        if (files != null) {
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        for (File inFile : files) {
            if (inFile.isFile()) {
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
                if (fileFound == 0) {
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
                    String fileSize;
                    float filesize = inFile.length();
                    if (filesize >= 1024 * 1024 * 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize / (1024 * 1024 * 1024)));
                        fileSize = String.valueOf(filesize) + " GB";
                    } else if (filesize >= 1024 * 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize / (1024 * 1024)));
                        fileSize = String.valueOf(filesize) + " MB";
                    } else if (filesize >= 1024) {
                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%f", filesize / (1024)));
                        fileSize = String.valueOf((long) filesize) + " KB";
                    } else fileSize = String.valueOf(filesize) + "B";
                    Date lastModified = new Date(inFile.lastModified());
                    try {
                        dataManager.addTrack1(inFile, inFile.getName().substring(0, inFile.getName().lastIndexOf(".")),
                                "No description", fileSize, duration, lastModified.toString(), "Data lost");
                        adapter.notifyDataSetChanged();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //inFile.delete();
                } else fileFound = 0;
            }
        }
    }
    }

    private void syncDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Upload to Cloud");
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inputDialogView = inflater.inflate(R.layout.sync_cloud, null);
        builder.setView(inputDialogView);
        final AlertDialog dialogSync = builder.create();
        final TextView drive = (TextView) inputDialogView.findViewById(R.id.driveItem);
        drive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).authenticateGoogle();
                dialogSync.dismiss();
            }
        });
        final TextView dropbox = (TextView) inputDialogView.findViewById(R.id.dropboxItem);
        dropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(), "Upload " + selected.getTrackName() +
                //    " to Dropbox", Toast.LENGTH_SHORT).show();
                // if(dropboxAuthentication()) uploadToDropbox();
                //dropboxAuthentication();

                authenticateDropBoxAccount();
                //uploadToDropbox();
                dialogSync.dismiss();
            }
        });
        dialogSync.show();
        //builder.show().setCanceledOnTouchOutside(true);
    }

    private void editDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit");
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inputDialogView = inflater.inflate(R.layout.edit_dialog, null);
        builder.setView(inputDialogView);
        final EditText textInput = (EditText) inputDialogView.findViewById(R.id.filename);
        textInput.setText(selected.getTrackName());
        final EditText descInput = (EditText) inputDialogView.findViewById(R.id.description);
        descInput.setText(selected.getTrackDescription());
        textInput.setSelectAllOnFocus(true);
        textInput.requestFocus();
        textInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(textInput, 0);
            }
        }, 10);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String rename = textInput.getText().toString();
                String description = descInput.getText().toString();
                File file = new File(Environment.getExternalStorageDirectory() + "/Recorder/" + rename + ".wav");
                if(rename.equalsIgnoreCase(selected.getTrackName())) {
                    if(!description.equalsIgnoreCase(selected.getTrackDescription())) {
                        dataManager.setTrackDescription(selected, description);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), "Description successfully edited", Toast.LENGTH_SHORT).show();
                    }
                    else Toast.makeText(getActivity(), "No edit performed", Toast.LENGTH_SHORT).show();

                    if(contextualActionBar && listView.getCheckedItemCount() == 1) finishContextualActionBarMode();
                }
                else {
                    if (!file.exists()) {
                        if(!description.equalsIgnoreCase(selected.getTrackDescription())) {
                            dataManager.renameTrack(selected, rename);
                            dataManager.setTrackDescription(selected, description);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), "Recording successfully edited", Toast.LENGTH_SHORT).show();
                        } else {
                            dataManager.renameTrack(selected, rename);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), "Name successfully edited", Toast.LENGTH_SHORT).show();
                        }
                        if(contextualActionBar && listView.getCheckedItemCount() == 1) finishContextualActionBarMode();
                    } else {
                        //Toast.makeText(getActivity(), "File with the same name exists", Toast.LENGTH_SHORT).show();
                        ((ViewGroup)inputDialogView.getParent()).removeView(inputDialogView);
                        builder.show().setCanceledOnTouchOutside(false);
                        textInput.setError("File with the same name already exists");
                    }
                }
            }
        });
        builder.show().setCanceledOnTouchOutside(false);
    }

    private void showDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.details, null);
        TextView name =(TextView) view.findViewById(R.id.name);
        name.setText(selected.getTrackName());
        TextView date = (TextView) view.findViewById(R.id.date);
        date.setText(selected.getDateRecorded());
        TextView path = (TextView) view.findViewById(R.id.path);
        path.setText(selected.getTrackPath().toString().substring(0, selected.getTrackPath().toString().lastIndexOf("/")));
        TextView description = (TextView) view.findViewById(R.id.description);
        description.setText(selected.getTrackDescription());
        TextView duration =(TextView) view.findViewById(R.id.duration);
        duration.setText(selected.getTrackDuration());
        TextView size = (TextView) view.findViewById(R.id.size);
        size.setText(selected.getTrackSize());
        TextView format = (TextView) view.findViewById(R.id.format);
        format.setText(selected.getTrackPath().toString().substring(selected.getTrackPath().toString().
                        lastIndexOf("."), selected.getTrackPath().toString().length()));
        TextView recordedWith = (TextView) view.findViewById(R.id.recordedWith);
        recordedWith.setText(selected.getDevice());
        TextView quality = (TextView) view.findViewById(R.id.quality);
        quality.setText("High/44100 Hz");
        builder.setTitle("Details");
        builder.setView(view);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void deleteDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Delete recording " + "'" + selected.getTrackName() + "' ?");
        dialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dataManager.removeTrack(selected);
                dataManager.removeDeletedTracks1();
                if(contextualActionBar)      finishContextualActionBarMode();
                toggleEmptyNotice();
                checkIfLastPlayedTrackDeleted();
                adapter.notifyDataSetChanged();
                Toast.makeText(getActivity(), "Recording deleted", Toast.LENGTH_SHORT).show();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialogDelete = dialogBuilder.create();
        dialogDelete.show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dropbox.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                dropbox.getSession().finishAuthentication();
                String accessToken = dropbox.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
               // Log.i(&quot;DbAuthLog&quot;, &quot;Error authenticating&quot;, e);
            }
            if(dropboxAuthenticationTry)
            uploadToDropbox();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        dataManager.removeDeletedTracks1();
    }

    public void populateListView() {
        dataManager = new DataManager();
        adapter = new MyListAdapter();
        listView.setAdapter(adapter);
        listView.clearChoices();
        for(TrackItem trackItem: dataManager.getTracksList()) { trackItem.trackSelected(false); }
        toggleEmptyNotice();
        checkIfLastPlayedTrackDeleted();
        //registerForContextMenu(listView);
        //TrackItemAdapter adapter = new TrackItemAdapter(getActivity().
        // getApplicationContext(),dataManager.getTracksList());
    }

    @Override
    public void onRefresh() {

    }

    public class MyListAdapter extends ArrayAdapter<TrackItem> {

        MyListAdapter() {
            //super(getActivity(), R.layout.fragment_player, dataManager.getTracksList());
            super(getActivity(), R.layout.tracks_item_view, dataManager.getTracksList());
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        private class ViewHolder {
            TextView sizeText;
            TextView lengthText;
            TextView nameText;
            TextView descriptionText;
            TextView wholeItem;
            TextView optionItem;
            ImageView option;
            CheckBox checkbox;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

            final ViewHolder viewHolder;
            final TrackItem currentTrack = dataManager.getTracksList().get(position);
            View itemView;
            if(convertView == null) {
                viewHolder = new ViewHolder();
                //LayoutInflater inflater = LayoutInflater.from(getContext());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                //convertView = inflater.inflate(R.layout.tracks_item_view, parent, false);
                itemView = inflater.inflate(R.layout.tracks_item_view, parent, false);
                viewHolder.nameText = (TextView) itemView.findViewById(R.id.trackTextView);
                viewHolder.descriptionText = (TextView) itemView.findViewById(R.id.track_description);
                viewHolder.lengthText = (TextView) itemView.findViewById(R.id.length);
                viewHolder.sizeText = (TextView) itemView.findViewById(R.id.size);
                viewHolder.wholeItem = (TextView) itemView.findViewById(R.id.wholeItem);
                viewHolder.wholeItem.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        lastSelectedPosition = (Integer) viewHolder.wholeItem.getTag();
                        TrackItem element = dataManager.getTracksList().get(lastSelectedPosition);
                        if(listView.getCheckedItemCount() == 0) {
                            for(TrackItem trackItem: dataManager.getTracksList()) {
                                trackItem.trackSelected(false);
                            }
                        }
                        if(!listView.isItemChecked(lastSelectedPosition)) {
                            element.trackSelected(true);
                        }
                        else {
                            element.trackSelected(false);
                        }
                        listView.setItemChecked(lastSelectedPosition, element.isSelected());
                    }
                });

                viewHolder.wholeItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (contextualActionBar) {
                            lastSelectedPosition = (Integer) viewHolder.wholeItem.getTag();
                            TrackItem element = dataManager.getTracksList().get(lastSelectedPosition);
                            if(!listView.isItemChecked(lastSelectedPosition)) {
                                element.trackSelected(true);
                            } else {
                                element.trackSelected(false);
                            }
                            listView.setItemChecked(lastSelectedPosition, element.isSelected());
                        } else {
                            lastPlayedTrackPosition = (Integer) viewHolder.wholeItem.getTag();
                            stopPlayer();
                            playingTrack = dataManager.getTracksList().get(lastPlayedTrackPosition);
                            startPlayer(playingTrack);

                            for(TrackItem trackItem: dataManager.getTracksList()) { trackItem.trackSelected(false); }
                            TrackItem element = dataManager.getTracksList().get(lastPlayedTrackPosition);
                            element.trackSelected(true);
                            adapter.notifyDataSetChanged();
//                            for (int i = 0; i < listView.getCount(); i++) {
//                                getViewByPosition(i, listView).setBackgroundColor(ContextCompat.getColor
//                                        (getActivity(), R.color.colorTransparent));
//                            }
//                            getViewByPosition(position, listView).setBackgroundColor
//                                    (ContextCompat.getColor(getActivity(), R.color.colorCustom));
                        }
                    }
                });

                viewHolder.optionItem = (TextView) itemView.findViewById(R.id.optionItem);
                viewHolder.optionItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Creating the instance of PopupMenu
                        final PopupMenu popup = new PopupMenu(getActivity(), viewHolder.option);
                        //Inflating the Popup using xml file
                        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                        //registering popup with OnMenuItemClickListener
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                //selected = currentTrack;
                                lastSelectedPosition = (Integer) viewHolder.wholeItem.getTag();
                                selected = dataManager.getTracksList().get(lastSelectedPosition);
                                if(item.getTitle().toString().equalsIgnoreCase("Play")) {
                                    lastPlayedTrackPosition = lastSelectedPosition;
                                    playingTrack = selected;
                                    stopPlayer();
                                    startPlayer(playingTrack);
                                }
                                if(item.getTitle().toString().equalsIgnoreCase("Edit")) {
                                    editDialog();
                                }
                                if(item.getTitle().toString().equalsIgnoreCase("Share")) {
                                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                    sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selected.getTrackPath()));
                                    sharingIntent.setType("audio/*");
                                    //mShareActionProvider.setShareIntent(sharingIntent);
                                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                                }
                                if(item.getTitle().toString().equalsIgnoreCase("Sync to Cloud")) {
                                    syncDialog();
                                }
                                if(item.getTitle().toString().equalsIgnoreCase("Details")) {
                                    showDetails();
                                }
                                if(item.getTitle().toString().equalsIgnoreCase("Delete")) {
                                    deleteDialog();
                                }
                                return true;
                            }
                        });
                        popup.show();
                    }
                });

                viewHolder.option = (ImageView) itemView.findViewById(R.id.options);
                viewHolder.checkbox = (CheckBox) itemView.findViewById(R.id.check);
                itemView.setTag(viewHolder);
                viewHolder.wholeItem.setTag(position);
                //viewHolder.checkbox.setTag(currentTrack);
            }
            else {
                itemView = convertView;
                //((ViewHolder) itemView.getTag()).checkbox.setTag(currentTrack);
                ((ViewHolder) itemView.getTag()).wholeItem.setTag(position);
                //viewHolder = (ViewHolder) convertView.getTag();
            }

            final ViewHolder holder = (ViewHolder) itemView.getTag();
            if(!contextualActionBar) holder.checkbox.setVisibility(View.INVISIBLE);
            else holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(currentTrack.isSelected());
            holder.nameText.setText(currentTrack.getTrackName());
            holder.descriptionText.setText(currentTrack.getTrackDescription());
            holder.lengthText.setText(currentTrack.getTrackDuration());
            holder.sizeText.setText(currentTrack.getTrackSize());
            holder.wholeItem.setSelected(currentTrack.isSelected());
            holder.optionItem.setSelected(currentTrack.isSelected());
            toggleEmptyNotice();
            checkIfLastPlayedTrackDeleted();

//            View itemView = convertView;
//            if (itemView == null){
//                itemView = getActivity().getLayoutInflater().inflate(R.layout.tracks_item_view, parent, false);
//            }
//            //Find the track to work with
//            final TrackItem currentTrack = dataManager.getTracksList().get(position);

            return itemView;
        }
    }

//    public View getViewByPosition(int pos, ListView listView) {
//        final int firstListItemPosition = listView.getFirstVisiblePosition();
//        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;
//        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
//            return listView.getAdapter().getView(pos, null, listView);
//        } else {
//            final int childIndex = pos - firstListItemPosition;
//            return listView.getChildAt(childIndex);
//        }
//    }

    private void startPlayer(TrackItem currentTrack){
        Uri uri = Uri.fromFile(currentTrack.getTrackPath()); //Load track location
        final double duration;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getActivity().getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            //mediaPlayer.reset();
            mediaPlayer.setDataSource(getActivity().getApplicationContext(), uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.seekTo(progressValue);
            duration = mediaPlayer.getDuration();
            track_duration.setText(String.format(Locale.ENGLISH, "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) duration),
                    TimeUnit.MILLISECONDS.toSeconds((long) duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.
                                    MILLISECONDS.toMinutes((long) duration))));
            trackSeekBar.setMax((int) duration);
            //trackSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            myHandler.postDelayed(UpdateSongTime, 10);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    trackSeekBar.setProgress(trackSeekBar.getMax());
                    current_duration.setText(String.format(Locale.ENGLISH, "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes((long) duration),
                            TimeUnit.MILLISECONDS.toSeconds((long) duration) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.
                                            MILLISECONDS.toMinutes((long) duration))));
                    progressValue = 0;
                    manager.cancel(1);
                    for(TrackItem trackItem: dataManager.getTracksList()) { trackItem.trackSelected(false); }
                    adapter.notifyDataSetChanged();
                    synchronized (this) {
                        mPauseButton.setVisibility(View.INVISIBLE);
                        mPlayButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupNotification();
        track_text.setText(currentTrack.getTrackName());
        currentTrack.trackSelected(true);
        adapter.notifyDataSetChanged();
    }

    private void stopPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressValue = 0;
        if(manager!=null)
        manager.cancel(1);
        for(TrackItem trackItem: dataManager.getTracksList()) {
            trackItem.trackSelected(false);
        }
        adapter.notifyDataSetChanged();
    }

    private void setupNotification() {
        remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.notification_player);
        remoteViews.setImageViewResource(R.id.notification_image, R.mipmap.ic_launcher);
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.ic_media_pause);
        remoteViews.setImageViewResource(R.id.notif_stop, R.drawable.cast_ic_expanded_controller_stop);
        remoteViews.setTextViewText(R.id.notification_total_duration, "/" + track_duration.getText());
        remoteViews.setTextViewText(R.id.notification_status, playingTrack.getTrackName());
        remoteViews.setTextViewText(R.id.notification_pause, "PAUSE");
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                mediaPlayer.getCurrentPosition(), null, true);

        manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        builder = new NotificationCompat.Builder(getActivity());
        builder.setSmallIcon(R.drawable.ic_audiotrack)
                .setContent(remoteViews)
                .setOngoing(true);

        Intent notificationIntent = new Intent(getActivity().getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getActivity(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Intent notificationIntentPause = new Intent(getActivity(), MainActivity.class);
        notificationIntentPause.putExtra("pauseClicked", "clicked");
        PendingIntent contentIntentPause = PendingIntent.getActivity(getActivity(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_pause_resume, contentIntentPause);

        Intent notificationIntentStop = new Intent(getActivity().getApplicationContext(), MainActivity.class);
        notificationIntentStop.putExtra("stopClicked", "clicked");
        PendingIntent contentIntentStop = PendingIntent.getActivity(getActivity(), 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notif_stop, contentIntentStop);

        manager.notify(1, builder.build());
    }

    private void switchToNotificationPlayMode() {
        //remoteViews.setTextViewText(R.id.notification_status, "Playing...");
        remoteViews.setTextViewText(R.id.notification_pause, "PAUSE");
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.ic_media_pause);
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                mediaPlayer.getCurrentPosition(), null, true);

        manager.notify(1, builder.build());
    }

    private void switchToNotificationPauseMode() {
        //remoteViews.setTextViewText(R.id.notification_status, "Paused");
        remoteViews.setTextViewText(R.id.notification_pause, "PLAY");
        remoteViews.setImageViewResource(R.id.notif_pause_resume, R.drawable.ic_media_play);
        remoteViews.setChronometer(R.id.notification_chronometer, SystemClock.elapsedRealtime() +
                mediaPlayer.getCurrentPosition(), null, false);

        manager.notify(1, builder.build());
    }

    private void checkIfLastPlayedTrackDeleted() {
        if(selected != null && !selected.fileExists() && selected.getTrackName().
                equalsIgnoreCase(track_text.getText().toString())) {
            stopPlayer();
            trackSeekBar.setProgress(0);
            track_text.setText("Recording");
            track_duration.setText("00:00");
            current_duration.setText("00:00");
        }
    }

    private void toggleEmptyNotice() {
        if(dataManager.getTracksList().isEmpty())   empty_notice.setVisibility(View.VISIBLE);
        else                                        empty_notice.setVisibility(View.INVISIBLE);
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            double startTime;
            if(mediaPlayer!=null) {
                startTime = mediaPlayer.getCurrentPosition();
                current_duration.setText(String.format(Locale.ENGLISH, "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                        toMinutes((long) startTime)))
                );
                trackSeekBar.setProgress((int) startTime);
                myHandler.postDelayed(this, 5);
            }
        }
    };

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

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//    }

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