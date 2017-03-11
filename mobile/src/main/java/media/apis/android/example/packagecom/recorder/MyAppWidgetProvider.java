package media.apis.android.example.packagecom.recorder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Created by Ali on 23/02/2017.
 */

public class MyAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for(int i=0; i<appWidgetIds.length; i++) {
            int currentWidgetId = appWidgetIds[i];

//            String url = "http://www.tutorialspoint.com";
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setData(Uri.parse(url));
//
//            PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
//
//            views.setOnClickPendingIntent(R.id.button, pending);
//            appWidgetManager.updateAppWidget(currentWidgetId, views);
//            Toast.makeText(context, "widget added", Toast.LENGTH_SHORT).show();

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("record", "Start Recording");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.imageView, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(currentWidgetId, views);
        }
    }
}
