package com.sungazerstudios.MicroMouse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class FrontEndActivity extends Activity implements SensorEventListener {
	private final int UPDATE_FREQ = 100; //number of times per second to send over network
	private final String LOG_TAG = "FrontEndActivity";
	private final String TARGET_URL = "192.168.10.150";
	private final int TARGET_PORT = 8080;
	//private final String BULLSHIT_SERVICE = "com.sungazerstudios.MicroMouse.BullShitService";
	float[] Rot = new float[9];
	float[] Inc = new float[9];
	float[] mOrient = { 0.0f, 0.0f, 0.0f };
	float[] mAccelVec = { 0.0f, 0.0f, 0.0f };
	float[] mMagVec = { 0.0f, 0.0f, 0.0f };
	Sensor mAccel;
	Sensor mMag;
	SensorManager mSensorManager;
	long prevEventTime = 0;
	Socket mSocket = null;
	PrintWriter toServer = null;
	TextView FrontAndCenter;
	TextView ConnectionStatus;
	Button LeftClick;
	Button RightClick;
	boolean hasConnection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_front_end);
		FrontAndCenter = (TextView) findViewById(R.id.FrontAndCenter);
		ConnectionStatus = (TextView) findViewById(R.id.ConnectionStatus);
		LeftClick = (Button) findViewById(R.id.LeftClick);
		RightClick = (Button) findViewById(R.id.RightClick);
		
		final Button button = (Button) findViewById(R.id.ResetConnection);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	toServer.close();
            	try {
					mSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
                setUpComms();
                
            	
            }
        });
        
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
			mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
		setUpComms();
		engageOrientationThread();		
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccel,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mMag,
				SensorManager.SENSOR_DELAY_GAME);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		/*
		try {
			oThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			
		}*/
	}
	
	protected void onDestroy() {
		super.onDestroy();
		oThread.stop();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_front_end, menu);
		return true;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mAccelVec = event.values;
		} else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mMagVec = event.values;
		}
		prevEventTime = event.timestamp;
	}
	/**
	 * Sends the orientation data to the server.
	 */
	Thread oThread;
	protected void engageOrientationThread() {
		oThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					Log.d(LOG_TAG, "Calculating Orientation");
					SensorManager.getRotationMatrix(Rot, Inc, mAccelVec,
							mMagVec);
					SensorManager.getOrientation(Rot, mOrient);
					// print to screen
					FrontAndCenter.post(new Runnable() {
						public void run() {
							DecimalFormat df = new DecimalFormat("#.######");
							String display = "";
							for (float i : mOrient) {
								display += df.format(i) + ", ";
							}
							FrontAndCenter.setText(display);
							ConnectionStatus.setText("Sending data: " + hasConnection);
						}
					});
					if (hasConnection) {
						Log.d(LOG_TAG, "Sending orientation data");
						giveMyOrientation();
					}

					// troll around until the end of time
					try {
						Thread.sleep(1000 / UPDATE_FREQ);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		oThread.start();
	}

	protected void setUpComms() {
		try {
			mSocket = new Socket(TARGET_URL, TARGET_PORT);
			toServer = new PrintWriter(mSocket.getOutputStream(), true);
			hasConnection = true;
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host.");
			hasConnection = false;
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to host. ");
			hasConnection = false;
		}
	}

	protected void giveMyOrientation() {
		int l = 0;
		int r = 0;
		if (LeftClick.isPressed()){
			l = 1;
		}
		if (RightClick.isPressed()){
			r = 1;
		}
		toServer.println(mOrient[0] + "," + mOrient[1] + "," + l + "," + r);
	}
}
