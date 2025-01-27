/*
 * Released under MIT License http://opensource.org/licenses/MIT
 * Copyright (c) 2013 Plasty Grove
 * Refer to file LICENSE or URL above for full text 
 */

package br.com.rescue_bots_android.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import br.com.rescue_bots_android.ConfigActivity;
import br.com.rescue_bots_android.R;
import br.com.rescue_bots_android.RouteActivity;
import br.com.rescue_bots_android.controller.RouteController;
import br.com.rescue_bots_android.controller.TrackerController;
import br.com.rescue_bots_android.gps.GPSTrackerImplementation;
import br.com.rescue_bots_android.gps.GPSTrackerInterface;
import br.com.rescue_bots_android.math.MathEngine;
import br.com.rescue_bots_android.socket.Client;
import br.com.rescue_bots_android.socket.Server;
import br.com.rescue_bots_android.socket.v2.ClienteSocket;
import br.com.rescue_bots_android.sqlite.entity.Path;
import br.com.rescue_bots_android.ui.ActivityList.AboutScreen;
import br.com.rescue_bots_control.JoystickView;
import br.com.rescue_bots_control.JoystickView.OnJoystickMoveListener;
import br.com.rescuebots.pojo.Tracker;

public class MainActivity extends Activity  implements GPSTrackerInterface,SensorEventListener{

	private static final String TAG = "BlueTest5-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	private BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;

	private boolean mIsUserInitiatedDisconnect = false;

	// All controls here
	private TextView mTxtReceive;
	private EditText mEditSend;
	private Button mBtnDisconnect;
	private Button mBtnSend;
	private Button mBtnClear;
	private Button mBtnClearInput;
	private Button mBtnConfig;
	private ImageButton ibClaw;
	private ScrollView scrollView;
	private CheckBox chkScroll;
	private CheckBox chkReceiveText;
	private CheckBox checkBoxJoystickEnable;
	private JoystickView joystickView;
	private TextView editTextAngle;

	private boolean mIsBluetoothConnected = false;

	private BluetoothDevice mDevice;

	private ProgressDialog progressDialog;
	
	
	public static final String DEVICE_EXTRA = "com.blueserial.SOCKET";
	public static final String DEVICE_UUID = "com.blueserial.uuid";
	private static final String DEVICE_LIST = "com.blueserial.devicelist";
	private static final String DEVICE_LIST_SELECTED = "com.blueserial.devicelistselected";
	public static final String BUFFER_SIZE = "com.blueserial.buffersize";
	public static final String BLUE_SOCKET = "com.blueserial.BLUESOCKET";
	public GPSTrackerImplementation gps;
	
	private RouteController routeController;
	private TrackerController trackerController = null;
	private List<Path> coordinates;
	private boolean joystickEnabled = false;
	private boolean vunforiaMode = false;
	
	ClawControll clawControll;
	
	 // record the compass picture angle turned
	    private float currentDegree = 0f;
	    // device sensor manager
	    private SensorManager mSensorManager;
	    private ImageView image;

	    //Server server;
	    Client myClient ;
	    
	    private String robottype; 
	    private float accuraccy = 10f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main_blue);
		
		
		
		ActivityHelper.initialize(this);
		
		routeController = new RouteController(this);
		coordinates = new ArrayList<Path>();
		
		
		trackerController = new TrackerController(this);
		
		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);

		Log.d(TAG, "Ready");

		mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);
		mBtnSend = (Button) findViewById(R.id.btnSend);
		mBtnClear = (Button) findViewById(R.id.btnClear);
		mTxtReceive = (TextView) findViewById(R.id.txtReceive);
		mEditSend = (EditText) findViewById(R.id.editSend);
		scrollView = (ScrollView) findViewById(R.id.viewScroll);
		chkScroll = (CheckBox) findViewById(R.id.chkScroll);
		chkReceiveText = (CheckBox) findViewById(R.id.chkReceiveText);
		checkBoxJoystickEnable = (CheckBox) findViewById(R.id.checkBoxJoystickEnable);
		mBtnClearInput = (Button) findViewById(R.id.btnClearInput);
		mBtnConfig = (Button) findViewById(R.id.buttonConfig2);
		//imageButtonClaw (Button)findViewById(R.id.imageButtonClaw);
		editTextAngle = (TextView) findViewById(R.id.editTextAngle);
		joystickView = (JoystickView) findViewById(R.id.viewJoystick);
		//Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
	    ibClaw = (ImageButton) findViewById(R.id.imageButtonClaw);
	    clawControll= new ClawControll(ibClaw);
	    
	    
	    
	    
	    
		joystickView.setOnJoystickMoveListener(new OnJoystickMoveListener() {

            @Override
            public void onValueChanged(int angle, int power, int direction) {
                // TODO Auto-generated method stub
            	mTxtReceive.append(" " + String.valueOf(angle) + "°");
            	mTxtReceive.append(" " + String.valueOf(power) + "%");
                switch (direction) {
                case JoystickView.FRONT:
                	mTxtReceive.append("FRONT");
                	sendSerial("a");
                    break;
                case JoystickView.FRONT_RIGHT:
                	mTxtReceive.append("FRONT_RIGHT");
                	sendSerial("c");
                    break;
                case JoystickView.RIGHT:
                	mTxtReceive.append("RIGHT");
                	sendSerial("c");
                    break;
                case JoystickView.RIGHT_BOTTOM:
                	mTxtReceive.append("RIGHT_BOTTOM");
                	sendSerial("f");
                    break;
                case JoystickView.BOTTOM:
                	mTxtReceive.append("BOTTOM");
                	sendSerial("d");
                    break;
                case JoystickView.BOTTOM_LEFT:
                	mTxtReceive.append("BOTTOM_LEFT");
                	sendSerial("e");
                    break;
                case JoystickView.LEFT:
                	mTxtReceive.append("LEFT");
                	sendSerial("b");
                    break;
                case JoystickView.LEFT_FRONT:
                	mTxtReceive.append("LEFT_FRONT");
                	sendSerial("b");
                    break;
                default:
                	mTxtReceive.append("CENTER");
                	sendSerial("g");
                }
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);
		
		
		
		mTxtReceive.setMovementMethod(new ScrollingMovementMethod());

		mBtnDisconnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mIsUserInitiatedDisconnect = true;
				new DisConnectBT().execute();
			}
		});

		mBtnSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					mBTSocket.getOutputStream().write(mEditSend.getText().toString().getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		mBtnClear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mEditSend.setText("");
			}
		});
		
		mBtnClearInput.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//mTxtReceive.setText("");
				if(mBtnClearInput.getText().toString().equalsIgnoreCase(LocationManager.GPS_PROVIDER)){
					mBtnClearInput.setText(LocationManager.NETWORK_PROVIDER);
					gps.setProvider(LocationManager.NETWORK_PROVIDER);
				}else{
					mBtnClearInput.setText(LocationManager.GPS_PROVIDER);
					gps.setProvider(LocationManager.GPS_PROVIDER);
				}
			}
		});
		mBtnConfig.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this,ConfigActivity.class);
				startActivity(i);
			}
		});
		
		
		
		ibClaw.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(clawControll!=null){
					clawControll.changeState();
				}
			}
		});
		checkBoxJoystickEnable.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(checkBoxJoystickEnable.isChecked()){
					joystickEnabled = true;
					joystickView.setEnabled(true);
					ibClaw.setEnabled(true);
				}else{
					joystickEnabled = false;
					joystickView.setEnabled(false);
					ibClaw.setEnabled(false);
				}
			}
		});
		joystickView.setEnabled(true);
		ibClaw.setEnabled(true);
		
		initGPSListener();
		
		// initialize your android device sensor capabilities
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		        image = (ImageView) findViewById(R.id.compassImg);     


	}
	
	
	public void routeOnClick(View view){
		Location l = gps.getLocation();
		Intent intent = new Intent(this,RouteActivity.class);
		intent.putExtra("latitude", (l!=null)?l.getLatitude():"");
		intent.putExtra("longitude", (l!=null)?l.getLongitude():"");
		startActivity(intent);
	}
	
	
	
	
	public void initGPSListener(){
		 // create class object
		gps = new GPSTrackerImplementation(this);
	    // check if GPS enabled     
	     if(gps.canGetLocation()){
	  	  String locationText = getLocationText(gps.getLocation());
	 	   if(locationText!=null){
	 		  Log.i("INFO",locationText);
	 	   }else{
	 		  Log.i("INFO","Sem coordenada");
	 	   }
	    }else{
	  	  // can't get location
	        // GPS or Network is not enabled
	        // Ask user to enable GPS/network in settings
	        gps.showSettingsAlert();
	    }
	     if(mBtnClearInput.getText().toString().equalsIgnoreCase(LocationManager.GPS_PROVIDER)){
	    	 mBtnClearInput.setText(LocationManager.NETWORK_PROVIDER);
				gps.setProvider(LocationManager.NETWORK_PROVIDER);
	     }else{
	    	 mBtnClearInput.setText(LocationManager.GPS_PROVIDER);
				gps.setProvider(LocationManager.GPS_PROVIDER);
	     }
	}
	public void stopGPSListener(){
		
   // check if GPS enabled     
	 	   gps.stopUsingGPS();
	 	  Log.i("INFO","GPS stoped");
	 	  
	   
	}
	
	@Override
	    public void onSensorChanged(SensorEvent event) {
	        // get the angle around the z-axis rotated
	        float degree = Math.round(event.values[0]);
	        //tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
	        // create a rotation animation (reverse turn degree degrees)
	        RotateAnimation ra = new RotateAnimation(
	                currentDegree,
	                degree,
	                Animation.RELATIVE_TO_SELF, 0.5f,
	                Animation.RELATIVE_TO_SELF,
	                0.5f);
	        // how long the animation will take place
	        ra.setDuration(210);
	        // set the animation after the end of the reservation status
	        ra.setFillAfter(true);
	        // Start the animation
	        image.startAnimation(ra);
	        currentDegree = degree;
	       
	     
	        editTextAngle.setText("AngleM : "+currentDegree + " DistM : " +distance + " GPSDir " + gpsDirection + " Dif " + diference + " Ponto " + index);
		     
	    }
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	        // not in use
	    }

	
	    private GeomagneticField geoField;
	    private float myBearing =0;
	    private float heading = 0;
	    private int gpsDirection = 0;
	    private float diference = 0;
	    private Location origin = null;
	    
	    
	    
	    
	    
	    
	    
	    
	    private static final int TWO_MINUTES = 1000 * 60 * 2;

	    /** Determines whether one Location reading is better than the current Location fix
	      * @param location  The new Location that you want to evaluate
	      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	      */
	    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	        if (currentBestLocation == null || location == null) {
	            // A new location is always better than no location
	            return true;
	        }

	        // Check whether the new location fix is newer or older
	        long timeDelta = location.getTime() - currentBestLocation.getTime();
	        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	        boolean isNewer = timeDelta > 0;

	        // If it's been more than two minutes since the current location, use the new location
	        // because the user has likely moved
	        if (isSignificantlyNewer) {
	            return true;
	        // If the new location is more than two minutes older, it must be worse
	        } else if (isSignificantlyOlder) {
	            return false;
	        }

	        // Check whether the new location fix is more or less accurate
	        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	        boolean isLessAccurate = accuracyDelta > 0;
	        boolean isMoreAccurate = accuracyDelta < 0;
	        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	        // Check if the old and new location are from the same provider
	        boolean isFromSameProvider = isSameProvider(location.getProvider(),
	                currentBestLocation.getProvider());

	        // Determine location quality using a combination of timeliness and accuracy
	        if (isMoreAccurate) {
	            return true;
	        } else if (isNewer && !isLessAccurate) {
	            return true;
	        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	            return true;
	        }
	        return false;
	    }

	    /** Checks whether two providers are the same */
	    private boolean isSameProvider(String provider1, String provider2) {
	        if (provider1 == null) {
	          return provider2 == null;
	        }
	        return provider1.equals(provider2);
	    }

	    
	    
	    
	    
	    
	    
	    
	    
	@Override
	public void notifyLocation(Location location) {
		 if(joystickEnabled){ //modo joystick habilitado
			 return;
		 }
		 if(vunforiaMode){
			 return;
		 }
		 if(location.getAccuracy()>=accuraccy){
			 
			 mTxtReceive.append(location.getAccuracy() + " -> accuracy is menor than -> " +accuraccy);
			 return;
		 }else{
			 mTxtReceive.append("Accuracy -> " +location.getAccuracy());
			 //accuraccy = location.getAccuracy();
			 /*if(!isBetterLocation(origin, location)){
				 mTxtReceive.append("Not is better -> " +location.getAccuracy());
				 return;
			 }*/
		 }
			 
		 String locationText = getLocationText(gps.getLocation());
		 //if(gps.getLocation()!=null){
		//	 trackerController.inserir(fillLocationBean(location));
		 //}
	  	   if(locationText!=null){
	  		 Log.i("INFO",locationText);
	  	   }else{
	  		 Log.i("INFO","Sem coordenada");
	  	   }
	  	 //SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
	  	 //String serverIP = settings.getString(ConfigActivity.PARAM_SERVERIP , "");
	     //String serverPort = settings.getString(ConfigActivity.PARAM_SERVERPORT , "");
	     
	     StringBuilder message = new StringBuilder();
	     message.append(locationText);
	     /*if(isFinishing()){
	    	 message.append("YES;"); //ok i get
	     }else{
	    	 message.append("NO;"); //found
	     }*/
	     //if(origin==null){
	    	 origin = new Location( gps.getLocation());
	     //}
	     
	     geoField = new GeomagneticField(
	             Double.valueOf(location.getLatitude()).floatValue(),
	             Double.valueOf(location.getLongitude()).floatValue(),
	             Double.valueOf(location.getAltitude()).floatValue(),
	             System.currentTimeMillis()
	          );
	     
	     
	     
	     myBearing += geoField.getDeclination();
	     heading = myBearing - (myBearing + heading); 
	     gpsDirection = Math.round (-heading / 360 + 180);
	     
	     if(origin!=null && destiny!=null){
		     //Location dest = new Location("dest");
		     //dest.setLatitude(destiny[0]);
		     //dest.setLongitude(destiny[1]);
		     distance = meterDistanceBetweenPoints(origin.getLatitude(),origin.getLongitude(), destiny[0],destiny[1]);
	     }else{
	    	 distance = -1;
	     }
	     if(distance>1){ //distancia in mater around
	    	 message.append("NO"); //found
	     }else{
	    	 if(distance>=0 && distance<=1){
	    		 message.append("YES"); //ok i get
		    	 destiny = null;
		    	 sendSerial("g");
	    	 }else{
	    		 message.append("NO"); //found
	    	 }
	    	
	    	
	     }
		 SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
	  	 String serverIP = settings.getString(ConfigActivity.PARAM_SERVERIP , "");
	     String serverPort = settings.getString(ConfigActivity.PARAM_SERVERPORT , "");
	     
	     String robotId = settings.getString(ConfigActivity.PARAM_ROBOTID , "");
	     robottype = settings.getString(ConfigActivity.PARAM_BOTTYPE , "");
	     message.append(";"+robottype);
	     message.append(";"+robotId); //found
	     
	     
	     message.append(";"+currentDegree);
	     message.append(";"+gpsDirection);
	     message.append(";"+distance);
	     message.append(";"+diference);
		 message.append(";"+index);
	     message.append(";"+lastmessage);
	     message.append(";"+foundsucess);
	     
	   
	     
	     if(sendToServer(serverIP,serverPort, message,robotId)){ 
	    	 message.append("RESCUEBOT CLIENT at "+ serverIP + ":" +serverPort +"ONLINE =)");	
	     }else{
	    	 message.append("<font color=red>RESCUEBOT CLIENT at "+ serverIP + ":" +serverPort +"OFFLINE =(</font>");	
	     }
	}
	
	
	public void selectRouteFromServices(List<Path> coordinates) {
		// Instantiate the RequestQueue.
		//RequestQueue queue = Volley.newRequestQueue(this);
		SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
	  	String server = settings.getString(ConfigActivity.PARAM_SERVERIP , "");
	  	String port = "8083";//settings.getString(ConfigActivity.PARAM_SERVERPORT , "");
	  	//
		String url ="http://" + server + ":" + port +"/rescuebots_web/CommandServlet?method=getsaviourfoundcoordinate";
		new RequestTask().execute(url);
	
	}
	public void updateRouteFromServices() {
		// Instantiate the RequestQueue.
		//RequestQueue queue = Volley.newRequestQueue(this);
		SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
	  	String server = settings.getString(ConfigActivity.PARAM_SERVERIP , "");
	  	String port = "8083";//settings.getString(ConfigActivity.PARAM_SERVERPORT , "");
	  	String robotid = settings.getString(ConfigActivity.PARAM_ROBOTID , "");
	  	//
		String url ="http://" + server + ":" + port +"/rescuebots_web/CommandServlet?method=update&robotid="+robotid;
		new RequestTask().execute(url);
	
	}
	
	@SuppressLint("NewApi")
	public boolean sendToServer(String serverIP, String serverPort, StringBuilder message,String robotId){
		if(serverIP!=null && serverPort!=null && !serverIP.equalsIgnoreCase("") && !serverPort.equalsIgnoreCase("")){
			try {
				myClient = new Client(
						serverIP, 
						Integer.parseInt(serverPort), 
						message.toString(),
						mTxtReceive);
				//myClient = new ClienteSocket(mTxtReceive,robotId,serverIP,Integer.parseInt(serverPort));
				//String usuario = "jalmeida";
				//myClient.initClient(serverIP, Integer.parseInt(serverPort) , robotId);
				if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
					//myClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"CONNECT");
					myClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,message.toString());
					
				}else{
					myClient.execute(message.toString());
					//myClient.execute("CONNECT");
				}
				return true;
			} catch (Exception e) {
				Log.e(this.getLocalClassName(),e.getLocalizedMessage(),e);
			}
		}
	
		return false;
	}
		/*
	@SuppressLint("NewApi")
	public boolean sendToServer(String serverIP, String serverPort, StringBuilder message){
		if(serverIP!=null && serverPort!=null && !serverIP.equalsIgnoreCase("") && !serverPort.equalsIgnoreCase("")){
			try {
				myClient = new Client(
						serverIP, 
						Integer.parseInt(serverPort), 
						message.toString(),
						mTxtReceive);
				
				//myClient.executeOnExecutor(exec, params)();
				if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
					myClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"CONNECT");
				}else{
					myClient.execute();
				}
				return true;
			} catch (Exception e) {
				Log.e(this.getLocalClassName(),e.getLocalizedMessage(),e);
			}
		}
	
		return false;
	}*/
	public Tracker fillLocationBean(Location location){
		Tracker t = new Tracker();
		t.setAccuracy(String.valueOf(location.getAccuracy()) );
		t.setAltitude(String.valueOf( location.getAltitude() ));
		t.setEaring( String.valueOf(location.getBearing() ));
		t.setLatitude(String.valueOf(location.getLatitude() ));
		t.setLongitude(String.valueOf(location.getLongitude() ));
		t.setProvider( location.getProvider() );
		t.setSpeed( String.valueOf(location.getSpeed() ));
		t.setTime( String.valueOf(location.getTime()));
		return t;
	}
	private double meterDistanceBetweenPoints(double lat_a, double lng_a, double lat_b, double lng_b) {
		double pk = (double) (180.f/Math.PI);

		double a1 = lat_a / pk;
		double a2 = lng_a / pk;
		double b1 = lat_b / pk;
		double b2 = lng_b / pk;

		double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
		double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
		double t3 = Math.sin(a1)*Math.sin(b1);
	    double tt = Math.acos(t1 + t2 + t3);

	    return 6366000*tt;
	}
	public String getLocationText(Location location){
		if(location!=null){
			StringBuilder out = new StringBuilder();
			out.append("0;");
			out.append(location.getAccuracy() + ";");
			out.append(location.getAltitude() + ";");
			out.append(location.getBearing() + ";");
			out.append(location.getLatitude() + ";");
			out.append(location.getLongitude() + ";");
			out.append(location.getProvider() + ";");
			out.append(location.getSpeed() + ";");
			out.append(location.getTime() + ";");
			
			//controller.inserir(fillLocationBean(location));
			return out.toString();	
		}else{
			return "No Location";
		}
		
	}
	
	private int index = 0;
	private boolean finishFouding = false; 
	private double distance = -1;
	private Double[] destiny = null;
	
	public void automatic(){
		if(gps.getLocation()!=null && destiny!=null){
			Location location = gps.getLocation();
			Double[] current = new Double[]{location.getLatitude(),location.getLongitude()};
			
			
			String d = direction(current, destiny);
			//distance = MathEngine.getDistanceD(current, destiny);
			
			//int volume = (distance>0)?100/(int)distance:0;
			
			//ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, volume);             
			//toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);  
			
			if(distance>1){
				//tone.startTone(ToneGenerator.TONE_CDMA_PIP,150); 
				sendSerial(d);
			}else{
				finishFouding = false;
				try {
					sendSerial("b");
					Thread.sleep(4000);
					if(robottype!=null && robottype.equalsIgnoreCase("saviour")){
						updateRouteFromServices();
						coordinates = new ArrayList<Path>();
						destiny = null;
						
					}else{
						
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//finishFouding = false;
				//ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
				//tone.startTone(ToneGenerator.TONE_CDMA_PIP,1050); 
				///finishFouding = true;
			}
		
		}else{
			sendSerial("g");
		}
		//mTxtReceive.append("current location......: " +  current);
		//mTxtReceive.append("destiny location......: " +  destiny);
		//mTxtReceive.append("serial location.......: " +  d);
		//mTxtReceive.append("serial index..........: " + index);
		//mTxtReceive.append("serial finishFouding..: " + finishFouding);
		
	}
	
	public String direction(Double[] center, Double[] destiny){
		//if (destiny != null) {
			//double angle = MathEngine.getDirection(center, destiny);
		    
			///double dif = Double.parseDouble(""+ currentDegree) - angle;
			//Log.i("INFO",Double.parseDouble(""+ currentDegree) + "-" + angle+"="+dif);
		    diference = (currentDegree>gpsDirection)?currentDegree-gpsDirection:currentDegree+gpsDirection;
		    //diference   = Math.abs( currentDegree-gpsDirection );
		   
			if(diference>180 && diference<315){
				//direita
				return "c";
			}else if(diference>45 && diference<=180){
				//esquerda
				return "b";
			}else{
				//frente
				return "a";
			}
		//}
		//return null;
	}
	
	//List<Double[]> coordinates = new ArrayList<Double[]>(){{
	//	   add(new Double[]{-22.65267667,-50.39065073});
		  // add(new Double[]{-22.65267369,-50.39065192});
		  // add(new Double[]{-22.65267311,-50.39065371});
		  // add(new Double[]{-22.65267087,-50.39065442});
		  // add(new Double[]{-22.65267369,-50.39065192});
	//	}};
	
	
	
	public void autoOnClick(View view){
		
		
		
		/* mSensorManager.unregisterListener(this);
			if (mBTSocket != null && mIsBluetoothConnected) {
				new DisConnectBT().execute();
			}
			vunforiaMode = true;
		*/
		  Intent intent = new Intent(this, AboutScreen.class);
	        intent.putExtra("ABOUT_TEXT_TITLE", "User Defined Targets");
		
		  
		  intent.putExtra("ACTIVITY_TO_LAUNCH",
                  "app.UserDefinedTargets.UserDefinedTargets");
              intent.putExtra("ABOUT_TEXT",
                  "UserDefinedTargets/UD_about.html");
		 
	        
			intent.putExtra(DEVICE_EXTRA, mDevice);
			intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
			intent.putExtra(BUFFER_SIZE, mMaxChars);
			//intent.putExtra(BLUE_SOCKET,mBTSocket);
			
		  
		  
		 // startActivity(intent);
		  
		 
			startActivity(intent);
	}
	private static String lastmessage = "";
	private void sendSerial(String message){
		try {
			if(mBTSocket!=null && serialSender==true && !lastmessage.equalsIgnoreCase(message)){
				mBTSocket.getOutputStream().write(message.getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			
		}
	}
	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() {
			return t.isAlive();
		}
		String oldMessage = "";
		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
					byte[] buffer = new byte[256];
					if (inputStream.available() > 0) {
						inputStream.read(buffer);
						int i = 0;
						/*
						 * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
						 */
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
						}
						final String strInput = new String(buffer, 0, i);

						/*
						 * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
						 */

						if (chkReceiveText.isChecked()) {
							mTxtReceive.post(new Runnable() {
								@Override
								public void run() {
									if(oldMessage!=null && strInput!=null && !oldMessage.equalsIgnoreCase(strInput)){
										mTxtReceive.append(strInput);
										oldMessage = strInput;
									}
									//Uncomment below for testing
									//mTxtReceive.append("\n");
									//mTxtReceive.append("Chars: " + strInput.length() + " Lines: " + mTxtReceive.getLineCount() + "\n");
									
									int txtLength = mTxtReceive.getEditableText().length();  
									if(txtLength > mMaxChars){
										mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
									}

									if (chkScroll.isChecked()) { // Scroll only if this is checked
										scrollView.post(new Runnable() { // Snippet from http://stackoverflow.com/a/4612082/1287554
													@Override
													public void run() {
														scrollView.fullScroll(View.FOCUS_DOWN);
													}
												});
									}
								}
							});
						}

					}
					Thread.sleep(500);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	
		public void stop() {
			bStop = true;
		}

	}

	private class DisConnectBT extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (mReadThread != null) {
				mReadThread.stop();
				while (mReadThread.isRunning())
					; // Wait until it stops
				mReadThread = null;

			}

			try {
				mBTSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect) {
				finish();
			}
		}

	}
	class RequestTask extends AsyncTask<String, String, Void>{
        
	    @Override
	    protected Void doInBackground(String... uri) {
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpResponse response;
	        String responseCoordinate = null;
	        try {
	        	boolean found = true;
	        	while(found){
	        		   response = httpclient.execute(new HttpGet(uri[0]));
	   	            StatusLine statusLine = response.getStatusLine();
	   	            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	   	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	   	                response.getEntity().writeTo(out);
	   	                responseCoordinate = out.toString();
	   	                out.close();
	   	                try {
	   	                	String[] params =null;
	   	                	if(responseCoordinate!=null && !responseCoordinate.equalsIgnoreCase("")){
	   	                		params = responseCoordinate.split(";");
	   	                		destiny = new Double[]{Double.parseDouble(params[0]), Double.parseDouble(params[1])};
	   	                		Path bean = new Path();
		   	             		bean.setLatitude(params[0]);
		   	             		bean.setLongitude(params[1]);
		   	             		bean.setRobotId("");
		   	             		bean.setFound("NO");
	   	                		coordinates.add(bean);
	   	                		found = false;
	   	                	}
							Thread.sleep(2000);
							
						} catch (InterruptedException e) {
							Log.e("ERROR", e.getMessage());
						}
	   	            } else{
	   	                //Closes the connection.
	   	            	Log.e("ERROR", "Not conection found");
	   	                response.getEntity().getContent().close();
	   	                throw new IOException(statusLine.getReasonPhrase());
	   	            }
	        	}
	         
	        } catch (ClientProtocolException e) {
	        	Log.e("ERROR", e.getMessage());
	        } catch (IOException e) {
	        	Log.e("ERROR", e.getMessage());
	        }
			return null;
	        
	    }

	    @Override
	    protected void onPostExecute(Void result) {
	        super.onPostExecute(result);
	        //Do anything with response..
	    }
	}
	private void msg(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPause() {
		// to stop the listener and save battery
        mSensorManager.unregisterListener(this);
		if (mBTSocket != null && mIsBluetoothConnected) {
			new DisConnectBT().execute();
		}
		Log.d(TAG, "Paused");
		super.onPause();
		
	

	}

	@Override
	protected void onResume() {
		
		
		
		SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
		robottype = settings.getString(ConfigActivity.PARAM_BOTTYPE , "");
		if(robottype!=null && robottype.equalsIgnoreCase("FLAREGUN")){
			routeController.selectAll(coordinates);
		}else if(robottype!=null && robottype.equalsIgnoreCase("SAVIOUR")){
			selectRouteFromServices(coordinates);
		}
		accuraccy = settings.getInt(ConfigActivity.PARAM_ACCURACCY , 10);
		
		
		Log.d(TAG, "Resumed");
		super.onResume();
		
		 // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
		
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}

	}

	@Override
	protected void onStop() {
		Log.d(TAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	public static final int VULFORIA_REQUEST = 230;
	public static final int VULFORIA_RESULT = 231;
	public boolean serialSender = true;
	public boolean foundsucess = false;
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	//if(resultCode==VULFORIA_REQUEST){
    	
    	serialSender = true;
    	
    	 if (requestCode == VULFORIA_REQUEST) {
    	       // if(resultCode == Activity.RESULT_OK){
    	        	foundsucess = true; //data.getBooleanExtra("foundsucess",false);
    	        	 //if(foundsucess){
    	        		 sendSerial("g");
    	        		 serialSender = false;
    	        		 vunforiaMode = false;
    	        		 
    	        	 //}else{
    	        	//	 index= index +1;
    	        	//	 serialSender = true;
    	        	 //}
    	       // }
    	 }
    	
    }
	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			if(progressDialog==null){
			progressDialog = ProgressDialog.show(MainActivity.this, "Aguarde por favor", "Conectando");// http://stackoverflow.com/a/11130220/1287554
			}
		}

		@SuppressLint("NewApi")
		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "Não é possível comunicação ao dispositivo. Qual o serial device? Também cheque se o UUID está correto nas opções", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
				
				new AutonomyAsyncTask().execute();
				
			}
			if(progressDialog!=null){
			progressDialog.dismiss();
			}
			
			 
		}

	}
	
	public class AutonomyAsyncTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected void onPreExecute() {
			
		}
		@Override
		protected Void doInBackground(Void... params) {
			
			for (int i = 0; i < coordinates.size(); i++) {
				//Double[] current = null;
				
				
				if(destiny==null && coordinates!=null && coordinates.size()>0){
					destiny = coordinates.get(0).toCoordinate();
					index = index+1;
					finishFouding = false;
				}else if(index<coordinates.size()-1){
					destiny = coordinates.get(index).toCoordinate();
					finishFouding = false;
				}else if(index>=coordinates.size()-1){
					destiny = coordinates.get(coordinates.size()-1).toCoordinate();
					finishFouding = false;
				}else{
					index = 0;
					finishFouding = true;
				}
				
				  while(!finishFouding && mIsBluetoothConnected){
				    	 automatic();
				    	 try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				     }
				
				
			}
			 
			return null;
		}
		
		
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(coordinates!=null && coordinates.size()>0){
				if(progressDialog!=null){
					progressDialog.dismiss();
					}
					
				//mIsUserInitiatedDisconnect = true;
				//new DisConnectBT().execute();
				/*serialSender = false;
					
				 Intent intent = new Intent(MainActivity.this, AboutScreen.class);
			        intent.putExtra("ABOUT_TEXT_TITLE", "User Defined Targets");
				
				  
				  intent.putExtra("ACTIVITY_TO_LAUNCH",
		                  "app.UserDefinedTargets.UserDefinedTargets");
		              intent.putExtra("ABOUT_TEXT",
		                  "UserDefinedTargets/UD_about.html");
				 
					
					intent.putExtra(DEVICE_EXTRA, mDevice);
					intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
					intent.putExtra(BUFFER_SIZE, mMaxChars);
					startActivityForResult(intent, VULFORIA_REQUEST);
				  
				  */
				 
			}
			
		}
	}

	/**
	 * Classe para controle da garra acoplada ao robo
	 * Estado aberto ou fechado enviando o paramentro
	 * i - Para fechar a garra
	 * h - Para fechar a garra
	 * @author nova3d-macmini03
	 *
	 */
	public class ClawControll {
		public ImageButton button;
		public static final String STATE_CLOSE = "i";
		public static final String STATE_OPEN = "h";
		
		public String clawstate = STATE_CLOSE; //close is default
		public ClawControll(ImageButton button){
			this.button = button;
		}
		public void changeState(){
			if(clawstate==STATE_CLOSE){
				clawstate = STATE_OPEN;
				button.setImageResource(R.drawable.hand_open);
			}else if(clawstate==STATE_OPEN){
				clawstate = STATE_CLOSE;
				button.setImageResource(R.drawable.hand_close);
			}
			sendSerial(clawstate);
		}
	}
}
