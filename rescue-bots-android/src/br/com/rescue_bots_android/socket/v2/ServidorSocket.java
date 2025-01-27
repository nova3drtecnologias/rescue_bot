package br.com.rescue_bots_android.socket.v2;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;

public class ServidorSocket {
	private ServerSocket server = null;
	
	public void startServer(int port){
		try {
			server = new ServerSocket(port,100);
			while(true){
				try{
					MultiClient client = new MultiClient(server.accept());
					Thread clientes = new Thread(client);
					clientes.start();
				}catch(EOFException eof){
					eof.printStackTrace();
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServidorSocket server = new ServidorSocket();
		server.startServer(1313);
	}
}
