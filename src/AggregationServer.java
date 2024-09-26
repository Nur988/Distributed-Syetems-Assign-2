//import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AggregationServer {

    public static void main(String[] args) throws IOException
    {

        int port ;

        //Read the args first element for port or set it default to 6666
        if(args.length ==0)
       {
           port = 6666;
       }
        else
       {
           port = Integer.parseInt(args[0]);
        }

        // Now Threads will be used to handle multiple operations from here
        // Handling clients
        // removing old contents
        handleClient(port);



    }

    public static void handleClient(int port) throws IOException
    {
        //try connecting and creatinf server socket
        //then handle each client that connects in a different thread for concurrency
        try(ServerSocket server = new ServerSocket(port))
        {

            //Loop and recieve client connections continuously
            while(true) {
                Socket client = server.accept();

                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
            }

        }
    }




}



class ClientHandler extends Thread {




    }
