package net.iosmart.ismart56control;

import android.app.smdt.SmdtManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private SmdtManager smdt;
    private static final String TAG = "Iosmart56-MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        smdt = SmdtManager.create(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        String path = "http://192.168.2.114/json-data";
        Log.i(TAG, path);
        new GetSensorDataTask().execute(path);
    }

    public class GetSensorDataTask extends AsyncTask<String, String, IsmartData> {

        @Override
        protected IsmartData doInBackground(String... strings) {
            IsmartData ismartData = new IsmartData();
            try {
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(strings[0]);
                HttpResponse response = client.execute(post);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    String jsonData = EntityUtils.toString(entity);
                    Log.i(TAG, jsonData);
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
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            return ismartData;
        }

        @Override
        protected void onPostExecute(IsmartData ismartData) {
            super.onPostExecute(ismartData);
            Log.i(TAG, ismartData.toString());

        }
    }

}


class IsmartData {
    private String title;
    private String temperature;
    private String humididy;
    private String mac;
    private String volt;

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

    @Override
    public String toString() {
        return "IsmartData{" +
                "title='" + title + '\'' +
                ", temperature='" + temperature + '\'' +
                ", humididy='" + humididy + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}
