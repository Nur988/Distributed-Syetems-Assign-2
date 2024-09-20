package Client;

import java.io.*;
import java.net.*;
import java.util.*;

//import  org.json.simple.JSONObject;

public class GETClient {
    public static void main(String[] args) throws IOException {

            Socket socket =new Socket("localhost",6666);
            DataOutputStream outstream =new DataOutputStream(socket.getOutputStream());

            while(true) {
                    System.out.println("Client: ");
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    outstream.writeUTF(input);
            }
//            outstream.writeUTF("Hello Server");
//            outstream.flush();
//            outstream.close();
//            socket.close();

}}