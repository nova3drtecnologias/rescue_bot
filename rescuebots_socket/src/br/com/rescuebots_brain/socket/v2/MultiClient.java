package br.com.rescuebots_brain.socket.v2;

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
	private static Vector<ObjectOutputStream> clientesConnectados;
	
	@SuppressWarnings("unchecked")
	public MultiClient(Socket s){
		clientesConnectados = new Vector<ObjectOutputStream>();
		this.server = s;
		
	}
	
	private void getStreams() throws IOException{
		output = new ObjectOutputStream (server.getOutputStream());
		output.flush();
		
		input = new ObjectInputStream( server.getInputStream() );
	}
	
	@SuppressWarnings("unchecked")
	private void processConnection() throws IOException{
		Information info = null;
		do{ 
			try{
				clientesConnectados.add(output);
					
				info = (Information) input.readObject(); // lê uma nova mensagem mudei para pegar objeto
				messageFull = "Mensagem : " + info.getMessage();
				//messageFull += " Ponto X : " + info.getOrigem();
				
				messageFull += " Robot Id : " + info.getRobotid();
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
		}while(info!=null);
	}
	
	public String getMessageFull() {
		return messageFull;
	}

	public void setMessageFull(String messageFull) {
		this.messageFull = messageFull;
	}

	public static Vector<ObjectOutputStream> getClientesConnectados() {
		return clientesConnectados;
	}

	public static void setClientesConnectados(Vector<ObjectOutputStream> clientesConnectados) {
		MultiClient.clientesConnectados = clientesConnectados;
	}

	@SuppressWarnings("unchecked")
	private void sendAll(String message){
		
		Enumeration elem = clientesConnectados.elements();
		while (elem.hasMoreElements()) {
			ObjectOutputStream object = (ObjectOutputStream) elem.nextElement();
			try{
				Rota info = new Rota();
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
