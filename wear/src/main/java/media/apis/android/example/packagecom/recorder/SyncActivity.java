package media.apis.android.example.packagecom.recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.WindowManager;

public class SyncActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener {

    private DelayedConfirmationView mDelayedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mDelayedView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);
                mDelayedView.setListener(SyncActivity.this);
                //mDelayedView.setVisibility(View.VISIBLE);
                // Two seconds to cancel the action
                mDelayedView.setTotalTimeMs(MainActivity.fileLength/120);
                // Start the timer
                mDelayedView.start();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    @Override
    public void onTimerFinished(View view) {
        Intent intent = new Intent(SyncActivity.this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Successfully sent!");
        startActivity(intent);
        mDelayedView.reset();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        }, 1000);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onTimerSelected(View view) {
//        Intent intent = new Intent(SyncActivity.this, ConfirmationActivity.class);
//        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Sending unsuccessful");
//        startActivity(intent);
//        mDelayedView.reset();
//        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startActivity(new Intent(getApplicationContext(), MainActivity.class));
//            }
//        }, 1000);
//        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

//    private void deleteTempFile() {
//        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.wav");
//        file.delete();
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Toast.makeText(getApplicationContext(), "D", Toast.LENGTH_LONG).show();
//        deleteTempFile();
//    }
}
