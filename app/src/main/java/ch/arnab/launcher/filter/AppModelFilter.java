package ch.arnab.launcher.filter;

import java.util.ArrayList;

import ch.arnab.launcher.AppModel;

public interface AppModelFilter {
    ArrayList<AppModel> filer(ArrayList<AppModel> apps);
}
