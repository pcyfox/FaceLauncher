package com.taike.lib_network.udp;

import com.elvishew.xlog.XLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by melo on 2017/9/20.
 */

public class UDPSocketClient {

    private MsgArrivedListener msgArrivedListener;
    private static UDPSocketClient instance;
    private static final String TAG = "UDPSocket";
    // 单个CPU线程池大小
    private static final int POOL_SIZE = 5;
    private static final int BUFFER_LENGTH = 8 * 1024;
    private byte[] receiveByte = new byte[BUFFER_LENGTH];

    private static final String BROADCAST_IP = "255.255.255.255";

    // 端口号，
    public static int CLIENT_PORT = 2068;
    public static int SERVER_PORT = 1099;
    public static String SERVER_IP = "";

    private boolean isThreadRunning = false;
    private DatagramSocket client;
    private DatagramPacket receivePacket;
    private long lastReceiveTime = 0;
    private static final long TIME_OUT = 120 * 1000;
    private static final long HEARTBEAT_MESSAGE_DURATION = 10 * 1000;

    private ExecutorService mThreadPool;
    private Thread clientThread;
    private HeartbeatTimer timer;
    private boolean isStarted = false;

    public boolean isStarted() {
        return isStarted;
    }

    private UDPSocketClient() {
        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * POOL_SIZE);
        // 记录创建对象时的时间
        lastReceiveTime = System.currentTimeMillis();
    }

    private UDPSocketClient(int port) {
        this();
        CLIENT_PORT = port;
    }


    public static UDPSocketClient getInstance() {
        if (instance == null) {
            instance = new UDPSocketClient();
        }
        return instance;
    }

    public void startUDPSocket() {
        if (client != null || isStarted) return;
        try {
            // 表明这个 Socket 在设置的端口上监听数据。
            client = new DatagramSocket(CLIENT_PORT);
            XLog.d(" client = new DatagramSocket(CLIENT_PORT)" + client);
            if (receivePacket == null) {
                // 创建接受数据的 packet
                receivePacket = new DatagramPacket(receiveByte, BUFFER_LENGTH);
            }
            startSocketThread();
            XLog.d("startUDPSocket() called");
            isStarted = true;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public MsgArrivedListener getMsgArrivedListener() {
        return msgArrivedListener;
    }

    public void setMsgArrivedListener(MsgArrivedListener msgArrivedListener) {
        this.msgArrivedListener = msgArrivedListener;
    }

    /**
     * 开启发送数据的线程
     */
    private void startSocketThread() {
        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                XLog.d("clientThread is running...");
                receiveMessage();
            }
        });
        isThreadRunning = true;
        clientThread.start();
    }

    /**
     * 处理接受到的消息
     */
    private void receiveMessage() {
        while (isThreadRunning && msgArrivedListener != null) {
            try {
                if (client != null) {
                    client.receive(receivePacket);
                }
                lastReceiveTime = System.currentTimeMillis();
            } catch (IOException e) {
                XLog.d("UDP数据包接收失败！线程停止");
                stopUDPSocket();
                e.printStackTrace();
                return;
            }
            if (receivePacket == null || receivePacket.getLength() == 0) {
                XLog.d("无法接收UDP数据或者接收到的UDP数据为空");
                continue;
            }
            //String strReceive = new String(receivePacket.getData(), 0, receivePacket.getLength(),new C);
            try {
                String strReceive = new String(receivePacket.getData(), 0, receivePacket.getLength(), "utf-8");
                XLog.d("接收到广播数据 " + receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort() + "  内容  " + strReceive);
                if (msgArrivedListener != null) {
                    msgArrivedListener.onMsgArrived(strReceive);
                }
                // 每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
                if (receivePacket != null) {
                    receivePacket.setLength(BUFFER_LENGTH);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void stopUDPSocket() {
        isThreadRunning = false;
        receivePacket = null;
        if (clientThread != null) {
            clientThread.interrupt();
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (timer != null) {
            timer.exit();
        }
        isStarted = false;
    }

    /**
     * 启动心跳，timer 间隔十秒
     */
    private void startHeartbeatTimer() {
        timer = new HeartbeatTimer();
        timer.setOnScheduleListener(new HeartbeatTimer.OnScheduleListener() {
            @Override
            public void onSchedule() {
                XLog.d("timer is onSchedule...");
                long duration = System.currentTimeMillis() - lastReceiveTime;
                XLog.d("duration:" + duration);
                if (duration > TIME_OUT) {//若超过两分钟都没收到我的心跳包，则认为对方不在线。
                    XLog.d("超时，对方已经下线");
                    // 刷新时间，重新进入下一个心跳周期
                    lastReceiveTime = System.currentTimeMillis();
                } else if (duration > HEARTBEAT_MESSAGE_DURATION) {//若超过十秒他没收到我的心跳包，则重新发一个。
                    String string = "hello,this is a heartbeat message";
                    sendBroadcast(string);
                }
            }

        });
        timer.startTimer(0, 1000 * 10);
    }


    public void sendBroadcast(final String message) {
        if (client == null) {
            return;
        }
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(BROADCAST_IP);
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, targetAddress, CLIENT_PORT);
                    try {
                        client.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                  //  XLog.d("sendBroadcast() called with: message = [" + message + "]");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void sendMessage(final String message, final String Ip, final int port) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress targetAddress = InetAddress.getByName(Ip);
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), targetAddress, port);
                    XLog.d("client.send    " + client);
                    client.send(packet);
                    // 数据发送事件
                    XLog.d("数据发送成功");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public interface MsgArrivedListener {
        void onMsgArrived(String msg);
    }


}
