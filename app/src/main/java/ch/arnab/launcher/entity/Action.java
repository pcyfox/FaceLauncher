package ch.arnab.launcher.entity;

public enum Action {
    LAUNCH_APP,//启动指定APP
    LAUNCH_APP_SINGLE_TOP,//启动指定APP到栈顶,并干掉泰克的其它应用
    CLOSE_APP,//关闭指定APP
    NOTIFY_APP_UPDATE,//通知指定APP进行更新检测
    CLEAR_APP,//清理数据和缓存
    INSTALL_APP,//安装APP、
    UNINSTALL_APP,//卸载APP
    EXEC_CMD,//执行任意命令
    DOWNLOAD_FILE
}
