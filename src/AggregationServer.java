import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


public class AggregationServer {

    private static long serverLamportClock = 1;
    private static Map<Socket, UUID> idMap = new HashMap<>(); // Map to store client sockets and their unique IDs
    private static List<String> weatherData = new ArrayList<>(); // List to store weather data
    private static Map<Socket, Long> serverTime = new ConcurrentHashMap<>(); // Map to store socket and the last interaction timestamp
    private static Map<Socket,String> fileMap = new ConcurrentHashMap<>();
    private static final int TIMEOUT = 30000; // Timeout duration set to 30 seconds (in milliseconds)
    private static ReentrantLock fileLock = new ReentrantLock();

    public static void main(String[] args) throws IOException {

        int port;

        // Read the first argument for port, or set it to default 6666 if no arguments are provided
        if (args.length == 0) {
            port = 6666;
        } else {
            port = Integer.parseInt(args[0]);
        }


        //remove older data thread
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                removeInactiveClients();
                try {
                    Thread.sleep(30000); // Sleep for 30 seconds before running cleanup again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        cleanupThread.start();

        // Now, threads will be used to handle multiple clients concurrently
        startClientThread(port);




    }

    /**
     * This method starts a thread to handle incoming client connections.
     * Each client will be handled in a separate thread.
     */
    public static void startClientThread(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {

            // Continuously accept client connections
            while (true) {

                System.out.println("Lamport-Clock = " + serverLamportClock);
                Socket client = server.accept(); // Accept a new client connection
                System.out.println("New client connected: " + client.getRemoteSocketAddress());

                // Generate a unique ID for the socket
                UUID uniqueID = UUID.randomUUID();
                System.out.println("Client connected from " + client.getRemoteSocketAddress());

                // Store the socket and unique ID in the idMap
                idMap.put(client, uniqueID);

                // Start a new thread to handle the client
                Thread clientThread = new Thread(() -> {
                    try {
                        handleClient(client, uniqueID);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                clientThread.start();
            }
        }
    }

    /**
     * This method handles each client connection.
     * It checks the client request (GET or PUT) and processes it accordingly.
     * A timeout is applied for PUT requests, and it gets reset after each valid data transmission.
     */
    public static void handleClient(Socket client, UUID uniqueID) throws IOException {

        try (
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                DataInputStream in = new DataInputStream(client.getInputStream());
        ) {

            while (true) { // Keep connection open until manually closed or timeout occurs
                try {
                    String clientMessage = in.readUTF(); // Read client message

                    // If the command is invalid, return a 400 Bad Request response
                    if (!clientMessage.startsWith("GET") && !clientMessage.startsWith("PUT")) {
                        out.writeUTF("HTTP/1.1 400 Bad Request\r\n\r\n Invalid command");
                        out.flush();
                    }
                    // Handle GET request
                    else if (clientMessage.startsWith("GET")) {


                        if (weatherData.isEmpty()) {
                            String response = "HTTP/1.1 404 Not Found\r\nNo data";
                            out.writeUTF(response);
                            out.flush();
                        } else {
                            String responseJson = "";
                            String data = weatherData.get(weatherData.size() - 1); // Get the latest weather data
                            long clock = extractLamportClock(clientMessage);
                            serverLamportClock=Math.max(serverLamportClock,clock)+1;

                            responseJson += "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"+
                                    "Content-Length: "+
                                    data.length() + "\r\n\r\n" + data+
                                    "Lamport-Clock: "+serverLamportClock;
                            out.writeUTF(responseJson);
                            out.flush();
                        }
                    }
                    // Handle PUT request
                    else if (clientMessage.startsWith("PUT")) {

                        // Validate if the JSON is correct
                        String responseJson = isJsonCorrect(clientMessage);
                        if (responseJson.equals("-1")) {
                            out.writeUTF("HTTP/1.1 500 Internal Server Error\r\n\r\n An internal server error occurred");
                            out.flush();
                        } else if (responseJson.trim().isEmpty()) {
                            out.writeUTF("HTTP/1.1 204 No Content\r\n\r\n No data");
                            out.flush();
                        } else {

                            // Add the valid data to the weatherData list
                            int index = responseJson.indexOf("{");
                            weatherData.add(responseJson.substring(index));
                            storeJsonData(responseJson.substring(index),uniqueID);
                            storeClientUpdateTime(client);
                            long clock = extractLamportClock(clientMessage);
                            serverLamportClock = Math.max(serverLamportClock,clock)+1;


                            // Check if the client is new or existing
                            if (!serverTime.containsKey(client)) {
                                serverTime.put(client, System.currentTimeMillis());
                                out.writeUTF("HTTP/1.1 201 Created\r\n\r\n Created"+"Lamport-Clock: "+serverLamportClock);
                            } else {
                                out.writeUTF("HTTP/1.1 200 OK\r\n\r\n Received"+"Lamport-Clock: "+serverLamportClock);
                            }
                            out.flush();

                        }
                    }

                }
                catch(EOFException e)
                {

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method iterates through all clients in the serverTime map,
     * checks if any client has exceeded the 30-second timeout, and removes them.
     * It also deletes the associated file for each removed client.
     *
     * This function is typically called in a separate thread, which runs periodically
     * to clean up inactive clients and their associated resources (e.g., files).
     *
     * Timeout is determined by comparing the last update timestamp with the current system time.
     * If the difference exceeds the TIMEOUT (30 seconds), the client is removed from the map
     * and its associated file is deleted.
     *
     * serverTime A map that stores the client socket and the last interaction timestamp.
     * fileMap A map that stores the client socket and the associated file to delete when the client is inactive.
     */
    public static void removeInactiveClients() {
        long currentTime = System.currentTimeMillis(); // Get the current timestamp

        // Iterate over all clients in the serverTime map
        for (Socket client : new ArrayList<>(serverTime.keySet())) {
            long lastUpdateTime = serverTime.get(client);

            // Check if the client's last update exceeds the TIMEOUT (30 seconds)
            if ((currentTime - lastUpdateTime) > TIMEOUT) {
                // Remove the client from the serverTime map
                serverTime.remove(client);
                System.out.println("Client removed due to timeout: " + client.getRemoteSocketAddress());

                // Get and remove the filename associated with this client
                String filename = fileMap.remove(client);
                if (filename != null) {
                    File file = new File(filename);

                    // Attempt to delete the associated file
                    if (file.exists() && file.delete()) {
                        System.out.println("Deleted file: " + filename);
                    } else {
                        System.out.println("Failed to delete file: " + filename);
                    }
                }
            }
        }
    }

    /**
     * This method stores the client's socket along with the current update time.
     * It can be used to track the last interaction time for a client.
     * @param client The client's socket
     */
    public static void storeClientUpdateTime(Socket client) {
        // Store the current system time (in milliseconds) as the last update time for the client
        long currentTime = System.currentTimeMillis();
        serverTime.put(client, currentTime); // Add/Update the client's update time
        System.out.println("Updated client time for: " + client.getRemoteSocketAddress() + " at " + currentTime);
    }

    /**
     * Stores the provided JSON data into a uniquely named file.
     * The filename is generated using the provided UUID to ensure uniqueness.
     *
     * @param jsonData The JSON data to be written to the file. This should be a valid JSON string.
     * @param uniqueID The unique identifier (UUID) associated with the data. It is used to create a unique filename for the output file.
     * @throws IOException If an I/O error occurs while writing to the file, such as if the file cannot be created or written to.
     *
     * Expected Output:
     * - A file named "weather_data_<uniqueID>.json" is created in the "data" directory.
     * - If the operation is successful, a message is printed to the console indicating the filename where the data was stored.
     * - If an error occurs during file writing, an error message is printed to the console.
     */

    private static void storeJsonData(String jsonData,UUID uniqueID) throws IOException {
        // Generate a unique filename based on timestamp or UUID
        String filename = "data/weather_data_" + uniqueID +"_"+System.currentTimeMillis()+ ".json";

        // Specify the file path (current directory or a specific directory)

        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(jsonData);
            writer.flush();
            System.out.println("Stored data in " + filename);


        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        finally {
            fileLock.unlock(); // Ensure the lock is released
        }
    }

    /**
     * This method checks if the input message contains a valid JSON body.
     * The input is considered valid if it contains a "Lamport-Clock" and the JSON format is correct.
     * @param input The client message (may include HTTP headers and body)
     * @return Returns the JSON string if correct, or "-1" if the input is invalid.
     */
    private static String isJsonCorrect(String input) {
        int index = input.indexOf("Lamport-Clock");
        if (index == -1) return "-1"; // Invalid if "Lamport-Clock" not found
        index = input.indexOf("{", index);
        if (index == -1) return "-1"; // Invalid if JSON object not found
        String jsonString = input.substring(index);
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            return jsonString; // Return the JSON body if it's valid
        }
        return "-1"; // Invalid JSON format
    }

    /**
     * Cleans up resources associated with a client socket when the client disconnects.
     * This includes closing the client connection, removing the client from the server's tracking maps,
     * and deleting any associated files that were created for this client.
     *
     * @param client The socket representing the client connection to be cleaned up.
     * @throws IOException If an I/O error occurs while closing the client connection or deleting the associated file.
     *
     * Expected Output:
     * - The client socket is closed, and the connection is terminated.
     * - The client's entry is removed from the idMap and serverTime maps.
     * - If an associated file exists, it is deleted, and a message is printed to the console indicating
     *   whether the deletion was successful or failed.
     */

    private static void cleanupClient(Socket client) throws IOException {
        // Close the client connection
        client.close();

        // Remove the client from the maps
        idMap.remove(client);
        serverTime.remove(client);

        // Delete the associated file if it exists
        String filename = fileMap.remove(client);
        if (filename != null) {
            File file = new File(filename);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Deleted file: " + filename);
                } else {
                    System.out.println("Failed to delete file: " + filename);
                }
            }
        }
    }

    /**
     * Extracts the Lamport clock value from the HTTP response string.
     *
     * @param response The HTTP response string containing the Lamport clock.
     * @return The Lamport clock value as an Integer, or null if not found or invalid.
     */
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


