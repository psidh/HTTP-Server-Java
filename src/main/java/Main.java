import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("Accepted new connection");

        // Create a new thread for each connection
        new Thread(new ClientHandler(clientSocket)).start();
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (serverSocket != null) serverSocket.close();
      } catch (IOException e) {
        System.out.println("IOException during cleanup: " + e.getMessage());
      }
    }
  }
}

class ClientHandler implements Runnable {
  private Socket clientSocket;

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      InputStream in = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      OutputStream out = clientSocket.getOutputStream();
      
      // Read the request line
      String line = reader.readLine();
      System.out.println("Received: " + line);

      // Split the request line into parts
      String[] HTTPRequest = line.split(" ", 0);
      System.out.println("Request Path: " + HTTPRequest[1]);

      // Continue reading headers
      String header;
      String userAgent = null;
      while ((header = reader.readLine()) != null && !header.isEmpty()) {
        if (header.startsWith("User-Agent:")) {
          userAgent = header.substring("User-Agent:".length()).trim();
        }
      }

      // Handle the request
      String requestPath = HTTPRequest[1];
      
      if (requestPath.equals("/user-agent") && userAgent != null) {
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + userAgent.length() + "\r\n\r\n" + userAgent;
        out.write(response.getBytes());
      } else if (requestPath.startsWith("/echo/")) {
        String msg = requestPath.substring(6);
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + msg.length() + "\r\n\r\n" + msg;
        out.write(response.getBytes());
      } else if(requestPath.equals("/")) {
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n";
        out.write(response.getBytes());
      } else {
        String response = "HTTP/1.1 404 Not Found\r\n\r\n";
        out.write(response.getBytes());
      }

      out.flush();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (clientSocket != null) clientSocket.close();
      } catch (IOException e) {
        System.out.println("IOException during cleanup: " + e.getMessage());
      }
    }
  }
}
