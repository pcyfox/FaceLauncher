package ch.taike.launcher.entity;

import androidx.annotation.Keep;

@Keep
public class LauncherMessage {
    private Action action;
    private String data;

    public LauncherMessage(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
