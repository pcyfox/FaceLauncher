package ch.taike.launcher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import androidx.lifecycle.Observer;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.elvishew.xlog.XLog;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;
import java.util.List;

import ch.taike.launcher.AppListAdapter;
import ch.taike.launcher.AppModel;
import ch.taike.launcher.AppsLoader;
import ch.taike.launcher.entity.Action;
import ch.taike.launcher.filter.AppModelFilter;
import ch.taike.launcher.filter.AppModelFilterImpl;

/**
 * Created by Arnab Chakraborty
 */
public class AppsGridFragment extends GridFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>> {
    private static final String TAG = "AppsGridFragment";
    private AppModelFilter appModelFilter;
    AppListAdapter mAdapter;
    private List<AppModel> allApps;
    private List<AppModel> loadedApps;
    private boolean isShowAllApp = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        appModelFilter = new AppModelFilterImpl();
        setEmptyText("No Applications");
        mAdapter = new AppListAdapter(getActivity());
        setGridAdapter(mAdapter);
        // till the data is loaded display a spinner
        setGridShown(false);
        // create the loader to load the apps list in background
        getLoaderManager().initLoader(0, null, this);
        LiveEventBus.get().with(Action.SHOW_ALL_APPS.name(), String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                isShowAllApp = "true".equals(s) || "1".equals(s);
                if (allApps != null && mAdapter != null) {
                    if (isShowAllApp) {
                        mAdapter.setData(allApps);
                    } else {
                        mAdapter.setData(appModelFilter.filer(allApps));
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle bundle) {
        return new AppsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> apps) {
        allApps = apps;
        loadedApps = appModelFilter.filer(apps);
        if (isShowAllApp) {
            mAdapter.setData(allApps);
        } else {
            mAdapter.setData(loadedApps);
        }
        if (isResumed()) {
            setGridShown(true);
        } else {
            setGridShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppModel>> loader) {
        mAdapter.setData(null);
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        AppModel app = (AppModel) getGridAdapter().getItem(position);
        if (app != null) {
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(app.getApplicationPackageName());
            if (intent != null) {
                XLog.i(TAG + ":user startActivity called with: app = [" + app + "]");
                startActivity(intent);
            }
        }
    }

    public List<AppModel> getAllApps() {
        return allApps;
    }


    public List<AppModel> getLoadedApps() {
        return loadedApps;
    }
}
