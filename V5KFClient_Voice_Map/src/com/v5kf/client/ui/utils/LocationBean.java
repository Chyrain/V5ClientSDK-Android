package com.v5kf.client.ui.utils;

import java.io.Serializable;

public class LocationBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double latitude;
	private double longitude;
	private double altitude;
	private float accuracy;
	private String name;
	private String address;
	
	public LocationBean() {
		// TODO Auto-generated constructor stub
	}
	
	public LocationBean(double lat, double lng) {
		this.latitude = lat;
		this.longitude = lng;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public double getAltitude() {
		return altitude;
	}
	
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	
	public float getAccuracy() {
		return accuracy;
	}
	
	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
}
