package com.feheren_fekete.applist;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class ApplistService extends RemoteViewsService {

    private static final String TAG = ApplistService.class.getSimpleName();

    private HashMap<Integer, ApplistRemoteViewsFactory> mWidgetViewFactories = new HashMap<>();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId =
                intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return null;
        }

        ApplistRemoteViewsFactory widgetViewFactory =
                new ApplistRemoteViewsFactory(this.getApplicationContext(), intent);
        mWidgetViewFactories.put(appWidgetId, widgetViewFactory);
        Log.d(TAG, "Created factory for Appwidget ID: " + appWidgetId);
        return widgetViewFactory;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Common.ACTION_APP_SELECTED)) {
            onAppSelected(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void onAppSelected(Intent intent) {
        int appWidgetId =
                intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
        String packageName = intent.getStringExtra(Common.EXTRA_PACKAGE_NAME);
        String activityName = intent.getStringExtra(Common.EXTRA_ACTIVITY_NAME);

        Log.d(TAG, "Appwidget ID:" + appWidgetId);
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            ApplistRemoteViewsFactory widgetViewFactory = mWidgetViewFactories.get(appWidgetId);
            if (widgetViewFactory != null) {
                widgetViewFactory.onAppSelected(packageName, activityName);
            }
        }
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class ApplistRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ApplistRemoteViewsFactory.class.getSimpleName();

    private Context mContext;
    private int mAppWidgetId; // Not used yet.
    private List<ResolveInfo> mApplications;

    public ApplistRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Since we reload the data in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        mApplications.clear();
    }

    public int getCount() {
        return mApplications.size();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        ResolveInfo intentResolveInfo = mApplications.get(position);

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.applist_item);
        rv.setTextViewText(
                R.id.button_applistItem,
                intentResolveInfo.loadLabel(mContext.getPackageManager()));

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view.
        Bundle extras = new Bundle();
        extras.putString(Common.EXTRA_PACKAGE_NAME, intentResolveInfo.activityInfo.packageName);
        extras.putString(Common.EXTRA_ACTIVITY_NAME, intentResolveInfo.activityInfo.name);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        rv.setOnClickFillInIntent(R.id.button_applistItem, fillInIntent);

        return rv;
    }

    public void onAppSelected(String packageName, String activityName) {
        Log.d(TAG, "App selected: " + packageName + ": " + activityName);
    }

    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        return 2;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return false;
    }

    public void onDataSetChanged() {
        mApplications = getApplicationList();
    }

//    private List<ApplistItem> getApplicationList() {
//        final PackageManager pm = mContext.getPackageManager();
//        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
//
//        List<ApplistItem> applist = new ArrayList<>();
//        for (ApplicationInfo app : apps) {
//            if(pm.getLaunchIntentForPackage(app.packageName) != null) {
//                applist.add(new ApplistItem(app, app.loadLabel(pm).toString()));
//            }
//        }
//        Collections.sort(applist, ALPHABETICAL_ORDER);
//        return applist;
//    }

    private List<ResolveInfo> getApplicationList() {
        final PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> applist = pm.queryIntentActivities(intent, 0);

        Collections.sort(applist, new Comparator<ResolveInfo>() {
            public int compare(ResolveInfo e1, ResolveInfo e2) {
                final String label1 = e1.loadLabel(pm).toString();
                final String label2 = e2.loadLabel(pm).toString();
                return label1.compareTo(label2);
            }
        });
        return applist;
    }

}
