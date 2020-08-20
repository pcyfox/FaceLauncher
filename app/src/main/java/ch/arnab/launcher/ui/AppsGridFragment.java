package ch.arnab.launcher.ui;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.GridView;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.util.ArrayList;

import ch.arnab.launcher.AppListAdapter;
import ch.arnab.launcher.AppModel;
import ch.arnab.launcher.AppsLoader;
import ch.arnab.launcher.filter.AppModelFilter;
import ch.arnab.launcher.filter.AppModelFilterImpl;

/**
 * Created by Arnab Chakraborty
 */
public class AppsGridFragment extends GridFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>> {

    private AppModelFilter appModelFilter;
    AppListAdapter mAdapter;

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
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle bundle) {
        return new AppsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> apps) {
        mAdapter.setData(appModelFilter.filer(apps));
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
                startActivity(intent);
            }
        }
    }
}
