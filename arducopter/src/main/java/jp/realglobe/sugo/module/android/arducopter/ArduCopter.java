package jp.realglobe.sugo.module.android.arducopter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.realglobe.sugo.actor.Emitter;
import jp.realglobe.sugo.actor.ModuleMethod;

/**
 * arduCopter モジュール。
 * Created by fukuchidaisuke on 16/11/28.
 */
public class ArduCopter extends Emitter implements Cloneable {


    private static final String LOG_TAG = ArduCopter.class.getName();

    private static final SparseArray<String> MODE_PREFIXES;

    static {
        MODE_PREFIXES = new SparseArray<>();
        MODE_PREFIXES.put(Type.TYPE_COPTER, "COPTER_");
        MODE_PREFIXES.put(Type.TYPE_PLANE, "PLANE_");
        MODE_PREFIXES.put(Type.TYPE_ROVER, "ROVER_");
    }

    public static final String CONNECT_TYPE_UDP = "UDP";
    public static final String CONNECT_TYPE_TCP = "TCP";
    public static final String CONNECT_TYPE_USB = "USB";
    public static final String CONNECT_TYPE_BLUETOOTH = "BLUETOOTH";
    public static final String CONNECT_TYPE_SOLO = "SOLO";


    private final ControlTower tower;
    private final Drone drone;

    private final ControlApi control;
    private final VehicleApi vehicle;
    private final MissionApi mission;
    private final GimbalApi gimbal;
    private final SoloCameraApi soloCamera;
    private final DroneWrapper info;
    private final MyTowerListener listener;

    public ArduCopter(String name, Handler handler, Context context) {
        super(name);

        this.tower = new ControlTower(context);
        this.drone = new Drone(context);

        this.control = ControlApi.getApi(this.drone);
        this.vehicle = VehicleApi.getApi(this.drone);
        this.mission = MissionApi.getApi(this.drone);
        this.gimbal = GimbalApi.getApi(this.drone);
        this.soloCamera = SoloCameraApi.getApi(this.drone);
        this.info = new DroneWrapper(this.drone, this.gimbal);

        this.listener = new MyTowerListener(this.tower, this.info, handler, this);
        this.tower.connect(this.listener);
    }


    public void close() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        if (this.tower.isTowerConnected()) {
            this.tower.unregisterDrone(this.drone);
            this.tower.disconnect();
        }
    }

    /**
     * ドローンにつなぐ。
     * <table border=1>
     * <caption>引数の例</caption>
     * <tr><th>type</th><th>address</th><th>説明</th></tr>
     * <tr><th>USB</th><th>null</th><th>USB 変換されたシリアルポートにデフォルトボーレートでつなぐ。デフォルトボーレートは 57600</th></tr>
     * <tr><th>USB</th><th>230400</th><th>USB 変換されたシリアルポートにボーレート 230400 でつなぐ</th></tr>
     * <tr><th>UDP</th><th>null</th><th>デフォルトポートで UDP 接続を待つ。デフォルトポートは 14550</th></tr>
     * <tr><th>UDP/TCP</th><th>192.168.1.3</th><th>IP アドレス 192.168.1.3 に TCP/UDP のデフォルトポートでつなぐ。TCP のデフォルトポートは 5763</th></tr>
     * <tr><th>UDP/TCP</th><th>192.168.1.3:12345</th><th>IP アドレス 192.168.1.3 に TCP/UDP のポート 12345 でつなぐ</th></tr>
     * </table>
     *
     * @param type    接続タイプ
     * @param address 詳細
     */
    @ModuleMethod
    public void connect(String type, String address) {
        if (this.drone.isConnected()) {
            Log.w(LOG_TAG, "Drone already connected");
            return;
        }
        final ConnectionParameter connectParams = parseConnectionParameter(type, address);
        Log.i(LOG_TAG, "Connect " + connectParams);
        this.drone.connect(connectParams);
    }

    private static ConnectionParameter parseConnectionParameter(String type, String address) {
        switch (type.toUpperCase(Locale.US)) {
            case CONNECT_TYPE_UDP: {
                if (address == null || address.isEmpty()) {
                    return ConnectionParameter.newUdpConnection(null);
                }
                final Address udp = Address.parse(address);
                final int localPort = udp.getLocalPort() > 0 ? udp.getLocalPort() : ConnectionType.DEFAULT_UDP_SERVER_PORT;
                if (udp.getRemoteHost() == null || udp.getRemoteHost().isEmpty()) {
                    return ConnectionParameter.newUdpConnection(localPort, null);
                }
                final String remoteHost = udp.getRemoteHost();
                final int remotePort = udp.getRemotePort() > 0 ? udp.getRemotePort() : ConnectionType.DEFAULT_UDP_SERVER_PORT;
                return ConnectionParameter.newUdpWithPingConnection(localPort, remoteHost, remotePort, new byte[]{}, null);
            }
            case CONNECT_TYPE_TCP: {
                final Address tcp = Address.parse(address);
                if (tcp.getRemoteHost() == null || tcp.getRemoteHost().isEmpty()) {
                    throw new IllegalArgumentException("no remote host: " + address);
                }
                final String remoteHost = tcp.getRemoteHost();
                final int remotePort = tcp.getRemotePort() > 0 ? tcp.getRemotePort() : ConnectionType.DEFAULT_TCP_SERVER_PORT;
                return ConnectionParameter.newTcpConnection(remoteHost, remotePort, null);
            }
            case CONNECT_TYPE_USB: {
                if (address == null || address.isEmpty()) {
                    return ConnectionParameter.newUsbConnection(null);
                }
                final int baudRate = Integer.parseInt(address);
                return ConnectionParameter.newUsbConnection(baudRate, null);
            }
            case CONNECT_TYPE_BLUETOOTH: {
                return ConnectionParameter.newBluetoothConnection(address, null);
            }
            case CONNECT_TYPE_SOLO: {
                final int sep = address.indexOf(":");
                if (sep < 0) {
                    return ConnectionParameter.newSoloConnection(address, null, null);
                }
                return ConnectionParameter.newSoloConnection(address.substring(0, sep), address.substring(sep + 1), null);
            }
            default: {
                throw new IllegalArgumentException("unsupported connect type: " + type);
            }
        }
    }

    /**
     * ドローンとの接続を切る
     */
    @ModuleMethod
    public void disconnect() {
        if (!this.drone.isConnected()) {
            Log.w(LOG_TAG, "Drone is not connecting");
            return;
        }
        this.drone.disconnect();
    }

    /**
     * 高さを変える
     *
     * @param altitude 高さ
     */
    @ModuleMethod
    public void climbTo(double altitude) {
        this.control.climbTo(altitude);
    }

    /**
     * 移動
     *
     * @param latitude  緯度
     * @param longitude 経度
     */
    @ModuleMethod
    public void goTo(double latitude, double longitude) {
        this.control.goTo(new LatLong(latitude, longitude), true, null);
    }

    /**
     * その場で止まる
     */
    @ModuleMethod
    public void pause() {
        this.control.pauseAtCurrentLocation(null);
    }

    /**
     * 離陸
     *
     * @param altitude 離陸後の目標高さ
     */
    @ModuleMethod
    public void takeoff(double altitude) {
        this.control.takeoff(altitude, null);
    }

    /**
     * 着陸
     */
    @ModuleMethod
    public void land() {
        this.vehicle.setVehicleMode(VehicleMode.COPTER_LAND);
    }

    /**
     * 離陸地点の上に帰る
     */
    @ModuleMethod
    public void returnToLaunch() {
        this.vehicle.setVehicleMode(VehicleMode.COPTER_RTL);
    }

    /**
     * 向きを変える
     *
     * @param angle        角度
     * @param angularSpeed 向きを変える速度
     * @param relative     相対的な角度かどうか
     */
    @ModuleMethod
    public void turnTo(double angle, double angularSpeed, boolean relative) {
        this.control.turnTo((float) angle, (float) angularSpeed, relative, null);
    }

    /**
     * 駆動切り替え
     *
     * @param arm true なら駆動させる
     */
    @ModuleMethod
    public void arm(boolean arm) {
        this.vehicle.arm(arm);
    }

    /**
     * 動作モードを切り替える
     *
     * @param newMode 動作モード
     */
    @ModuleMethod
    public void setMode(String newMode) {
        final Type type = this.drone.getAttribute(AttributeType.TYPE);
        final String prefix = MODE_PREFIXES.get(type.getDroneType());
        if (prefix == null) {
            throw new IllegalStateException("unsupported type: " + type.getDroneType());
        }
        this.vehicle.setVehicleMode(VehicleMode.valueOf(prefix + newMode.toUpperCase(Locale.US)));
    }

    /**
     * 起点を設定する
     *
     * @param latitude  緯度
     * @param longitude 経度
     * @param altitude  高さ
     */
    @ModuleMethod
    public void setHome(double latitude, double longitude, double altitude) {
        this.vehicle.setVehicleHome(new LatLongAlt(latitude, longitude, altitude), null);
    }

    /**
     * ミッション内の指定したコマンドに移る。
     * ミッションについては {@link Missions} を参照
     *
     * @param index コマンド位置
     */
    @ModuleMethod
    public void jumpToCommand(int index) {
        this.mission.gotoWaypoint(index, null);
    }

    /**
     * ドローンに保存されているミッションを読み込む。
     * ミッションは EVENT_MISSION イベントで受け取る。
     * ミッションについては {@link Command} を参照
     */
    @ModuleMethod
    public void loadMission() {
        this.mission.loadWaypoints();
    }

    /**
     * ドローンにミッションを保存する。
     * ミッションについては {@link Command} を参照
     *
     * @param mission ミッション
     */
    @ModuleMethod
    public void saveMission(Object[] mission) {
        final Mission newMission = Missions.decode(mission);
        this.mission.setMission(newMission, true);
    }

    /**
     * ミッションの実行を開始する。
     * ミッションについては {@link Command} を参照
     *
     * @param forceModeChange ミッションを実行できるモードに自動で移るか
     * @param forceArm        自動で駆動を開始するか
     */
    @ModuleMethod
    public void startMission(boolean forceModeChange, boolean forceArm) {
        this.mission.startMission(forceModeChange, forceArm, null);
    }

    /**
     * ミッションの実行を一時停止する。
     * ミッションについては {@link Command} を参照
     */
    @ModuleMethod
    public void pauseMission() {
        this.mission.pauseMission(null);
    }

    /**
     * 接続しているかどうかを返す
     *
     * @return 接続していれば true
     */
    @ModuleMethod
    public boolean isConnected() {
        return this.info.isConnected();
    }


    /**
     * 駆動しているかどうかを返す
     *
     * @return 駆動していれば true
     */
    @ModuleMethod
    public boolean isArmed() {
        return this.info.isArmed();
    }

    /**
     * 飛んでいるかどうかを返す
     *
     * @return 飛んでいれば true
     */
    @ModuleMethod
    public boolean isFlying() {
        return this.info.isFlying();
    }

    /**
     * 機種を返す
     *
     * @return 機種情報。{@link Event#type} を参照
     */
    @ModuleMethod
    public Map<String, Object> getType() {
        return this.info.getType();
    }

    /**
     * 動作モードを返す
     *
     * @return 動作モード情報。{@link Event#mode} を参照
     */
    @ModuleMethod
    public Map<String, Object> getMode() {
        return this.info.getMode();
    }

    /**
     * 速さを返す
     *
     * @return 速さ情報。{@link Event#speed} を参照
     */
    @ModuleMethod
    public Map<String, Object> getSpeed() {
        return this.info.getSpeed();
    }

    /**
     * バッテリー状態を返す
     *
     * @return バッテリー状態情報。{@link Event#battery} を参照
     */
    @ModuleMethod
    public Map<String, Object> getBattery() {
        return this.info.getBattery();
    }

    /**
     * 起点を返す
     *
     * @return 起点情報。{@link Event#home} を参照
     */
    @ModuleMethod
    public Map<String, Object> getHome() {
        return this.info.getHome();
    }

    /**
     * 現在位置を返す
     *
     * @return 現在位置情報。{@link Event#position} を参照
     */
    @ModuleMethod
    public Map<String, Object> getPosition() {
        return this.info.getPosition();
    }

    /**
     * 読み込んだミッションを返す
     *
     * @return 読み込んだミッション情報。{@link Event#mission} を参照
     */
    @ModuleMethod
    public Map<String, Object> getMission() {
        return this.info.getMission();
    }

    /**
     * 到達したミッションコマンドを返す
     *
     * @return 到達したミッションコマンド情報。{@link Event#commandReached} を参照
     */
    @ModuleMethod
    public Map<String, Object> getReachedCommand() {
        return this.info.getReachedCommand();
    }

    /**
     * イベントを有効にする
     *
     * @param events 有効化するイベント。null なら全てのイベントを有効化する
     */
    @ModuleMethod
    public void enableEvents(Object[] events) {
        this.listener.enableEvents(parseEvents(events));
    }

    /**
     * イベントを無効にする
     *
     * @param events 無効化するイベント。null なら全てのイベントを無効化する
     */
    @ModuleMethod
    public void disableEvents(Object[] events) {
        this.listener.disableEvents(parseEvents(events));
    }

    private List<Event> parseEvents(Object[] events) {
        if (events == null) {
            return Arrays.asList(Event.values());
        }
        final List<Event> list = new ArrayList<>();
        for (Object event : events) {
            list.add(Event.valueOf((String) event));
        }
        return list;
    }

    /**
     * 有効なイベントを返す
     *
     * @return 有効なイベント
     */
    @ModuleMethod
    public List<String> getEnableEvents() {
        final List<String> events = new ArrayList<>();
        for (Event event : this.listener.getEnableEvents()) {
            events.add(event.name());
        }
        return events;
    }

    /**
     * ジンバルの向きを返す
     *
     * @return ジンバルの向き。{@link Event#gimbalOrientation} を参照
     */
    @ModuleMethod
    public Map<String, Object> getGimbalOrientation() {
        return this.info.getGimbalOrientation();
    }

    private final GimbalApi.GimbalOrientationListener gimbalOrientationListener = new GimbalApi.GimbalOrientationListener() {
        @Override
        public void onGimbalOrientationUpdate(GimbalApi.GimbalOrientation orientation) {
            Log.d(LOG_TAG, "Gimbal orientation is " + orientation);
        }

        @Override
        public void onGimbalOrientationCommandError(int error) {
            Log.e(LOG_TAG, "Gimbal orientation update failed: " + error);
        }
    };

    /**
     * ジンバルの制御を奪う
     */
    @ModuleMethod
    public void startGimbalControl() {
        this.gimbal.startGimbalControl(gimbalOrientationListener);
    }

    /**
     * ジンバルの制御を返還する
     */
    @ModuleMethod
    public void stopGimbalControl() {
        this.gimbal.stopGimbalControl(gimbalOrientationListener);
    }

    /**
     * ジンバルの向きを変える
     *
     * @param pitch 上下を向く角度
     * @param roll  頭を横に傾ける角度
     * @param yaw   左右を向く角度
     */
    @ModuleMethod
    public void setGimbalOrientation(double pitch, double roll, double yaw) {
        this.gimbal.updateGimbalOrientation((float) pitch, (float) roll, (float) yaw, gimbalOrientationListener);
    }

    /**
     * 映像の記録を開始する
     */
    @ModuleMethod
    public void startVideoRecording() {
        this.soloCamera.startVideoRecording(null);
    }

    /**
     * 映像の記録を止める
     */
    @ModuleMethod
    public void stopVideoRecording() {
        this.soloCamera.stopVideoRecording(null);
    }

}
