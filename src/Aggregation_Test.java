import java.io.*;
import java.net.Socket;
import java.util.UUID;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Aggregation_Test {
    static HashMap<Socket, Long> serverTime = new HashMap<>();
    private static final long TIMEOUT = 30000; // 30 seconds
    static HashMap<Socket, String> fileMap = new HashMap<>();

    @Before
    public void setUp() {

    }

    // Test the processing of valid JSON data
    // Ensure that valid JSON is correctly processed
    @Test
    public void testProcessDataValid() {
        String result = AggregationServer.isJsonCorrect("Lamport-Clock: 0 {\"id\":\"123\",\"name\":\"Test\"}");
        assertEquals("{\"id\":\"123\",\"name\":\"Test\"}",result);

    }


    // Test data storage and file creation
    // Ensure that data is stored correctly and files are created
    @Test
    public void testStoreData() throws IOException  {
        UUID serverId = UUID.randomUUID();
        String jsonData = "{\"id\":\"123\",\"name\":\"Test\"}";

        // Store the data
        int response = AggregationServer.storeJsonData(jsonData, serverId);


        assertEquals(1, response);


    }
    @Test
    public void testValidLamportClock() {
        // Case with a valid Lamport-Clock header
        String response = "HTTP/1.1 200 OK\r\nLamport-Clock: 12345\r\nContent-Type: text/html\r\n\r\n";
        long lamportClock = AggregationServer.extractLamportClock(response);
        assertEquals(12345, lamportClock);
    }

    @Test
    public void testInvalidLamportClockValue() {
        // Case where the Lamport-Clock header has an invalid value
        String response = "HTTP/1.1 200 OK\r\nLamport-Clock: abc\r\nContent-Type: text/html\r\n\r\n";
        long lamportClock = AggregationServer.extractLamportClock(response);
        assertEquals(-1, lamportClock); // Should return -1 as the clock value is invalid
    }

    @Test
    public void testStoreClientUpdateTime() throws Exception {
        // Create a mock socket
        Socket mockSocket = mock(Socket.class);
        SocketAddress mockAddress = new InetSocketAddress("127.0.0.1", 8080);
        when(mockSocket.getRemoteSocketAddress()).thenReturn(mockAddress);

        // Call the storeClientUpdateTime method
        int result = AggregationServer.storeClientUpdateTime(mockSocket);

        // Assert that the return value is 1
        assertEquals(1, result);


    }
    @Test
    public void testRemoveInactiveClients() throws Exception {
        // Create mock Socket objects
        Socket activeClient = mock(Socket.class);
        Socket inactiveClient = mock(Socket.class);

        // Mock remote socket address for logging
        when(inactiveClient.getRemoteSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        // Add an active client and an inactive client to the serverTime map
        long currentTime = System.currentTimeMillis();
        AggregationServer.serverTime.put(activeClient, currentTime); // active client has current time
        AggregationServer.serverTime.put(inactiveClient, currentTime - (TIMEOUT + 1000)); // inactive client exceeded the timeout

        // Mock a filename for the inactive client
        String inactiveFilename = "testfile.txt";
        fileMap.put(inactiveClient, inactiveFilename);

        // Mock the file operations
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.delete()).thenReturn(true); // Assume the file is deleted successfully

        // Test the removal of inactive clients
        int result = AggregationServer.removeInactiveClients();


        // Assert that the flag is set to 1 due to successful file deletion
        assertEquals(0, result);

    }


}