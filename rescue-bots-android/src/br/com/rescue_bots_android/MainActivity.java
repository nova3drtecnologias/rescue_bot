package br.com.rescue_bots_android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import br.com.rescue_bots_android.controller.TrackerController;
import br.com.rescue_bots_android.gps.GPSTrackerImplementation;
import br.com.rescue_bots_android.gps.GPSTrackerInterface;
import br.com.rescue_bots_android.socket.Client;
import br.com.rescue_bots_android.socket.Server;
import br.com.rescuebots.pojo.Tracker;
import br.com.rescuebots.utils.Network;

public class MainActivity extends Activity implements GPSTrackerInterface {
	private TextView textViewLog = null;
	
	Server server;
	Client myClient ;
	GPSTrackerImplementation gps;
	TrackerController controller = null;
	Button btnDisconnect;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			
			controller = new TrackerController(this);
			
			
			textViewLog = (TextView)findViewById( R.id.textViewLog );
			
			
			appendLog("#########################################");
			appendLog("#########################################");
			appendLog("WELCOME TO RESCUEBOTs INTERFACE");
			appendLog("#########################################");
			appendLog("\n.");	
			
			try {
				 SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
				 
				 String botype = settings.getString(ConfigActivity.PARAM_BOTTYPE , "");
				 if(botype.equalsIgnoreCase("flaregun")){
					 appendLog("SAVIOUR");
				 }else{
					 if(botype.equalsIgnoreCase("saviour")){
						 appendLog("RESCUEBOT TYPE is ");	
						 appendLog("FLAREGUN");
					 }else{
						 appendLog("RESCUEBOT TYPE is UNDEFINED ");	
					 }
				 }
				 
				 
				 if(settings.getBoolean(ConfigActivity.PARAM_ISSERVER , false)){
					 server = new Server(this);
					 appendLog("RESCUEBOT IS SERVER at" +Network.getIpAddress()+":"+server.getPort());
				 }
				 
				 
			    
			     
			  
			     if(settings.getBoolean(ConfigActivity.PARAM_ISTRAKER , false)){
			    	 initGPSListener();
			     }else{
			    	 stopGPSListener();
			     }
			     
			     
			} catch (Exception e) {
				Log.e(this.getLocalClassName(),e.getLocalizedMessage(),e);
			}
			
			
		
	}
	
	
	public void initGPSListener(){
		 // create class object
		gps = new GPSTrackerImplementation(this);
     // check if GPS enabled     
      if(gps.canGetLocation()){
   	  String locationText = getLocationText(gps.getLocation());
  	   if(locationText!=null){
  		 appendLog(locationText);
  	   }else{
  		 appendLog("Sem coordenada");
  	   }
     }else{
   	  // can't get location
         // GPS or Network is not enabled
         // Ask user to enable GPS/network in settings
         gps.showSettingsAlert();
     }
	}
	public void stopGPSListener(){
		
    // check if GPS enabled     
	 	   gps.stopUsingGPS();
	 	   appendLog("GPS stoped");
	 	  
	   
	}
	@Override
	public void notifyLocation(Location location) {
		 String locationText = getLocationText(gps.getLocation());
	  	   if(locationText!=null){
	  		 appendLog(locationText);
	  	   }else{
	  		 appendLog("Sem coordenada");
	  	   }
	  	 SharedPreferences settings = this.getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
	  	 String serverIP = settings.getString(ConfigActivity.PARAM_SERVERIP , "");
	     String serverPort = settings.getString(ConfigActivity.PARAM_SERVERPORT , "");
	     String robotype = settings.getString(ConfigActivity.PARAM_BOTTYPE , "");
	     
	     StringBuilder message = new StringBuilder();
	     message.append(locationText);
	     message.append("NO;"); //found
	     
	     String robotId = settings.getString(ConfigActivity.PARAM_ROBOTID , "");
	     message.append(robotId); //found
	     message.append(";"+robotype);
	     if(sendToServer(serverIP,serverPort, message)){
	    	 appendLog("RESCUEBOT CLIENT at "+ serverIP + ":" +serverPort +"ONLINE =)");	
	     }else{
	    	 appendLog("<font color=red>RESCUEBOT CLIENT at "+ serverIP + ":" +serverPort +"OFFLINE =(</font>");	
	     }
	}
	public boolean sendToServer(String serverIP, String serverPort, StringBuilder message){
		try {
			myClient = new Client(
					serverIP, 
					Integer.parseInt(serverPort), 
					message.toString(),
					textViewLog);
			myClient.execute();
			return true;
		} catch (Exception e) {
			Log.e(this.getLocalClassName(),e.getLocalizedMessage(),e);
		}
		return false;
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
			
			controller.inserir(fillLocationBean(location));
			return out.toString();	
		}else{
			return "No Location";
		}
		
	}
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
	public boolean isServerOnline(String serverIP, String serverPort){
		try {
			myClient = new Client(
					serverIP, 
					Integer.parseInt(serverPort), 
					textViewLog);
			myClient.execute();
			return true;
		} catch (Exception e) {
			Log.e(this.getLocalClassName(),e.getLocalizedMessage(),e);
		}
		return false;
	}
	public void appendLog(String text){
		if(textViewLog!=null){
			textViewLog.append(Html.fromHtml(text));
			textViewLog.append("\n.");
		}else{
			Log.i(this.getLocalClassName(),"Não está pronto");
		}
	}
	public void btnConfigOnClick(View view){
		Intent intent = new Intent(this, ConfigActivity.class);
		startActivity(intent);
	}


	
	
	
}
