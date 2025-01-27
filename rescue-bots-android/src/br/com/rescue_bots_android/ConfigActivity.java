package br.com.rescue_bots_android;


import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import br.com.rescue_bots_android.bluetooth.Homescreen;
import br.com.rescue_bots_android.gps.GPSTrackerImplementation;
import br.com.rescuebots.utils.Network;

/**
 * Class de configuração do BOT, ela quem realiza a configuração do dispositivo
 * PARAM_SERVERIP = "server_ip" -> Define o ip do server, se o BOT for o proprio server ele coloca o IP local
 * PARAM_SERVERPORT = "server_port" -> Define a porta do server, se o BOT for um server ele define como default 8080
 * PARAM_BOTTYPE = "bot_type" -> Bot Type define o BOT como sendo FLAREGUN (Sinalizador) ou SAVIOUR (Resgatador)
 * PARAM_ISSERVER = "isserver" -> Define se um BOT será o MASTER, ou o BRAIN (Cerebro) ou o SERVER, se for verdadeiro ele
 * será encarregado de gerenciar os outros BOTs e monitorar seus possiveis trajetos ou TRACKLOG
 * @author nova3d-macmini03
 *
 */
public class ConfigActivity extends Activity {
	private EditText txtServerIP = null;
	private EditText txtServerPort = null;
	private RadioButton rbFlareGun = null;
	private RadioButton rbSaviour = null;
	private CheckBox checkServer = null;
	private ToggleButton tbGPSTracking = null;
	private TextView textViewRobotId = null;
	private EditText editTextAccuracy = null;
	
	public static final String PREFS_NAME = "ConfigPreferences";
	
	public static final String PARAM_SERVERIP = "server_ip";
	public static final String PARAM_SERVERPORT = "server_port";
	public static final String PARAM_BOTTYPE = "bot_type";
	public static final String PARAM_ISSERVER = "isserver";
	public static final String PARAM_ISTRAKER = "istraker";
	public static final String PARAM_ROBOTID = "robotid";
	public static final String PARAM_ACCURACCY = "accuraccy";
	/**
	 * Evento de criação e carregamento da tela de configurações
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		
		txtServerIP = (EditText) findViewById(R.id.editTextServerIP);
		txtServerPort = (EditText) findViewById(R.id.editTextServerPort);
		rbFlareGun = (RadioButton) findViewById(R.id.radioButtonFlareGun);
		rbSaviour = (RadioButton) findViewById(R.id.radioButtonSaviour);
		checkServer = (CheckBox) findViewById(R.id.checkBoxIsServer);
		textViewRobotId = (TextView) findViewById(R.id.textViewRobotId);
		//tbGPSTracking = (ToggleButton)findViewById(R.id.tbGPSTracking);
		tbGPSTracking = (ToggleButton) findViewById(R.id.tbGPSTracking);
		editTextAccuracy = (EditText) findViewById(R.id.editTextAccuracy);
		
		 SharedPreferences settings = ConfigActivity.this.getSharedPreferences(PREFS_NAME, 0);
		 txtServerIP.setText( settings.getString(ConfigActivity.PARAM_SERVERIP , "") );
		 txtServerPort.setText( settings.getString(ConfigActivity.PARAM_SERVERPORT, "") );
		 
		 String bottype = settings.getString(ConfigActivity.PARAM_BOTTYPE, "");
		
		 if(bottype !=null && bottype.equalsIgnoreCase("flaregun")){
			 rbFlareGun.setChecked(true);
			 rbSaviour.setChecked(false);
	      }else
	    	  if(bottype !=null && bottype.equalsIgnoreCase("saviour")){
	    		  rbFlareGun.setChecked(false);
	 			 rbSaviour.setChecked(true);
	    	  }else{
	    		  rbFlareGun.setChecked(false);
		 			 rbSaviour.setChecked(false);
	    	  }
	      
		 checkServer.setChecked(settings.getBoolean(ConfigActivity.PARAM_ISSERVER, false) );
		 tbGPSTracking.setChecked( settings.getBoolean(ConfigActivity.PARAM_ISTRAKER, false) );
		 //txtServerPort.setText(settings.getString(ConfigActivity.PARAM_ROBOTID, "") );
		 
		String address = getMacAddr();
		textViewRobotId.setText("ROBOTID ..: "+address);
		
		
		editTextAccuracy.setText(""+settings.getInt(ConfigActivity.PARAM_ACCURACCY , 10));
		/* SharedPreferences settings = ConfigActivity.this.getSharedPreferences(PREFS_NAME, 0);
  	      SharedPreferences.Editor editor = settings.edit();
  	      editor.putString(PARAM_ROBOTID , address);
  	      editor.commit();*/
		
		TextView.OnEditorActionListener editorListener = new TextView.OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				  if (actionId == EditorInfo.IME_NULL  
					      && event.getAction() == KeyEvent.KEYCODE_ENTER) { 
					      preferencesConfirm();
					   }
					   return true;
			}
		
		};
		
		txtServerIP.setOnEditorActionListener(editorListener);
		txtServerPort.setOnEditorActionListener(editorListener);
		
		
		
		tbGPSTracking.setOnCheckedChangeListener( new OnCheckedChangeListener() {
			        @Override
			        public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
			          SharedPreferences settings = ConfigActivity.this.getSharedPreferences(PREFS_NAME, 0);
			   	      SharedPreferences.Editor editor = settings.edit();
			   	      editor.putBoolean(PARAM_ISTRAKER , isChecked);
			   	      editor.commit();
			        }
			    }) ;
	}
	@SuppressLint("NewApi")
	public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
 
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
 
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }
 
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString().toUpperCase();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }
	/**
	 * Confirmação das preferencias
	 */
	private void preferencesConfirm(){
		
		
		
		
		  SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      
	      editor.putString(PARAM_SERVERIP , txtServerIP.getText().toString());
	      editor.putString(PARAM_SERVERPORT , txtServerPort.getText().toString());
	      
	      if(rbFlareGun.isChecked()){
	    	  editor.putString(PARAM_BOTTYPE, "flaregun");
	      }else{
	    	  if(rbSaviour.isChecked()){
	    		  editor.putString(PARAM_BOTTYPE , "saviour");
	    	  }
	      }
	      editor.putBoolean(PARAM_ISSERVER , checkServer.isChecked());
	      editor.putBoolean(PARAM_ISTRAKER , tbGPSTracking.isChecked());
	      String address = getMacAddr();
  	      editor.putString(PARAM_ROBOTID , address);
  	      editor.putInt(PARAM_ACCURACCY, Integer.parseInt(editTextAccuracy.getText().toString()));
  	      
  	      editor.commit();
	}
	/**
	 * Eventos clique no Componente
	 * @param view
	 */
	public void saviourBotOnClick(View view){
		Log.i(this.getLocalClassName(), "saviourBotOnClick");
		preferencesConfirm();
	}
	/**
	 * Eventos clique no Componente
	 * @param view
	 */
	public void flareGunBotOnClick(View view){
		Log.i(this.getLocalClassName(), "flareGunBotOnClick");
		preferencesConfirm();
	}
	/**
	 * Eventos clique no Componente
	 * @param view
	 */
	public void checkIsServer(View view){
		Log.i(this.getLocalClassName(), "checkIsServer");
		 if(checkServer.isChecked()){
			 try {
				 txtServerIP.setText(Network.getIpAddress());
		    	  txtServerPort.setText("8080");
			} catch (Exception e) {
				Log.e("ERROR", "No identify ip adress");
			}
	    	  
	      }
		preferencesConfirm();
	}
	public void applyOnClick(View view){
		preferencesConfirm();
		Intent intent = new Intent(this, Homescreen.class);
		startActivity(intent);
	}
}
