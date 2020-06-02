package com.taike.lib_common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.view.InputDevice;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.GsonUtils;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.INPUT_SERVICE;
import static android.content.Context.USB_SERVICE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UsbChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        sendEvent(new UsbChangeEvent(detectUsbDeviceWithInputManager(context)));
    }

    private void sendEvent(final UsbChangeEvent event) {
      //  LiveEventBus.get().with(UsbChangeReceiver.UsbChangeEvent.KEY, UsbChangeReceiver.UsbChangeEvent.class).postDelay(event, 500);

    }

    public static class Helper {
        private final static String ACTION = "android.hardware.usb.action.USB_STATE";
        private static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
        private static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

        static Helper instance;
        private UsbChangeReceiver receiver;

        public static Helper getInstance() {
            if (instance == null) {
                instance = new Helper();
            }
            return instance;
        }

        public void register(Context context) {
            if (context == null) return;
            context = context.getApplicationContext();
            if (receiver == null) {
                receiver = new UsbChangeReceiver();
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION);
            filter.addAction(ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(ACTION_USB_DEVICE_DETACHED);
            context.registerReceiver(receiver, filter);
        }

        public void unregister(Context context) {
            if (receiver == null || context == null) return;
            context.unregisterReceiver(receiver);

        }
    }

    //获取输入设备和usb设备列表0
    private List<String> detectUsbDeviceWithInputManager(Context context) {
        //获取输入设备
        InputManager im = (InputManager) context.getSystemService(INPUT_SERVICE);
        int[] devices = im.getInputDeviceIds();
        List<String> list = new ArrayList<>();
        for (int id : devices) {
            InputDevice device = im.getInputDevice(id);
            if (device.toString().contains("Location: external")) {
                if (!list.contains(device.getName())) {
                    list.add(device.getName());
                }
            }
        }
        //获取usb设备
        HashMap<String, UsbDevice> deviceHashMap = ((UsbManager) context.getSystemService(USB_SERVICE)).getDeviceList();
        for (Map.Entry entry : deviceHashMap.entrySet()) {
            UsbDevice device = (UsbDevice) entry.getValue();
            if (device != null) {
                if (device.getProductName() != null) {
                    if (!list.contains(device.getProductName())) {
                        list.add(device.getProductName());
                    }
                }
            }
        }
        XLog.d("detectUsbDeviceWithInputManager   " + GsonUtils.toJson(list));
        return list;
    }

    public class UsbChangeEvent {
        public static final String KEY = "UsbChangeEvent";
        List<String> list;

        public UsbChangeEvent(List<String> list) {
            this.list = list;
        }

        public List<String> getList() {
            return list;
        }
    }


}
