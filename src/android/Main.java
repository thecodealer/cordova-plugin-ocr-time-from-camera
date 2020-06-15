package com.thecodealer.OcrTimeFromCamera;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.api.CommonStatusCodes;

public class Main extends CordovaPlugin {

    public static CallbackContext jsContext;

    private static final int RC_OCR_CAPTURE = 9003;
    private static final String TAG = "MainActivity";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.jsContext = callbackContext;

        if (action.equals("start")) {
            this.start();
            return true;
        }
        else if (action.equals("stop")) {
            this.stop();
            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                Boolean hasResult = false;
                if (data != null) {
                    String text = data.getStringExtra(com.thecodealer.OcrTimeFromCamera.ocrreader.OcrCaptureActivity.TextBlockObject);
                    if (text != null) {
                        hasResult = true;
                        this.returnSuccess("captured", text);
                    }
                }
                this.returnSuccess("stopped", null);
            }
            else {
                String err = data.getParcelableExtra("err");
                Log.d(TAG, err);
                this.returnError("capture_error", err, null);
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.jsContext = callbackContext;
    }

    private void start() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                openOcrCaptureActivity();
            }
        });
    }

    private void stop() {
        this.returnSuccess("stopped", null);
    }

    private void openOcrCaptureActivity() {
        Intent intent = new Intent(this.cordova.getActivity().getApplicationContext(), com.thecodealer.OcrTimeFromCamera.ocrreader.OcrCaptureActivity.class);
        this.cordova.setActivityResultCallback(this);
        this.cordova.startActivityForResult(this, intent, RC_OCR_CAPTURE);
        this.returnSuccess("started", null);
    }

    public void returnError(String id, String message, String paramsJson) {
        if (this.jsContext != null) {
            try {
                JSONObject params = new JSONObject();
                params.put("id", id);
                params.put("error", message);
                params.put("params", paramsJson);
                returnResponse("error", params);
            }
            catch(JSONException e) {}
        }
    }

    public void returnSuccess(String event, String paramsJson) {
        if (this.jsContext != null) {
            try {
                JSONObject params = new JSONObject();
                params.put("event", event);
                params.put("data", paramsJson);
                returnResponse("success", params);
            }
            catch(JSONException e) {}
        }
    }

    public void returnResponse(String status, JSONObject response) {
        PluginResult.Status responseStatus = status.equals("success") ? PluginResult.Status.OK : PluginResult.Status.ERROR;
        PluginResult result = new PluginResult(responseStatus, response);
        result.setKeepCallback(true);
        this.jsContext.sendPluginResult(result);
    }
}
