
package Aggregate_Server;
import java.io.*;
import java.net.*;

public class AggregationServer {
    public static void main(String[] args) throws IOException {
        // Corrected "Serversocket" to "ServerSocket"

        String filePath = "data.json";
        try(ServerSocket server = new ServerSocket(6666))
        {
            System.out.println("Server is waiting for a client...");

           while(true)
           {
               Socket client = server.accept();
               System.out.println("Client connected");

               BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
               StringBuilder jsonData = new StringBuilder();
               String inputLine;
               while((inputLine = in.readLine()) != null)
               {
                   jsonData.append(inputLine).append("\n");
               }
               try(BufferedWriter out = new BufferedWriter(new FileWriter(filePath)))
               {
                   out.write(jsonData.toString());
                  // System.out.println(jsonData.toString());
               }
               client.close();
           }

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }


        // Close the input stream and socket
//        data_in_stream.close();
//        sock.close();
//        server.close();
    }
}
