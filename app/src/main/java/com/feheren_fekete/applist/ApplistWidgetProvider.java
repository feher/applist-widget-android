package com.feheren_fekete.applist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

public class ApplistWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews remoteViews = buildLayout(context, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // Called when the BroadcastReceiver receives an Intent broadcast.
    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(Common.ACTION_CLICK)) {
            String packageName = intent.getStringExtra(Common.EXTRA_PACKAGE_NAME);
            String activityName = intent.getStringExtra(Common.EXTRA_ACTIVITY_NAME);
            int appWidgetId =
                    intent.getIntExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);
            if (packageName != null
                    && activityName != null
                    && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                launchApp(context, packageName, activityName);
                notifyService(context, packageName, activityName, appWidgetId);
            } else {
                Toast.makeText(context, "Missing package name!", Toast.LENGTH_SHORT).show();
            }
        }
        super.onReceive(context, intent);
    }

    private void launchApp(Context context, String packageName, String activityName) {
        ComponentName componentName =
                new ComponentName(packageName, activityName);
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        i.setComponent(componentName);
        context.startActivity(i);
    }

    private void notifyService(Context context,
                               String packageName,
                               String activityName,
                               int appWidgetId) {
        Intent serviceIntent = new Intent(context, ApplistService.class);
        serviceIntent.setAction(Common.ACTION_APP_SELECTED);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.putExtra(Common.EXTRA_PACKAGE_NAME, packageName);
        serviceIntent.putExtra(Common.EXTRA_ACTIVITY_NAME, activityName);
        context.startService(serviceIntent);
    }

    private RemoteViews buildLayout(Context context, int appWidgetId) {
        // Specify the service to provide data for the widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, ApplistService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.applist_widget);
        rv.setRemoteAdapter(R.id.listView_apps, intent);

        // Set the empty view to be displayed if the collection is empty.  It must be a sibling
        // view of the collection view.
        rv.setEmptyView(R.id.listView_apps, R.id.textView_emptyAppList);

        // Bind a click listener template for the contents of the applist.
        final Intent onClickIntent = new Intent(context, ApplistWidgetProvider.class);
        onClickIntent.setAction(Common.ACTION_CLICK);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // We need to update the intent's data if we set an extra, since the extras will
        // be ignored otherwise.
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent onClickPendingIntent =
                PendingIntent.getBroadcast(
                        context, 0,
                        onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.listView_apps, onClickPendingIntent);

        return rv;
    }
}
