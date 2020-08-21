package ch.arnab.launcher.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.taike.lib_network.udp.UDPSocketClient;
import com.tk.launcher.R;

import java.util.List;

import ch.arnab.launcher.App;
import ch.arnab.launcher.AppModel;
import ch.arnab.launcher.SocketMsgHandler;
import ch.arnab.launcher.entity.Action;
import ch.arnab.launcher.entity.LauncherMessage;

public class CmdActivity extends FragmentActivity {
    private static final String TAG = "HomeScreenActivity";
    private EditText editTextCmd;
    private LauncherMessage launcherMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_cmd);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        PermissionUtils.permission(PermissionConstants.STORAGE);
        editTextCmd = findViewById(R.id.et_dmd);
        editTextCmd.setText("{\"action\":\"CLOSE_APP\",\"data\":\"com.taike.edu.stu\"}");

        LinearLayout linearLayout = findViewById(R.id.ll_cmd);


        for (final Action action : Action.values()) {
            RadioButton button = new RadioButton(this);
            button.setTextColor(ContextCompat.getColor(this, R.color.common_colorAccent));
            button.setText(action.name());
            linearLayout.addView(button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectAction(action);
                }
            });
        }


    }


    private void selectAction(Action action) {
        launcherMessage = new LauncherMessage(action, ((EditText) findViewById(R.id.et_data)).getText().toString());
        editTextCmd.setText(GsonUtils.toJson(launcherMessage));
    }


    @Override
    public void onBackPressed() {

    }


    public void sendCmd(View view) {
        //{"action":"CLOSE_APP","data":"com.taike.edu.stu"}
        //UDPSocketClient.getInstance().sendBroadcast(GsonUtils.toJson(new LauncherMessage(Action.LAUNCH_APP_SINGLE_TOP, "com.taike.edu.stu")));
        UDPSocketClient.getInstance().sendBroadcast(editTextCmd.getText().toString());
        //RootUtils.cleatApp("com.taike.edu.stu");
        //RootUtils.uninstallAPK("com.taike.edu.stu");
        // RootUtils.installAPK(this, "/sdcard/debug_TK-Stu_V1.0.0_1_2020-08-20_19-47-52.apk");
    }
}