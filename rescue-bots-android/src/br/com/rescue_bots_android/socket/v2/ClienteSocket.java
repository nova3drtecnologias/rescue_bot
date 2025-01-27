package br.com.rescue_bots_android.socket.v2;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import br.com.rescuebots.pojo.Information;

public class ClienteSocket  extends AsyncTask<String, Void, Void> {

	private static final long serialVersionUID = 1L;
	private Socket servidor = null;
	private ObjectOutputStream output = null;
    private ObjectInputStream input = null;
    private String messageFull = "";
    //private EditText enterField; // insere informações fornecidas pelo usuário
    //private EditText logArea; // exibe informações para o usuário
    private String host = "";
    private int port = 0;
    private String user = ""; 
    private TextView logger;
   // EditText editText_host;
   // EditText editText_log;
   // EditText editText_port;
   // EditText editText_usuario;
//	private Button button_send;
//	private EditText editText_command;
    
	public ClienteSocket(TextView logger, String user, String host, int port){
		this.host = host;
		this.user = user;
		this.port = port;
		this.logger = logger;
	}
	public void initClient(String host, int port, String user){
		try {
			this.user = user;
			this.host = host;
			this.port = port;
			connectToServer(host, port);
			getStreams();
			processConnection();
		}catch(EOFException eof){
			eof.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}finally{
			closeConnection();
		}
	}
	
	private void connectToServer(String host, int port) throws UnknownHostException, IOException{
		servidor = new Socket(InetAddress.getByName(host),port);
		if (servidor != null){
			displayMessage(user.toUpperCase() + " conectou ao >> " + servidor + " server \n");
		}
	}
		
	private void closeConnection(){
		try{
			if(output != null)output.close();
			if(input != null)input.close();
			if(servidor != null)servidor.close();
			System.out.println("Terminei a conexão!!!");
		}catch(IOException io){
			io.printStackTrace();
		}
	}
	
	private void getStreams() throws IOException{
		output = new ObjectOutputStream (servidor.getOutputStream());
		output.flush();
		
		input = new ObjectInputStream( servidor.getInputStream() );
	}
	
	private void processConnection() throws IOException{
		do{ 
			try{
				Information info;	
				info = (Information) input.readObject(); // lê uma nova mensagem mudei para pegar objeto
				if(info.getUsuario() != null && info.getUsuario().length() > 0){
					messageFull = info.getUsuario().toUpperCase();
					messageFull += info.getMessage();
				}else{
					messageFull = info.getMessage();
				}
				
				//adiciona mensagem enviada pelo servidor !!!!
				displayMessage(messageFull + "\n");

			}catch(ClassNotFoundException cl){
				cl.printStackTrace();
			}
		}while(!messageFull.contains("FIM"));
	}
	
	public void sendData( String message ){
	   try{
		   Information info = new Information();
         if(user != null) info.setUsuario(user);
    	 info.setMessage(message);
         output.writeObject(info);
         output.flush();
      }catch(IOException io){
         io.printStackTrace();
      }
	 }
	
	
	private void displayMessage( final String messageToDisplay ){
		/*
	      SwingUtilities.invokeLater(new Runnable(){
	            public void run(){
	               displayArea.append( messageToDisplay );
	            } 
	         });*/
		logger.append(messageToDisplay);
	   }
	/*
	public static void main(String[] args) {
		ClienteSocket client = new ClienteSocket();
		String usuario = "jalmeida";
		client.initClient("190.0.1.104", 1313 , usuario);
			
	}*/
	   /** Called when the activity is first created. */
    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cliente_socket_activity);
        
        editText_host = (EditText)findViewById(R.id.cliente_socket_activity_editText_host);
        editText_log = (EditText)findViewById(R.id.cliente_socket_activity_editText_log);
        editText_port = (EditText)findViewById(R.id.cliente_socket_activity_editText_port);
        editText_usuario = (EditText)findViewById(R.id.cliente_socket_activity_editText_usuario);
        button_send = (Button)findViewById(R.id.cliente_socket_activity_button_send);
        editText_command = (EditText)findViewById(R.id.cliente_socket_activity_editText_command);
        
        button_send.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				   //sendData(editText_command.getActionCommand());
					sendData(editText_command.getText().toString());
					editText_command.setText( "" );
			}
        	
        });*/
    	//enterField = new JTextField(); // cria enterField
        /*
	    enterField.addActionListener(new ActionListener(){
	            public void actionPerformed(ActionEvent event){
	               sendData(event.getActionCommand());
	               enterField.setText( "" );
	            } 
	         }
	      );

	      getContentPane().add( enterField, BorderLayout.NORTH );
        */
	/*	ClienteSocket client = new ClienteSocket();
		String usuario = "jalmeida";
		client.initClient("190.0.1.104", 1313 , usuario);
    }*/
	@Override
	protected Void doInBackground(String... params) {
		String param = params[0];
		try {	
			initClient(host, port , user);
			sendData(param);
		} catch (Exception e) {
			Log.e("ERROR",e.getMessage());
		}
		return null;
	}
	@Override
	protected void onPreExecute() {
		logger.append("iniciando envio");
		super.onPreExecute();
	}
	@Override
	protected void onPostExecute(Void result) {
		logger.append("finalizando envio");
		super.onPostExecute(result);
	}
}
