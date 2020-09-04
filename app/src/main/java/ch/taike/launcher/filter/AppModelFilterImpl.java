package ch.taike.launcher.filter;

import java.util.ArrayList;
import java.util.List;

import ch.taike.launcher.entity.AppModel;
import ch.taike.launcher.constant.ConstantData;

public class AppModelFilterImpl implements AppModelFilter {
    @Override
    public ArrayList<AppModel> filer(List<AppModel> apps) {
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
