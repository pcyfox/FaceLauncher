package ch.arnab.launcher.filter;

import java.util.ArrayList;

import ch.arnab.launcher.AppModel;
import ch.arnab.launcher.constant.ConstantData;

public class AppModelFilterImpl implements AppModelFilter {
    @Override
    public ArrayList<AppModel> filer(ArrayList<AppModel> apps) {
        ArrayList<AppModel> newAppModels = new ArrayList<>();
        for (AppModel appModel : apps) {
            String packageName = appModel.getAppInfo().packageName;
            for (String flag : ConstantData.TK_APP_FLAGS) {
                if (packageName.contains(flag) && !packageName.contains("launcher")) {
                    newAppModels.add(appModel);
                    break;
                }
            }
        }
        return newAppModels;
    }
}
