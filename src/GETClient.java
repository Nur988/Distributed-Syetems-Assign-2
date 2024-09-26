//import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.io.*;
//import org.json.JSONArray;
//import org.json.JSONObject;



public class GETClient {

        //Lamport clock declared
        private static Lamport lamportClock = new Lamport();

        public static void main(String[] args) throws IOException {


            //Take INPUT from the command line for port and host on Client Startup
           //Sample String "http://servername.domain.domain:portnumber"
            String host="localhost";
            int port = 8080;
            String getRequest = "GET";

            if (args.length < 1) {
                    System.exit(1);
            }
            else
            {

                    String [] host_port = parseServerUrl(args[0]);
                    host = host_port[0];
                    port = Integer.parseInt(host_port[1]);

                    getRequest = createGetRequest(host,port,lamportClock);

            }

            try( Socket socket =new Socket(host,port))
            {
                  // Declare the output and input stream to read and write data
                  DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                  BufferedReader in =  new BufferedReader(new InputStreamReader(socket.getInputStream()));



                 // System.out.println(getRequest);
                  out.writeUTF(getRequest);
                  out.flush();
                  lamportClock.incrementClockValue();
                  processResponse(in);


                // Wait 1 second before next connection attempt
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            }



}
//Function to process the response from the server and printing the content
    public static void processResponse(BufferedReader response) throws IOException
    {

        String line;

        String responseString = "";  // Use StringBuilder for efficiency
      // Read the full response
        while((line = response.readLine()) != null) {
           responseString += line;
           responseString += '\n';

        }





        int start = responseString.indexOf("{");
        int end = responseString.lastIndexOf("}");


        String jsonData = responseString.substring(start+1,end);

       String [] jsonDataSplit = jsonData.split(",");
       String  finalData = "" ;
       for(String data: jsonDataSplit)
       {
           finalData += data.trim();
           finalData += "\n";
       }

       System.out.println(finalData);







    }




//Function to create the GET request
public static String createGetRequest(String host, int port,Lamport lamportClockValue) {
        // Start building the request string
        String getRequest = "GET";
        getRequest = getRequest+" HTTP/1.1\n"+"Host: "+host+" Port :"+port+" Lamport-Clock :"+lamportClock.getClockValue();
        // Return the complete request string
        return getRequest;
}

public static String[] parseServerUrl(String url) {
                String host = null;
                int port = -1;


                if (url.startsWith("http://")) {
                        url = url.substring(7);

                }


                if (url.contains(":")) {

                        String[] parts = url.split(":");
                        host = parts[0]; // Host name
                        port = Integer.parseInt(parts[1]); // Port number
                } else {

                        host = url;
                        port = 4647; // Default port for HTTP
                }

                return new String[]{host, String.valueOf(port)};
        }









}