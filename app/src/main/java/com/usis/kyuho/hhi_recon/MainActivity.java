/*
* 현대중공업 POC
* Recon jet smart glasses 개발
* 유시스 신규호 주임연구원
* */

package com.usis.kyuho.hhi_recon;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import kr.usis.api.bean.Record;
import kr.usis.api.bean.Sort;
import kr.usis.api.core.das.DasApi;

public class MainActivity extends Activity {

    private static final String[] stateMsg = {
            "용접 상태 OK",              //0
            "용접 상태 NG",              //1
            "구획 - 용접기 미매칭",      //2
            "구획 - 용접기 매칭"         //3
    };
    private static final String[] detailMsg = {
            "정상 용접조건을 벗어 났습니다. 용접불량 확률이 높습니다.",                    //0
            "용접하고자 하는 구획정보와 용접기가 매칭되지 않았습니다.",                     //1
            "관리자에 연락하여 구획-용접기 매칭 후 용접작업 하실 것을 권고합니다.",        //2
            "정상입니다."                                                                     //3
    };
    private static final String[] type = {
            "state",             //0
            "detail1",           //1
            "detail2",           //2
            "crt",               //3
            "vtg",               //4
            "crtThreshold",     //5
            "vtgThreshold"      //6
    };

    private TextView statemsgTextView;
    private TextView detailmsg1TextView;
    private TextView detailmsg2TextView;
    private TextView crtTextView;
    private TextView vtgTextView;
    private TextView crtThresholdTextView;
    private TextView vtgThresholdTextView;
    private Chronometer chronometer;
    private LinearLayout goneLinear;
    private View view1;
    private WifiManager WM;
    private ConnectivityManager cm;
    private static DasApi dasApi = new DasApi("1.221.16.14", 9443, "admin", "admin", false);
    private HashMap<String, Object> wso2Hash;
    private HashMap<String, Object> uiCall;
    private String uiHttpUrl; // UI call URI
    private String boxId;
    private String welderId;
    /*
    * 카산드라에서 받는 데이터
    * */
    private String crtFeed;
    private String vtgFeed;
//    private String shipNo1;
//    private String divisionNo1;
//    private String processNo1;

    /*
    * UI에서 받는 데이터
    * */
    private String crtMin;
    private String crtMax;
    private String vtgMin;
    private String vtgMax;
    //    private String shipNo2;
//    private String divisionNo2;
//    private String processNo2;
    private String matcingData;

    private boolean crt_Warning = false;
    private boolean vtg_Warning = false;
    private boolean matching = false;
    private Getwso2api getwso2api;
    private GetUIInfo getUIInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createObject();
        try {
            wificonnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createObject() {
        statemsgTextView = (TextView) findViewById(R.id.statemsgTextView);
        detailmsg1TextView = (TextView) findViewById(R.id.detailmsg1TextView);
        detailmsg2TextView = (TextView) findViewById(R.id.detailmsg2TextView);
        crtTextView = (TextView) findViewById(R.id.crtTextView);
        vtgTextView = (TextView) findViewById(R.id.vtgTextView);
        crtThresholdTextView = (TextView) findViewById(R.id.crtThresholdTextView);
        vtgThresholdTextView = (TextView) findViewById(R.id.vtgThresholdTextView);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        goneLinear = (LinearLayout) findViewById(R.id.goneLinear);
        view1 = (View) findViewById(R.id.view1);
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        WM = (WifiManager) getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        getUIInfo = new GetUIInfo();
        getwso2api = new Getwso2api();
    }

    private void wificonnect() throws InterruptedException {
        tvSetText(type[1], "WIFI 연결중...");
        while (!WM.pingSupplicant()) {
            if (WM.isWifiEnabled() == false) {
                WM.setWifiEnabled(true);
            }
            Log.e("wifi", "Enabled:" + String.valueOf(WM.isWifiEnabled()));
            Log.e("wifi", "Ping:" + String.valueOf(WM.pingSupplicant()));
        }
        Log.e("wifi", "Enabled:" + String.valueOf(WM.isWifiEnabled()));
        Log.e("wifi", "Ping:" + String.valueOf(WM.pingSupplicant()));
        tvSetText(type[1], "WIFI 연결!");

        getUIInfo.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetUIInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            Log.e("GetUIInfo", "GetUIInfo Start");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.e("GetUIInfo", "GetUIInfo End");
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (matching) {
                detailmsg2TextView.setVisibility(View.GONE);
                view1.setVisibility(View.GONE);
                goneLinear.setVisibility(View.VISIBLE);
                if (getwso2api.getStatus() != AsyncTask.Status.RUNNING) {
                    getwso2api.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                tvSetText(type[5], crtMin + "~" + crtMax);
                tvSetText(type[6], vtgMin + "~" + vtgMax);
            } else {
                tvSetText(type[0], stateMsg[2]);
                tvSetText(type[1], detailMsg[1]);
                tvSetText(type[2], detailMsg[2]);
                goneLinear.setVisibility(View.GONE);
                if (getwso2api.getStatus() == AsyncTask.Status.RUNNING) {
                    getwso2api.cancel(true);
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                if (isCancelled()) {
                    Log.e("GetUIInfo", "GetUIInfo cancelled");
                    break;
                }
                Log.e("GetUIInfo", "GetUIInfo running");
                uiHttpUrl = "http://125.141.32.31:8888/getWpsInfo"; // UI call URI
                boxId = "X6003";
                welderId = "RIXX60052";

                HashMap<String, Object> uiCall = null;
                try {
                    uiCall = dasApi.getCurVoltMinMax(uiHttpUrl, welderId, boxId);
                    Log.e("GetUIInfo", uiCall.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("GetUIInfo", e.toString());
                    continue;
                }

                try {
                    crtMin = String.valueOf(uiCall.get("currentMin"));
                    crtMax = String.valueOf(uiCall.get("currentMax"));
                    vtgMin = String.valueOf(uiCall.get("voltageMin"));
                    vtgMax = String.valueOf(uiCall.get("voltageMax"));
                    matcingData = String.valueOf(uiCall.get("flag"));
//                shipNo2 = String.valueOf(uiCall.get("shipNo"));
//                divisionNo2 = String.valueOf(uiCall.get("divisionNo"));
//                processNo2 = String.valueOf(uiCall.get("processNo"));

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("GetUIInfo", e.toString());
                }

//                crtMin = String.valueOf(20);
//                crtMax = String.valueOf(200);
//                vtgMin = String.valueOf(30);
//                vtgMax = String.valueOf(300);
//                shipNo2 = String.valueOf(XXXX);
//                divisionNo2 = String.valueOf(T25C3);
//                processNo2 = String.valueOf(N41);
//                matcingData = "true";
//
//                try {
//                    wso2Hash = getRestfulAPIValue();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

//                shipNo1 = String.valueOf(wso2Hash.get("shipNo"));0
//                divisionNo1 = String.valueOf(wso2Hash.get("divisionNo"));
//                processNo1 = String.valueOf(wso2Hash.get("processNo"));

//                shipNo1 = "XXXX";
//                divisionNo1 = "T25C3";
//                processNo1 = "N42";

                Log.e("GetUIInfo", "crtMin:" + crtMin + " / crtMax:" + crtMax + " / vtgMin:" + vtgMin + " / vtgMax:" + vtgMax);
                Log.e("GetUIInfo", "matchingData:" + matcingData);
//
//                Log.e("GetUIInfo", "shipNo2:" + shipNo2 + " / divisionNo2:" + divisionNo2 + " / processNo2:" + processNo2);
//                Log.e("GetUIInfo", "shipNo1:" + shipNo1 + " / divisionNo1:" + divisionNo1 + " / processNo1:" + processNo1);

                if (matcingData.equals("true")) {
                    matching = true;
                } else {
                    matching = false;
                }

                publishProgress();

                try {
                    if (getwso2api.getStatus() == Status.RUNNING) {
                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class Getwso2api extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            while (true) {
                if (isCancelled()) {
                    Log.e("Getwso2api", "Getwso2api cancelled");
                    break;
                }
                Log.e("Getwso2api", "Getwso2api running");
                try {
                    wso2Hash = getRestfulAPIValue();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Getwso2api", e.toString());
                    continue;
                }
                try {
                    crtFeed = String.valueOf(wso2Hash.get("FeedbackVoltage"));
                    vtgFeed = String.valueOf(wso2Hash.get("FeedbackAmpere"));

                    Log.e("Getwso2api", "crtFeed:" + crtFeed + " / vtgFeed:" + vtgFeed);
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                publishProgress();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            Log.e("Getwso2api", "Getwso2api Start");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.e("Getwso2api", "Getwso2api Stop");
            super.onPostExecute(integer);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //Print feed value to UI
            tvSetText(type[3], String.valueOf(crtFeed));
            tvSetText(type[4], String.valueOf(vtgFeed));
            //Log.e("warning", "crt_warn:" + String.valueOf(crt_Warning) + "/" + "vtg_warn:" + String.valueOf(vtg_Warning));

            //Normal state
            if (crt_Warning == false && vtg_Warning == false) {
                tvSetText(type[0], stateMsg[0]);
                tvSetText(type[1], detailMsg[3]);
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
            }

            //Abnormal state
            else {
                tvSetText(type[0], stateMsg[1]);
                chronometer.start();
                String[] tmp = String.valueOf(chronometer.getText()).split(":");
                if (tmp.length == 3) {
                    Log.e("chronometer", tmp[0] + "시간" + tmp[1] + "분" + " / " + String.valueOf(chronometer.getText()));
                    tvSetText(type[1], tmp[0] + "시간" + tmp[1] + "분 동안 " + detailMsg[0]);
                } else {
                    Log.e("chronometer", tmp[0] + "분" + " / " + String.valueOf(chronometer.getText()));
                    if (Integer.parseInt(tmp[0]) > 0) {
                        tvSetText(type[1], tmp[0] + "분 동안 " + detailMsg[0]);
                    } else {
                        tvSetText(type[1], detailMsg[0]);
                    }
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    private void tvSetText(String t, String msg) {
        switch (t) {
            case "state": //0
                statemsgTextView.setText(msg);
                break;
            case "detail1": //1
                detailmsg1TextView.setText(msg);
                break;
            case "detail2": //2
                detailmsg2TextView.setText(msg);
                break;
            case "crt": //3
                if (Integer.parseInt(msg) < Integer.parseInt(crtMin) || Integer.parseInt(msg) > Integer.parseInt(crtMax)) {
                    crtTextView.setTextColor(Color.RED);
                    crt_Warning = true;
                } else {
                    crtTextView.setTextColor(Color.WHITE);
                    crt_Warning = false;
                }
                crtTextView.setText("●전류: " + msg + "A");
                break;
            case "vtg": //4
                if (Integer.parseInt(msg) < Integer.parseInt(vtgMin) || Integer.parseInt(msg) > Integer.parseInt(vtgMax)) {
                    vtgTextView.setTextColor(Color.RED);
                    vtg_Warning = true;
                } else {
                    vtgTextView.setTextColor(Color.WHITE);
                    vtg_Warning = false;
                }
                vtgTextView.setText("●전압: " + msg + "V");
                break;
            case "crtThreshold": //5
                //●정상 전류:240~270A
                crtThresholdTextView.setText("●정상 전류:" + msg + "A");
                break;
            case "vtgThreshold": //6
                //●정상 전압:20~30V
                vtgThresholdTextView.setText("●정상 전압:" + msg + "V");
                break;

        }
    }

    public static HashMap<String, Object> getRestfulAPIValue() throws Exception {
        String tableName = "WMRAWDATA";
        String correlation_Welder_ID = "RIXX60052";
        String correlation_Box_ID = "X6003";
        String query = "correlation_Welder_ID:\"" + correlation_Welder_ID + "\" AND correlation_Box_ID:\"" + correlation_Box_ID + "\"";

        int start = 0;
        int count = 1;
        Sort[] sortList = new Sort[1];
        sortList[0] = new Sort("_timestamp", "DESC");

        ArrayList<Record> result = dasApi.searchOnTable(tableName, query, start, count, sortList);
        HashMap<String, Object> dataHashMap = new HashMap<String, Object>();

        for (int i = 0; i < result.size(); i++) {
            dataHashMap = result.get(i).getDataHashMap();
        }
        return dataHashMap;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        getwso2api.cancel(true);
        getUIInfo.cancel(true);
        this.finish();
        super.onDestroy();
    }
}
