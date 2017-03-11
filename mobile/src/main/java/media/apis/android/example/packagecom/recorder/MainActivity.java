package media.apis.android.example.packagecom.recorder;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements DataApi.DataListener,
        Recorder.OnFragmentInteractionListener, Player.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    private static final String TAG = "Recorder";
    //private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int REQUEST_ACCCOUNT_PICK = 4;
    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult connectionResult;
    private ProgressDialog loading;

    private static final int MY_PERMISSIONS_REQUEST_MULTIPLE = 1;
    private static FragmentRefreshListener fragmentRefreshListener;
    final int[] ICONS = new int[]{
            R.drawable.microphone,
            R.drawable.ic_audiotrack,
    };

    public static FragmentRefreshListener getFragmentRefreshListener() {
        return fragmentRefreshListener;
    }

    public void setFragmentRefreshListener(FragmentRefreshListener fragmentRefreshListener) {
        MainActivity.fragmentRefreshListener = fragmentRefreshListener;
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
//        if(getIntent().getStringExtra("pauseClicked").equalsIgnoreCase("clicked"))
//        {
//            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
//        }
//        else if(getIntent().getStringExtra("stopClicked").equalsIgnoreCase("clicked"))
//        {
//            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
//        }
//        Bundle extras = getIntent().getExtras();
//        Toast.makeText(getApplicationContext(), extras.getString("record"), Toast.LENGTH_LONG).show();
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.INTERNET,
                            android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                            android.Manifest.permission.WAKE_LOCK},
                    MY_PERMISSIONS_REQUEST_MULTIPLE);
        }
        startService(new Intent(getBaseContext(), MyService.class));

//        String newString;
//        if (savedInstanceState == null) {
//            Bundle extras = getIntent().getExtras();
//            if(extras == null) {
//                newString = null;
//            } else {
//                newString = extras.getString("pauseClicked");
//            }
//        } else {
//            newString = (String) savedInstanceState.getSerializable("pauseClicked");
//        }
//        Toast.makeText(getApplicationContext(), newString, Toast.LENGTH_LONG).show();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the two
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        /*
        The {@link ViewPager} that will host the section contents.
        */
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(Player.mActionMode!=null) {
                    Player.mActionMode.finish();
                    Player.mActionMode = null;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.getTabAt(0).setIcon(ICONS[0]);
        tabLayout.getTabAt(1).setIcon(ICONS[1]);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

    }

    public void authenticateGoogle() {
//        try {
//            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
//        } catch (IntentSender.SendIntentException e) {
//            Log.e(TAG, "Exception while starting resolution activity", e);
//        }
        startActivityForResult(AccountPicker.newChooseAccountIntent(null,
                null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null),
                REQUEST_ACCCOUNT_PICK);
    }

    public void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull final DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything and must fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        loading = ProgressDialog.show(MainActivity.this, "Please wait",null,true,true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // Get an output stream for the contents.
                                OutputStream outputStream = result.getDriveContents().getOutputStream();
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                try {
                                    InputStream inputStream = new FileInputStream(Player.selected.getTrackPath());
                                    while ((len = inputStream.read(buf)) > 0) {
                                        outputStream.write(buf, 0, len);
                                    }
                                    outputStream.flush();
                                    inputStream.close();
                                    outputStream.close();
                                    loading.dismiss();
                                } catch (IOException e1) {
                                    Log.i(TAG, "Unable to write file contents.");
                                }
                                // Create the initial metadata - MIME type and title.
                                // Note that the user will be able to change the title later.
                                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                        .setMimeType("audio/wav").setTitle(Player.selected.getTrackName() + ".wav").build();
                                // Create an intent for the file chooser, and start it.
                                IntentSender intentSender = Drive.DriveApi
                                        .newCreateFileActivityBuilder()
                                        .setInitialMetadata(metadataChangeSet)
                                        .setInitialDriveContents(result.getDriveContents())
                                        .build(mGoogleApiClient);
                                try {
                                    startIntentSenderForResult(intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.i(TAG, "Failed to launch file chooser.");
                                }
                            }
                        }).start();
                    }
                });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "File successfully saved.");
                    Toast.makeText(getApplicationContext(), "File saved to Drive",
                            Toast.LENGTH_LONG).show();
                }
                if (resultCode == RESULT_CANCELED) {
                    if (mGoogleApiClient != null) { mGoogleApiClient.disconnect(); }
                    mGoogleApiClient = null;
                }
                break;
            case REQUEST_ACCCOUNT_PICK:
                if (resultCode == RESULT_OK) {
                    //if(haveNetworkConnection()) {
                        connectToGoogleApiClient(data);
                        saveFileToDrive();
                    //}
//                    else {
//                        Toast.makeText(getApplicationContext(), "No network connection",
//                                Toast.LENGTH_LONG).show();
//                    }
                }
                if (resultCode == RESULT_CANCELED) {
                    if (mGoogleApiClient != null) { mGoogleApiClient.disconnect(); }
                    mGoogleApiClient = null;
                }
                break;
        }
    }

    private boolean haveNetworkConnection() {
        boolean isConnected;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        isConnected = netInfo != null;
        return isConnected;
    }

    private void connectToGoogleApiClient(Intent data) {
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection failures.
            // Since no account name is passed, the user is prompted to choose.
            String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .setAccountName(email)
                    .build();
        }
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "API client connected.");
        //Wearable.DataApi.addListener(mGoogleApiClient, this);
        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());
        this.connectionResult = connectionResult;
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it. Called typically when the app is not yet authorized,
        // and an authorization dialog is displayed to the user.
        //try {
        //    connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        //} catch (IntentSender.SendIntentException e) {
        //    Log.e(TAG, "Exception while starting resolution activity", e);
        //}
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
//        Toast.makeText(getApplicationContext(), "Receiving file", Toast.LENGTH_LONG).show();
//        for (DataEvent event : dataEvents) {
//            if (event.getType() == DataEvent.TYPE_CHANGED &&
//                    event.getDataItem().getUri().getPath().equals(Environment.getExternalStorageDirectory().
//                            getAbsolutePath() + "/recording.wav")) {
//                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
//                Asset myAsset = dataMapItem.getDataMap().getAsset("asset");
//                Wearable.DataApi.getFdForAsset(mGoogleApiClient, myAsset).setResultCallback(
//                        new ResultCallback<DataApi.GetFdForAssetResult>() {
//                            @Override
//                            public void onResult(DataApi.GetFdForAssetResult getFdForAssetResult) {
//                                InputStream assetInputStream = getFdForAssetResult.getInputStream();
//                                SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
//                                final String current = sdf.format(new Date());
//                                Date date = new Date();
//                                String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
//                                String day          = (String) DateFormat.format("dd", date); // 20
//                                String monthString  = (String) DateFormat.format("MMM",  date); // Jun
//                                String monthNumber  = (String) DateFormat.format("MM",   date); // 06
//                                String year         = (String) DateFormat.format("yyyy", date); // 2013
//                                File file = new File(Environment.getExternalStorageDirectory().
//                                        getAbsolutePath()+ "/Recorder/" + "Wear " + day + " " +
//                                            monthString  + " " + year + ", " + current + ".wav");
//                                try {
//                                    FileOutputStream fOut = new FileOutputStream(file);
//                                    int nRead;
//                                    byte[] data = new byte[16384];
//                                    while ((nRead = assetInputStream.read(data, 0, data.length)) != -1) {
//                                        fOut.write(data, 0, nRead);
//                                    }
//                                    fOut.flush();
//                                    fOut.close();
//                                    syncFiles();
//                                    //Player.newInstance().populateListView();
//                                    onFragmentInteraction();
//                                } catch (IOException e) {
//                                    //System.out.println("ERROR File write failed: " + e.toString());
//                                }
//                            }
//                        }
//                );
//            }
//        }
    }

//    private void syncFiles() {
//        DataManager dataManager = new DataManager();
//        int fileFound=0;
//        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder");
//        File[] files = f.listFiles();
//        for (File inFile : files) {
//            if (inFile.isFile() && inFile.getName().startsWith("Wear")) {
//                String filename = null;
//                int pos = inFile.getName().lastIndexOf(".");
//                if (pos > 0) {
//                    filename = inFile.getName().substring(0, pos);
//                }
//                for (TrackItem track : dataManager.getTracksList()) {
//                    if (filename != null && filename.equalsIgnoreCase(track.getTrackName().trim())) {
//                        fileFound = 1;
//                        break;
//                    }
//                }
//                if(fileFound==0) {
//                    MediaPlayer mp = new MediaPlayer();
//                    FileInputStream fs;
//                    FileDescriptor fd;
//                    try {
//                        fs = new FileInputStream(inFile);
//                        fd = fs.getFD();
//                        mp.setDataSource(fd);
//                        mp.prepare();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    int length = mp.getDuration();
//                    mp.release();
//
//                    String duration = String.format(Locale.ENGLISH, "%02d:%02d",
//                            TimeUnit.MILLISECONDS.toMinutes(length) -
//                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(length)),
//                            TimeUnit.MILLISECONDS.toSeconds(length) -
//                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length)));
//                    SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.ENGLISH);
//                    final String current = sdf.format(new Date());
//                    Date date = new Date();
//                    String time         = (String) DateFormat.format("HH:mm", date); // 17:05
//                    String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
//                    String day          = (String) DateFormat.format("dd", date); // 20
//                    String monthString  = (String) DateFormat.format("MMM",  date); // Jun
//                    String monthNumber  = (String) DateFormat.format("MM",   date); // 06
//                    String year         = (String) DateFormat.format("yyyy", date); // 2013
//
//                    String fileSize;
//                    float filesize = inFile.length();
//                    if(filesize >= 1024 * 1024 * 1024) {
//                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024 * 1024)));
//                        fileSize = String.valueOf(filesize) + " GB";
//                    }
//                    else if (filesize >= 1024 * 1024) {
//                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", filesize/(1024 * 1024)));
//                        fileSize = String.valueOf(filesize) + " MB";
//                    }
//                    else if (filesize >= 1024) {
//                        filesize = Float.parseFloat(String.format(Locale.ENGLISH, "%f", filesize/(1024)));
//                        fileSize = String.valueOf((long) filesize) + " KB";
//                    }
//                    else fileSize = String.valueOf(filesize) + "B";
//
//                    String fileName = "Watch " + day + " " + monthString + " " + year + ", " + current;
//                    String dateRecorded = dayOfTheWeek + ", " + day + " " + monthString + " " + year + ", " + time;
//                    try {
//                        dataManager.addTrack1(inFile, fileName, "No description", fileSize,
//                                duration, dateRecorded, "Watch");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    inFile.delete();
//                }
//                else fileFound=0;
//            }
//        }
//    }

    public interface FragmentRefreshListener{
        void onRefresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MULTIPLE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //startActivity(i);
                    recreate();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("Important");
                    dialogBuilder.setMessage("Permissions are important for application to be operational. " +
                            "Please allow necessary permissions.");
                    dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //startActivity(i);
                            recreate();
                        }
                    });
//                    dialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Intent intent = new Intent(Intent.ACTION_MAIN);
//                            intent.addCategory(Intent.CATEGORY_HOME);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        }
//                    });
                    AlertDialog dialogPermission = dialogBuilder.create();
                    dialogPermission.show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

/*
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
*/

    @Override
    public void onDestroy() {
        deleteTempFile();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        //deleteTempFile();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
//        if (mGoogleApiClient != null) {
//            mGoogleApiClient.disconnect();
//        }
        super.onPause();
    }

    private void deleteTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorder/temp.wav");
        file.delete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), Settings.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction() {
        if(getFragmentRefreshListener()!= null){
            getFragmentRefreshListener().onRefresh();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else if (id == R.id.nav_sync) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(getApplicationContext(), Settings.class));
        }
        return true;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new Recorder();
                case 1:
                    return new Player();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }
}
