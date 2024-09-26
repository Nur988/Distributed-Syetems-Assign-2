
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
public class Content {
    public static void main(String[] args) throws IOException{

        String filePath = "data.json";

        try(Socket socket =new Socket("localhost",6666))
        {
            String data = new String(Files.readAllBytes(Paths.get(filePath)));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(data);
            bw.flush();
            System.out.println("Data Sent Succesfully");
           // System.out.println(data);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
       // DataOutputStream outstream =new DataOutputStream(socket.getOutputStream());
//        outstream.writeUTF("Hello Server");
//        outstream.flush();
//        outstream.close();
//        socket.close();

    }}