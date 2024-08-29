import java.io.*;
import java.net.*;
public class Content {
    public static void main(String[] args) {

        Socket socket =new Socket("localhost",6666);
        DataOutputStream outstream =new DataOutputStream(s.getOutputStream());
        outstream.writeUTF("Hello Server");
        outstream.flush();
        outstream.close();
        socket.close();

    }}