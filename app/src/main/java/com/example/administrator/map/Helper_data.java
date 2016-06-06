package com.example.administrator.map;

/**
 * Created by Administrator on 2016-06-07.
 */
public class Helper_data {
    private String title;
    private String lat;
    private String lng;

    public Helper_data(String title, String lat, String lng){

        this.title = title;
        this.lat = lat;
        this.lng = lng;

    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getTitle() {
        return title;
    }
}
