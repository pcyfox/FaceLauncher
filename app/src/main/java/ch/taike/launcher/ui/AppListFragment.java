package ch.taike.launcher.ui;

import android.os.Bundle;

import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.util.ArrayList;

import ch.taike.launcher.AppListAdapter;
import ch.taike.launcher.AppModel;
import ch.taike.launcher.AppsLoader;
import ch.taike.launcher.filter.AppModelFilter;
import ch.taike.launcher.filter.AppModelFilterImpl;

/**
 * Created by Arnab Chakraborty
 */
public class AppListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>> {
    AppListAdapter mAdapter;
    private AppModelFilter appModelFilter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText("No Applications");
        appModelFilter = new AppModelFilterImpl();
        mAdapter = new AppListAdapter(getActivity());
        setListAdapter(mAdapter);
        // till the data is loaded display a spinner
        setListShown(false);

        // create the loader to load the apps list in background
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle bundle) {
        return new AppsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> apps) {
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppModel>> loader) {
        mAdapter.setData(null);
    }
}
