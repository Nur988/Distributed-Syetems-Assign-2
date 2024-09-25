package Client;

//import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
//import org.json.JSONArray;
//import org.json.JSONObject;



public class GETClient {
    public static void main(String[] args) throws IOException {

            try(Socket socket =new Socket("localhost",6666))
            {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    out.write("GET\n");
                    out.flush();

                    //Receive response and print out the response
                    String resp =  in.readLine();
                    System.out.println("Aggregation Server response");
                    System.out.println(resp);






            }
            catch(IOException e)
            {
                    e.printStackTrace();
            }



}}