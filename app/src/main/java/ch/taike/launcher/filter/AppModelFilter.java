package ch.taike.launcher.filter;

import java.util.ArrayList;
import java.util.List;

import ch.taike.launcher.entity.AppModel;

public interface AppModelFilter {
    ArrayList<AppModel> filer(List<AppModel> apps);
}
