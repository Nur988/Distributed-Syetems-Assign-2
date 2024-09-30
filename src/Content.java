
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
//import org.json.JSONObject;
public class Content {
    public static void main(String[] args) throws IOException{

        // Ensure that two arguments are provided: server URL and filename
        if (args.length != 2) {
            System.out.println("Server url or filename is missing");
            return;
        }

        // Extract server URL and filename from command line arguments
        String serverHost = args[0].split(":")[0];
        int serverPort = Integer.parseInt(args[0].split(":")[1]);
        String filePath = args[1];
        // Print them to verify
       // System.out.println("Port: " + serverPort);
       // System.out.println("Filename: " + filePath);

        //Fetch the file from the filePath provided and convert it into a Json file and then send it to Server
        //construct the data format to be sent and send the a PUT request
        Map<String,String> dataMap = parseFile(filePath);
        if(!dataMap.containsKey("id"))
        {
            System.out.println("ID field not present");
        }
        String jsonObject = convertMapToJson(dataMap);
        //System.out.println(jsonObject);

        String request = createPutRequest(jsonObject);
        sendPutRequest(serverHost, serverPort, request);






    }

    private static Map<String, String> parseFile(String filePath) {
        Map<String, String> dataMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        dataMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
            return null;
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }
        return dataMap;
    }

    private static String convertMapToJson(Map<String, String> dataMap) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        for (Iterator<Map.Entry<String, String>> it = dataMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            jsonBuilder.append("\"").append(entry.getKey()).append("\": ")
                    .append("\"").append(entry.getValue()).append("\"");

            if (it.hasNext()) {
                jsonBuilder.append(", ");
            }
        }

        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }

    private static String createPutRequest(String jsonData) {
        return String.format(
                "PUT /weather.json HTTP/1.1\r\n" +
                        "User-Agent: ATOMClient/1/0\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: %d\r\n" +
                        "\r\n" +
                        "%s",
                jsonData.length(), jsonData
        );
    }
    private static void sendPutRequest(String serverHost, int serverPort, String putRequest) {
        try (Socket socket = new Socket(serverHost, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Send the PUT request
            out.writeUTF(putRequest);
            out.flush();

            // Read the response from the server
            String responseLine = in.readLine();
            if (responseLine != null) {
                System.out.println("Response: " + responseLine);
                if (responseLine.startsWith("HTTP/1.1 200") || responseLine.startsWith("HTTP/1.1 201")) {
                    System.out.println("Success: Response Code - " + responseLine);
                } else {
                    System.out.println("Error: Response Code - " + responseLine);
                }
            }
        } catch (IOException e) {
            System.out.println("Error sending PUT request: " + e.getMessage());
        }
    }







}