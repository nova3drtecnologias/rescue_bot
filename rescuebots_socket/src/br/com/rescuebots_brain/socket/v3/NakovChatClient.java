package br.com.rescuebots_brain.socket.v3;


/**
* Nakov Chat Client
* (c) Svetlin Nakov, 2002
* http://www.nakov.com
*
* NakovChatClient connects to Nakov Chat Server and prints all the messages
* received from the server. It also allows the user to send messages to the
* server. NakovChatClient thread reads messages and print them to the standard
* output. Sender thread reads messages from the standard input and sends them
* to the server.
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NakovChatClient
{
   public static final String SERVER_HOSTNAME = "localhost";
   public static final int SERVER_PORT = 2002;
   
   List<String> messages = new ArrayList<String>(){{
	   add("0;0;0;0;-22.3453453;-50.32453453;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3345345;-50.32453345;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3433422;-50.32494586;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3453454;-50.32453453;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3453444;-50.32453442;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3453423;-50.32453453;gps;0.0;1466714754608;NO;34:34:34:34");
	   add("0;0;0;0;-22.3453846;-50.32453454;gps;0.0;1466714754608;NO;34:34:34:34");
   }};
   
   public static void main(String[] args)
   {
	   ObjectInputStream in = null;
	   ObjectOutputStream out = null;
       try {
          // Connect to Nakov Chat Server
          Socket socket = new Socket(SERVER_HOSTNAME, SERVER_PORT);
          in = new ObjectInputStream(socket.getInputStream());
          out = new ObjectOutputStream(socket.getOutputStream());
          System.out.println("Connected to server " +
             SERVER_HOSTNAME + ":" + SERVER_PORT);
       } catch (IOException ioe) {
          System.err.println("Can not establish connection to " +
              SERVER_HOSTNAME + ":" + SERVER_PORT);
          ioe.printStackTrace();
          System.exit(-1);
       }

       // Create and start Sender thread
       Sender sender = new Sender(out);
       sender.setDaemon(true);
       sender.start();

       try {
          // Read messages from the server and print them
           Object message;
          while ((message=in.readObject()) != null) {
              System.out.println(message);
          }
       } catch (IOException ioe) {
          System.err.println("Connection to server broken.");
          ioe.printStackTrace();
       } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

   }
}
