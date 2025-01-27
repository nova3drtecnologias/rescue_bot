package br.com.rescue_bots_android.socket.v2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Vector;

import br.com.rescuebots.pojo.Information;
import br.com.rescuebots.pojo.Rota;

public class MultiClient  implements Runnable {
	private Socket server;
	private ObjectInputStream input = null;
	private ObjectOutputStream output = null;
	private String messageFull = "";
	@SuppressWarnings("unchecked")
	private static Vector clientesConnectados;
	
	@SuppressWarnings("unchecked")
	public MultiClient(Socket s){
		clientesConnectados = new Vector();
		this.server = s;
		
	}
	
	private void getStreams() throws IOException{
		output = new ObjectOutputStream (server.getOutputStream());
		output.flush();
		
		input = new ObjectInputStream( server.getInputStream() );
	}
	
	@SuppressWarnings("unchecked")
	private void processConnection() throws IOException{
		
		do{ 
			try{
				clientesConnectados.add(output);
				Rota info;	
				info = (Rota) input.readObject(); // lê uma nova mensagem mudei para pegar objeto
				messageFull = "Mensagem : " + info.getMessage();
				messageFull += " Origem : " + info.getOrigem();
				messageFull += " Destino : " + info.getDestino();
				if(info.getUsuario() != null && info.getUsuario().length() > 0){
					System.out.println(info.getUsuario() + " >> " + messageFull);
					sendAll(messageFull,info.getUsuario()  + " >> " );
				}else{
					System.out.println(server.getInetAddress().getHostName() + " >> " + messageFull);
					sendAll(server.getInetAddress().getHostName() + " >> " + messageFull);
				}
				
				clientesConnectados.remove(output);
			}catch(ClassNotFoundException cl){
				cl.printStackTrace();
			}
		}while(!messageFull.contains("FIM"));
	}
	
	@SuppressWarnings("unchecked")
	private void sendAll(String message){
		
		Enumeration elem = clientesConnectados.elements();
		while (elem.hasMoreElements()) {
			ObjectOutputStream object = (ObjectOutputStream) elem.nextElement();
			try{
				Information info = new Information();
				info.setMessage(message);
				object.writeObject(info);
				object.flush();
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void sendAll(String message, String usuario){
		
		Enumeration elem = clientesConnectados.elements();
		while (elem.hasMoreElements()) {
			ObjectOutputStream object = (ObjectOutputStream) elem.nextElement();
			try{
				Information info = new Information();
				info.setUsuario(usuario);
				info.setMessage(message);
				object.writeObject(info);
				object.flush();
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
		}
		
	}
	
	private void disconnectClient() {
		try{
			if(output != null)output.close();
			if(input != null)input.close();
			if(server != null)server.close();
			System.out.println("Disconecta cliente !!!");
		}catch(IOException io){
			io.printStackTrace();
		}
	}
	
	public void run() {
		try{
			getStreams();
			processConnection();
		}catch(IOException io){
			io.printStackTrace();
		}finally{
			disconnectClient();
		}
	}

}
