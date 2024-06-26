package com.nordic_id.reader.nordic_id;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.nordicid.nurapi.NurEventIOChange;
import android.util.Log;

/**
 * NordicIdPlugin
 */
public class NordicIdPlugin
        implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener, NurListener {
    private MethodChannel channel;
    private static final String CHANNEL_Initialize = "Initialize";
    private static final String CHANNEL_Connect = "Connect";
    private static final String CHANNEL_Destroy = "Destroy";
    private static final String CHANNEL_StopTrace = "StopTrace";
    private static final String CHANNEL_Reset = "Reset";
    private static final String CHANNEL_PowerOff = "PowerOff";
    private static final String CHANNEL_RefreshTracing = "RefreshTracing";
    private static final String CHANNEL_IsConnected = "IsConnected";
    private static final String CHANNEL_ConnectionStatus = "ConnectionStatus";
    private static final String CHANNEL_TagsStatus = "TagsStatus";
    private static final String CHANNEL_StartSingleScan = "StartSingleScan";
    private static final String CHANNEL_StartInventoryScan = "StartInventoryScan";
    private static final String CHANNEL_StopInventoryScan = "StopInventoryScan";
    private static final String CHANNEL_StartBarcodeScan = "StartBarcodeScan";
    private static final String CHANNEL_BarcodeScan = "BarcodeScan";
    private static final String CHANNEL_BarcodeStatus = "BarcodeStatus";
    private static final String CHANNEL_StopBarcodeScan = "StopBarcodeScan";
    private static final String ChANNEL_StartTrace = "StartTrace";

    private static final PublishSubject<Boolean> connectionStatus = PublishSubject.create();
    private static final PublishSubject<String> tagsStatus = PublishSubject.create();
    private static final PublishSubject<String> buttonEvent = PublishSubject.create();
    private static final PublishSubject<String> barcodeStatus = PublishSubject.create();
    private static final PublishSubject<Integer> traceEvent = PublishSubject.create();

    public static final String TAG = "NUR_Helper";

    Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "nordic_id");
        channel.setMethodCallHandler(this);
        initReadEvent(flutterPluginBinding.getBinaryMessenger());
        initConnectionEvent(flutterPluginBinding.getBinaryMessenger());
        initButtonEvent(flutterPluginBinding.getBinaryMessenger());
        initBarcodeEvent(flutterPluginBinding.getBinaryMessenger());
        initTraceEvent(flutterPluginBinding.getBinaryMessenger());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else {
            handleMethods(call, result);
        }

    }

    private void handleMethods(MethodCall call, Result result) {
        switch (call.method) {
            case CHANNEL_Initialize:
                init();
                result.success(true);
                break;
            case CHANNEL_Connect:
                NurHelper.getInstance().connect();
                result.success(true);
                break;
            case CHANNEL_IsConnected:
                final boolean isConnected = NurHelper.getInstance().isConnected();
                result.success(isConnected);
                break;
            case CHANNEL_Reset:
                NurHelper.getInstance().reset();
                result.success(true);
                break;
            case CHANNEL_PowerOff:
                NurHelper.getInstance().powerOff();
                result.success(true);
                break;
            case CHANNEL_StopTrace:
                NurHelper.getInstance().stopTrace();
                result.success(true);
                break;
            case CHANNEL_Destroy:
                NurHelper.getInstance().destroy();
                result.success(true);
                break;
            case CHANNEL_RefreshTracing:
                try {
                    if (NurHelper.getInstance().isTracingTag()) {
                        NurHelper.getInstance().stopTrace();
                    }
                    NurHelper.getInstance().clearInventoryReadings();
                    NurHelper.getInstance().ScanSingleTagThread();
                } catch (Exception ex) {
                    result.success(false);
                    ex.printStackTrace();
                }
                result.success(true);
                break;
            case CHANNEL_StartSingleScan:
                try {
                    if (NurHelper.getInstance().isTracingTag()) {
                        NurHelper.getInstance().stopTrace();
                    }
                    NurHelper.getInstance().clearInventoryReadings();
                    NurHelper.getInstance().ScanSingleTagThread();
                    result.success(true);
                } catch (Exception ex) {
                    result.success(false);
                    ex.printStackTrace();
                }
                break;
            case CHANNEL_StartBarcodeScan:
                try {
                    NurHelper.getInstance().StartBarcodeScan();
                    result.success(true);
                } catch (Exception ex) {
                    result.success(false);
                    ex.printStackTrace();
                }
                break;
            case CHANNEL_BarcodeScan:
                try {
                    NurHelper.getInstance().ScanBarcode();
                    result.success(true);
                } catch (Exception ex) {
                    result.success(false);
                    ex.printStackTrace();
                }
                break;
            case CHANNEL_StopBarcodeScan:
                try {
                    NurHelper.getInstance().StopBarcodeScan();
                    result.success(true);
                } catch (Exception ex) {
                    result.success(false);
                    ex.printStackTrace();
                }
                break;
            case ChANNEL_StartTrace:
                String tag = call.argument("tag");
                NurHelper.getInstance().setTagTrace(tag);
                String r = NurHelper.getInstance().startTrace();
                Log.i(TAG, tag);
                result.success(r);
                break;
            default:
                result.notImplemented();
        }
    }

    private static void initConnectionEvent(BinaryMessenger messenger) {
        final EventChannel connectionEventChannel = new EventChannel(messenger, CHANNEL_ConnectionStatus);
        connectionEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                connectionStatus
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(Boolean isConnected) {
                                eventSink.success(isConnected);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    private static void initButtonEvent(BinaryMessenger messenger) {
        final EventChannel connectionEventChannel = new EventChannel(messenger, "ButtonEvent");
        connectionEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                buttonEvent
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(String buttonEvent) {
                                eventSink.success(buttonEvent);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    private static void initTraceEvent(BinaryMessenger messenger) {
        final EventChannel connectionEventChannel = new EventChannel(messenger, "TraceEvent");
        connectionEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                traceEvent
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {

                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(Integer traceEvent) {
                                eventSink.success(traceEvent);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    private static void initBarcodeEvent(BinaryMessenger messenger) {
        final EventChannel scannerEventChannel = new EventChannel(messenger, CHANNEL_BarcodeStatus);
        scannerEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                barcodeStatus
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(String barcode) {
                                eventSink.success(barcode);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    private static void initReadEvent(BinaryMessenger messenger) {
        final EventChannel scannerEventChannel = new EventChannel(messenger, CHANNEL_TagsStatus);
        scannerEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                tagsStatus
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(String tag) {
                                eventSink.success(tag);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
        activityPluginBinding.addActivityResultListener(this);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
        activityPluginBinding.addActivityResultListener(this);
    }

    public void init() {
        NurHelper.getInstance().init(activity);
        NurHelper.getInstance().initReading(this);
        NurHelper.getInstance().initBarcodeReading();
    }

    @Override
    public void onConnected(boolean isConnected) {
        connectionStatus.onNext(isConnected);
    }

    @Override
    public void onStopTrace() {
    }

    @Override
    public void onTraceTagEvent(int scaledRssi) {
        traceEvent.onNext(scaledRssi);
    }

    @Override
    public void onClearInventoryReadings() {
    }

    @Override
    public void onInventoryResult(HashMap<String, String> tags, String jsonString) {
        tagsStatus.onNext(jsonString);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        NurHelper.getInstance().onActivityResult(requestCode, resultCode, data);
        return true;
    }

    @Override
    public void onIOChangeEvent(NurEventIOChange event) {
        try {
            JSONObject json = new JSONObject();
            json.put("source", Integer.toString(event.source));
            json.put("direction", Integer.toString(event.direction));
            buttonEvent.onNext(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBarcodeResult(String barcode) {
        barcodeStatus.onNext(barcode);
    }
}
