package net.iosmart.ismart56control;

import android.app.smdt.SmdtManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Iosmart56-MainActivity";
    private SmdtManager smdt;
    private boolean wifi_connected = false;
    private String subnet;
    private Timer timer=null;

    WifiManager wifiManager;

    private TextView textView;
    private ListView listViewData, listViewIp;

    ArrayList<String> dataList,ipList;
    ArrayAdapter<String> adapterData, adapterIp;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    textView.setText("Start to scan active IP in the same network: \n\n");
                    ipList.clear();
                    adapterIp.notifyDataSetInvalidated();
                    break;
                case 2:
                    textView.setText("Active IP in the same network: \n\n");
                    for(String ips: ipList) {
                        new GetSensorDataTask().execute("http:/" + ips + "/json-data");
                    }
                    break;
                case 3:
                    dataList.clear();
                    adapterData.notifyDataSetInvalidated();
                    break;
                case 4:

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        smdt = SmdtManager.create(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);  //net broadcast
        registerReceiver(intentReciver, filter);

        textView = findViewById(R.id.textview);
        listViewIp = findViewById(R.id.listviewip);
        listViewData = findViewById(R.id.listviewdata);
        ipList = new ArrayList();
        adapterIp = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, ipList);
        listViewIp.setAdapter(adapterIp);
        dataList = new ArrayList();
        adapterData = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, dataList);
        listViewData.setAdapter(adapterData);

        startTimer();


    }

    private BroadcastReceiver intentReciver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                    Log.i(TAG, info.getTypeName());
                    if(info != null && info.isConnected() && info.getTypeName().equals("WIFI")) {
                        wifi_connected=true;
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        int i = wifiInfo.getIpAddress();
                        subnet = (i & 0xff)+ "." + ((i>>8) & 0xff) + "." + ((i>>16) & 0xff) + ".";
                        Log.i(TAG, "--->subnet: " + subnet );
                    }else{
                        wifi_connected=false;
                    }
                    break;
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(intentReciver);
        if(timer != null) timer.cancel();

    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new MyTimerTask(), 100, 60*1000);  //60 seconds
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.i(TAG, "----> timer task ");
            if(wifi_connected) new ScanIpTask().execute(subnet);
        }
    }


    public class GetSensorDataTask extends AsyncTask<String, String, IsmartData> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mHandler.sendEmptyMessage(3);
        }

        @Override
        protected IsmartData doInBackground(String... strings) {
            IsmartData ismartData = new IsmartData();
            try {
                HttpClient client = new DefaultHttpClient();
                HttpParams params = client.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 3000);
                HttpConnectionParams.setSoTimeout(params, 3000);
                HttpGet method = new HttpGet(new URI(strings[0]));
                HttpResponse response = client.execute(method);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    String jsonData = EntityUtils.toString(entity);
                 //   Log.i(TAG, jsonData);
                    if (jsonData != null) {
                        //Parse json data: {"nodes":[{"temperature":"84.52","alias":"New PCB","mac":"EC:FA:BC:9C:D1:00","humidity":"31","volt":"6.60"}]}
                        JSONObject obj = new JSONObject(jsonData);
                        JSONArray jsonArray = obj.getJSONArray("nodes");
                        JSONObject object = jsonArray.getJSONObject(0);
                        String title = object.getString("alias");
                        String temp = object.getString("temperature");
                        String mac = object.getString("mac");
                        String humi = object.getString("humidity");
                        String volt = object.getString("volt");
                        ismartData.setHumididy(humi);
                        ismartData.setMac(mac);
                        ismartData.setTemperature(temp);
                        ismartData.setTitle(title);
                        ismartData.setVolt(volt);
                        ismartData.setIp(strings[0]);
                    }
                }
            }catch (Exception e){
                Log.i(TAG, "Nothing");
            //    e.printStackTrace();
            }
            return ismartData;
        }

        @Override
        protected void onPostExecute(IsmartData ismartData) {
            super.onPostExecute(ismartData);
            Log.i(TAG, ismartData.toString());
            if(ismartData.getMac() != null) {
                String result = "Device Alias: " + ismartData.getTitle();
                result += " , Temperature: " + ismartData.getTemperature() + " °F";
                result += " , Humidity: " + ismartData.getHumididy() + " %";
                result += " , IP: " + ismartData.getIp();
                result += " , Last Check in " + new SimpleDateFormat("HH:mm:ss").format(new Date());
                dataList.add(result);
                adapterData.notifyDataSetInvalidated();

            //    sendMessage();
            }

        }
    }

    private class ScanIpTask extends AsyncTask<String, String, Void> {
        //static final String subnet = "192.168.1.";
        static final int lower = 105;
        static final int upper = 120;
        static final int timeout = 5000;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mHandler.sendEmptyMessage(1);
        }

        @Override
        protected Void doInBackground(String... strings) {
            String subnet = strings[0];
            for (int i = lower; i <= upper; i++) {
                String host = subnet + i;
                try {
                    InetAddress inetAddress = InetAddress.getByName(host);
                    if (inetAddress.isReachable(timeout)){
                        publishProgress(inetAddress.toString(),"");
                    }else{
                        Log.i(TAG, inetAddress.toString());
                        publishProgress(inetAddress.toString(), ".");
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            textView.append("* ");
            if(values[1].equals("")) {
                ipList.add(values[0]);
                adapterIp.notifyDataSetInvalidated();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(TAG, "-----> Done");
            textView.append(" done");
            mHandler.sendEmptyMessage(2);

        }
    }


    private void sendMessage() {

        Thread sender = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GMailSender sender = new GMailSender("Your Gmail address", "Your Gmail Passwoed");
                    sender.sendMail("Email From Ismart App",
                            "Email content ",
                            "Your Email address",
                            "Send Email Address");
                } catch (Exception e) {
                    Log.i("mylog", "Error: " + e.getMessage());
                }
            }
        });
        sender.start();
    }


}


class IsmartData {
    private String title;
    private String temperature;
    private String humididy;
    private String mac;
    private String volt;
    private String ip;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumididy() {
        return humididy;
    }

    public void setHumididy(String humididy) {
        this.humididy = humididy;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getVolt() {
        return volt;
    }

    public void setVolt(String volt) {
        this.volt = volt;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "IsmartData{" +
                "title='" + title + '\'' +
                ", temperature='" + temperature + '\'' +
                ", humididy='" + humididy + '\'' +
                ", mac='" + mac + '\'' +
                ", volt='" + volt + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}




