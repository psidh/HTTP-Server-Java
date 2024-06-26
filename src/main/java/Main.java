import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main{
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      
      InputStream in = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = reader.readLine();
      System.out.println("received: " + line);

      String[] HTTPRequest  = line.split(" ", 0);
      System.out.println("request: " + HTTPRequest[1]);
      OutputStream out  = clientSocket.getOutputStream();

      // Correctly use the request path
      String requestPath = HTTPRequest[1];
      
      if (requestPath.equals("/echo/banana")) {
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 6\r\n\r\nbanana".getBytes());
      } else if(requestPath.equals("/")) {
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n".getBytes());
      } else {
        out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }

      out.flush();
      System.out.println("accepted new connection");

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}