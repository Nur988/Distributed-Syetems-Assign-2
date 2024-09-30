import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.*;


public class AggregationServer {

    private static Map<Socket,UUID> idMap = new HashMap<Socket,UUID>();
    private static List<String> weatherData = new ArrayList<>();
    private static Map<Socket,Long> serverTime = new HashMap<>();

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
        getClient(port);



    }

    public static void getClient(int port) throws IOException
    {
        //try connecting and create inf server socket
        //then handle each client that connects in a different thread for concurrency
        try(ServerSocket server = new ServerSocket(port))
        {

            //Loop and recieve client connections continuously
            while(true) {

                Socket client = server.accept();

                //Generate id for socket
                UUID uniqueID = UUID.randomUUID();
                System.out.println("Client connected to "+client.getRemoteSocketAddress());


                //store socket and id in map
                idMap.put(client,uniqueID);

                //Start thread
                Thread clientThread = new Thread(()-> {
                    try {
                        handleClient(client,uniqueID);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                clientThread.start();

                //

            }

        }
    }
    public static void handleClient(Socket client, UUID uniqueID) throws IOException
    {

        try(
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            DataInputStream in = new DataInputStream(client.getInputStream());
            ){

            String clientMessage = in.readUTF();
            if(clientMessage==null)
            {
                out.writeUTF("invalid command");
                out.flush();
            }
            else if(clientMessage.startsWith("GET"))
            {

                if(weatherData.isEmpty())
                {
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n No data";
                    out.writeUTF(response);
                    out.flush();
                }
               else{


                String responseJson="";
                String data = weatherData.get(weatherData.size()-1);
                responseJson+="HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 10\r\n\r\n"+data;
                out.writeUTF(responseJson);
                out.flush();
               }
            }
            else
            {
                    String responseJson = parseResponse(clientMessage);
                    if(responseJson.trim().isEmpty())
                    {
                        out.writeUTF("HTTP/1.1 204 Not Found\r\n\r\n No data");
                        out.flush();
                    }
                    else
                    {
                        if(responseJson.startsWith("-1"))
                        {
                            out.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\n Invalid Request");
                            out.flush();
                        }
                        else
                        {
                            weatherData.add(clientMessage);
                            if(!serverTime.containsKey(client))
                            {
                                out.writeUTF("HTTP/1.1 201 Created\r\n\r\n Created");
                                out.flush();
                            }
                            else
                            {
                                out.writeUTF("HTTP/1.1 200 OK\r\n\r\n Received");
                                out.flush();

                            }
                        }
                    }
                    //close socket
                    client.close();
            }



        }
        catch(IOException e)
        {
            e.printStackTrace();
        }







    }


    private static String parseResponse(String input)
    {
        int index = input.indexOf("Lamport-Clock");
        if(index == -1)return "-1";
        index = input.indexOf("{", index);
        if(index == -1)return "-1";
        String jsonString = input.substring(index);
        if (jsonString != null && !jsonString.trim().isEmpty() && jsonString.startsWith("{") && jsonString.endsWith("}")) {
            return jsonString;
        }
        return "-1";
    }


}



