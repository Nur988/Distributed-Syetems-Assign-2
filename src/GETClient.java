import java.io.IOException;
import java.net.*;
import java.io.*;




public class GETClient {

        //Lamport clock declared
        private static long clientlamportClock = 1;

        public static void main(String[] args) throws IOException {


            //Take INPUT from the command line for port and host on Client Startup
           //Sample String "http://servername.domain.domain:portnumber"
            String host="localhost";
            int port = 8080;
            String getRequest = "GET";

            if (args.length < 1) {
                    System.exit(1);
            }



            String [] host_port = parseServerUrl(args[0]);
            host = host_port[0];
            port = Integer.parseInt(host_port[1]);
            getRequest = createGetRequest(host,port,clientlamportClock+1);
            sendGetRequest(host,port,getRequest);







}
private static void sendGetRequest(String host,int port,String getRequest)
    {
        try( Socket socket =new Socket(host,port))
        {
            // Declare the output and input stream to read and write data
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in= new DataInputStream(socket.getInputStream());

            out.writeUTF(getRequest);
            out.flush();
            processResponse(in);
            // Continuously listen for responses
            while (true) {
                processResponse(in);
            }


            // Wait 1 second before next connection attempt
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }

    }





//Function to process the response from the server and printing the content
    public static void processResponse(DataInputStream response) throws IOException
    {

        String line;

        String responseString = response.readUTF();
        long clock = extractLamportClock(responseString);
        clientlamportClock = clock;


        if(responseString.contains("404"))
        {
            System.out.println("404 Not Found");
        } else {
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










    }




//Function to create the GET request
public static String createGetRequest(String host, int port,long lamportClockValue) {
        // Start building the request string
        String getRequest = "GET";
        getRequest = getRequest+" HTTP/1.1\n"+"Host: "+host+" Port :"+port+" Lamport-Clock :"+lamportClockValue;
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

    private static long extractLamportClock(String response) {
        // Split the response into individual lines using CRLF as delimiter
        String[] lines = response.split("\r\n");

        // Iterate through each line to search for the Lamport-Clock header
        for (String line : lines) {
            // Check if the line starts with "Lamport-Clock:"
            if (line.startsWith("Lamport-Clock:")) {
                // Split the line into key and value parts
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        // Parse the Lamport clock value and return it
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        // Handle the case where the Lamport clock value is not a valid integer
                        System.out.println("Invalid Lamport clock value: " + parts[1]);
                    }
                }
            }
        }
        // Return null if the Lamport clock was not found or could not be parsed
        return -1;
    }









}