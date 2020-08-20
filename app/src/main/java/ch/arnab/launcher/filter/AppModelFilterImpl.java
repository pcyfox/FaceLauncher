package ch.arnab.launcher.filter;

import java.util.ArrayList;

import ch.arnab.launcher.AppModel;

public class AppModelFilterImpl implements AppModelFilter {
    private String[] flags = {"com.taike", "com.tk"};
    @Override
    public ArrayList<AppModel> filer(ArrayList<AppModel> apps) {
        ArrayList<AppModel> newAppModels = new ArrayList<>();
        for (AppModel appModel : apps) {
            String packageName = appModel.getAppInfo().packageName;
            for (String flag : flags) {
                if (packageName.contains(flag) && !packageName.contains("launcher")) {
                    newAppModels.add(appModel);
                    break;
                }
            }
        }
        return newAppModels;
    }
}
