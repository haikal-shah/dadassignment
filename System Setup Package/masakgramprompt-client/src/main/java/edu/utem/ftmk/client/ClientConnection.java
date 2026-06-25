package edu.utem.ftmk.client;

import edu.utem.ftmk.network.Request;
import edu.utem.ftmk.network.Response;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnection {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    /**
     * Sends a request to the server and returns the response.
     */
    public static Response sendRequest(Request request) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            // Send request
            out.writeObject(request);
            out.flush();
            
            // Read response
            return (Response) in.readObject();
            
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Connection error: " + e.getMessage());
        }
    }
}
