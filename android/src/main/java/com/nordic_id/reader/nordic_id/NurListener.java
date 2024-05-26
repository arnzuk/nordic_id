package com.nordic_id.reader.nordic_id;

import java.util.HashMap;
import com.nordicid.nurapi.NurEventIOChange;

interface NurListener {
    void onConnected(boolean isConnected);

    void onStopTrace();

    void onTraceTagEvent(int scaledRssi);

    void onClearInventoryReadings();

    void onInventoryResult(HashMap<String, String> tags, String jsonString);

    void onIOChangeEvent(NurEventIOChange nurEventIOChange);

    void onBarcodeResult(String barcode);
}
