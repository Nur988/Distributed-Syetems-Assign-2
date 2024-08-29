import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        // Corrected "Serversocket" to "ServerSocket"
        ServerSocket server = new ServerSocket(6666);

        System.out.println("Server is waiting for a client...");

        // Accepts a connection from a client
        Socket sock = server.accept();
        System.out.println("Client connected.");

        // Corrected "server.getInputStream()" to "sock.getInputStream()"
        DataInputStream data_in_stream = new DataInputStream(sock.getInputStream());

        // Corrected variable name from "dis" to "data_in_stream"

        while(true)
        {
            String s = data_in_stream.readUTF();
            FileWriter file_writer = new FileWriter("Server.txt",true);
            file_writer.write(s+"\n");
            file_writer.flush();
            System.out.println("Message: " + s);

        }

        // Close the input stream and socket
//        data_in_stream.close();
//        sock.close();
//        server.close();
    }
}
