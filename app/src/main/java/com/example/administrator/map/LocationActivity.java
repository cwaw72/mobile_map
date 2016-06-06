package com.example.administrator.map;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.R.layout;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class LocationActivity extends Activity implements LocationListener {

    private final String serverUrl = "http://52.79.197.58/map/index.php";

    private GoogleMap mmap;
    private LocationManager locationManager;
    private String provider;

    private Button save_btn;
    private Button show_btn;

    private double current_lat;
    private double current_lng;

    public LocationActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        GooglePlayServicesUtil.isGooglePlayServicesAvailable(LocationActivity.this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);

        save_btn = (Button) findViewById(R.id.save_btn);
        show_btn = (Button) findViewById(R.id.show_btn);

        show_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                AsyncDataClass asyncRequestObject = new AsyncDataClass();
                asyncRequestObject.execute(serverUrl, "show_log");            }
        });

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {


                Context mContext = LocationActivity.this;
                LayoutInflater inflater = (LayoutInflater) LocationActivity.this.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

                //R.layout.dialog는 xml 파일명이고  R.id.popup은 보여줄 레이아웃 아이디
                View layout = inflater.inflate(R.layout.dialog_edit,(ViewGroup) findViewById(R.id.popup));
                AlertDialog.Builder aDialog = new AlertDialog.Builder(mContext);


                final EditText edit_title = (EditText) layout.findViewById(R.id.edit_title) ;

                aDialog.setTitle("내 정보 수정하기"); //타이틀바 제목
                aDialog.setView(layout); //dialog.xml 파일을 뷰로 셋팅

                aDialog.setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {


                                AsyncDataClass asyncRequestObject = new AsyncDataClass();
                                asyncRequestObject.execute(serverUrl, edit_title.getText().toString(), Double.toString( mmap.getMyLocation().getLatitude()
                                ), Double.toString(mmap.getMyLocation().getLongitude()));

                                current_lat = mmap.getMyLocation().getLatitude();
                                current_lat = mmap.getMyLocation().getLongitude();


                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 'No'
                                return;
                            }
                        });
                AlertDialog ad = aDialog.create();
                //ad.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                //ad.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                ad.show();//보여줌!
            }
        });

        if (provider == null) {  //위치정보 설정이 안되어 있으면 설정하는 엑티비티로 이동합니다
            new AlertDialog.Builder(LocationActivity.this)
                    .setTitle("위치서비스 동의")
                    .setNeutralButton("이동", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            })
                    .show();
        } else {   //위치 정보 설정이 되어 있으면 현재위치를 받아옵니다
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(provider, 1, 1, LocationActivity.this);
            setUpMapIfNeeded();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {//위치설정 엑티비티 종료 후
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                provider = locationManager.getBestProvider(criteria, true);
                if (provider == null) {//사용자가 위치설정동의 안했을때 종료
                    finish();
                } else {//사용자가 위치설정 동의 했을때
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    locationManager.requestLocationUpdates(provider, 1L, 2F, LocationActivity.this);
                    setUpMapIfNeeded();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(this);
    }

    private void setUpMapIfNeeded() {
        if (mmap == null) {
            mmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap(); if (mmap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mmap.setMyLocationEnabled(true);
        mmap.getMyLocation();

    }


    boolean locationTag = true;

    @Override
    public void onLocationChanged(Location location) {
        //if (locationTag) {//한번만 위치를 가져오기 위해서 tag를 주었습니다
            Log.d("myLog", "onLocationChanged: !!" + "onLocationChanged!!");
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            Toast.makeText(LocationActivity.this, "위도  : " + lat + " 경도: " + lng, Toast.LENGTH_SHORT).show();
         //   locationTag = false;
        //}

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    private class AsyncDataClass extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5000);

            HttpClient httpClient = new DefaultHttpClient(httpParameters);
            HttpPost httpPost = new HttpPost(params[0]);

            String jsonResult = "";
            try {

                if(params[1].equals("show_log")){
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("title", params[1]));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);
                    jsonResult = inputStreamToString(response.getEntity().getContent()).toString();
                    System.out.println("Returned Json object " + jsonResult.toString());



                }
                else {
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("title", params[1]));
                    nameValuePairs.add(new BasicNameValuePair("lat", params[2]));
                    nameValuePairs.add(new BasicNameValuePair("lng", params[3]));

                    Log.e("title,lat,lng", " : " + params[1] + " " + params[2] + " " + params[3]);

                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);
                    jsonResult = inputStreamToString(response.getEntity().getContent()).toString();
                    System.out.println("Returned Json object " + jsonResult.toString());
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return jsonResult;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);


            System.out.println("Resulted Value: " + result);

            if(result.contains("numbers")) {
                String data[] = new String[100];
                //문자 확인;
                StringTokenizer tokens = new StringTokenizer(result, "[]");

                //List<Helper_data> helper_data = new ArrayList<Helper_data>(2);

                for (int i = 0; tokens.hasMoreElements(); i++) {
                    data[i] = tokens.nextToken();
                    Log.e("data" + i, " : " + data[i]);
                }
                int j = 2;
                LatLng position = null;
                for (int i = 1; i <= Integer.parseInt(data[1]); i++) {
                    Helper_data m_data = new Helper_data(data[j], data[j + 1], data[j + 2]);
                    //helper_data.add(i,m_data);
                    j += 3;

                    position = new LatLng(Double.parseDouble(m_data.getLat()), Double.parseDouble(m_data.getLng()));
                    mmap.addMarker(new MarkerOptions().position(position).title(m_data.getTitle()));
                }
                mmap.moveCamera(CameraUpdateFactory.newLatLngZoom(position,13));
            }
        }
        private StringBuilder inputStreamToString(InputStream is) {
            String rLine = "";
            StringBuilder answer = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            try {
                while ((rLine = br.readLine()) != null) {
                    answer.append(rLine);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return answer;
        }
    }
    private int returnParsedJsonObject(String result){

        JSONObject resultObject = null;
        int returnedResult = 0;
        try {
            resultObject = new JSONObject(result);
            returnedResult = resultObject.getInt("success");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnedResult;
    }
}