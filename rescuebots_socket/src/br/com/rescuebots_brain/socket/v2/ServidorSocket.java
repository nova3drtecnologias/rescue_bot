package br.com.rescuebots_brain.socket.v2;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import br.com.rescuebots.utils.Network;
import br.com.rescuebots_brain.database.Database;
/**
 * Classe servidor, que inicia o serviço na porta especificada em PORTA
 * Essa classe gerencia os clientes que a solicitarem com menssagens via Socket
 * @author nova3d-webserver01
 *
 */
public class ServidorSocket implements Runnable {
	ServerSocket server;
	Database database = new Database(); 
	List<MultiClient> conections = null;
	private int port = 8085; //default
	
	  public ServidorSocket(int port) throws Exception{ 
		  conections = new ArrayList<MultiClient>();
		    this.port = port;
		  
	    	database.initDatabase();
	     
	    	//ss = new ServerSocket(porta);   

	        new Thread(this).start();   

		    System.out.println("Brain listener at "
		    		+ " ip : " + Network.getIpAddress()
		    		+ " port : " + port);
			System.out.println("-,-.      __,---.__   ,',`\\");
			System.out.println("-/ `.`. ,-'         `-/ /   )");
			System.out.println("(    `,'             _ \\   ;");
			System.out.println("-\\  _( _           ,'  )/  :");
			System.out.println("--\\ `-( `-.      ,'    /  /");
			System.out.println("---\\   \\ __`.___/_,-( /_,'");
			System.out.println("----`--'`,\\_o,(_)o_,',");
			System.out.println("--------(    /`-'\\    )");
			System.out.println("---------`--:\\,m//`--'");
			System.out.println("-------------`--'");	 
	    }   

	  public void run(){   
		  startServer();
	  }
	
	public void startServer(){
		try {
			server = new ServerSocket(port,100);
			while(true){
				try{
					MultiClient client = new MultiClient(server.accept());
					Thread clientes = new Thread(client);
					clientes.start();
					conections.add(client);
					//System.out.println("Success to conection");
				}catch(EOFException eof){
					eof.printStackTrace();
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String getIpAdress(){
		try {
			return  Network.getIpAddress();
		} catch (SocketException e) {
			
			e.printStackTrace();
		}
		return null;
	}
	public void printHelp(){
		System.out.println("Brain RescueBots Server is online at port : " 
	    		+ " ip : " + getIpAdress()
	    		+ " port : " + port);
		System.out.println("Configure your devices to this port and host");
		System.out.println("\\q --shutdown the server");
		System.out.println("\\l --list all users connections");
		System.out.println("\\m <robot_id> --monotoring a device from robot id");
		System.out.println("\\r <robot_id> <lat1,long1> <lat2,long2> --send rote to especific device");
	}
	/**
	 * \\l --list all users connections
	 * @param command
	 */
	public void listAll(String command){
		System.out.println("List All Clients");
		if(conections!=null){
			
			for (int i = 0; i < conections.size(); i++) {
				MultiClient multiClient = conections.get(i);
			    System.out.println(i+" : " +multiClient.getMessageFull());
			}
		}
	}
	/**
	 * "\\m <robot_id> --monotoring a device from robot id"
	 * @param command
	 */
	public void monitoring(String command){
		System.out.println("Start Monitor");
		if(conections!=null){
			
			for (int i = 0; i < conections.size(); i++) {
				MultiClient multiClient = conections.get(i);
			    System.out.println(i+" : " +multiClient.getMessageFull());
			    for (int j = 0; j < MultiClient.getClientesConnectados().size(); j++) {
			    	System.out.println(i+" : " +multiClient.getClientesConnectados().get(j).toString());
				}
			}
		}
	}
	public void sendRote(String command){
		System.out.println("Start Sending ROTE");
	}
	public void  execCommand( String command){
		if(command.equalsIgnoreCase("\\l")){
			listAll(command);
		}else if(command.equalsIgnoreCase("\\m")){
			monitoring(command);
		}else  if(command.equalsIgnoreCase("\\r")){
			sendRote(command);
		}else{
			System.out.println("Command "+ command +" not Found ");
			printHelp();
		}
	}
	public static void main(String[] args) {
		try {
			ServidorSocket serverMain = new ServidorSocket(8086);
			
			//server.startServer();
			Scanner scanner = new Scanner(System.in);
			System.out.println("BRAIN STARTED ..: we rule the world ?");
			String command = "";
			while(!command.equalsIgnoreCase("\\q")){
				System.out.println("Enter with server command or \\h to help");
				command=(scanner.next());
				if(command.equalsIgnoreCase("\\h")){
					serverMain.printHelp();
				}else{
					serverMain.execCommand(command);
					
				}
			}
		} catch (Exception e) {
			 e.printStackTrace();   
	           System.exit(1); 
		}
		
	}
}
