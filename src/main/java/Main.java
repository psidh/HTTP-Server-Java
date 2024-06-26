import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;


public class Main {
  public static void main(String[] args) {

    System.out.println("Beginning of the program");
    ServerSocket server = null;
    Socket client = null;

    try {

      server = new ServerSocket(4221);
      server.setReuseAddress(true);
      client = server.accept();

      InputStream in = client.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      OutputStream out = client.getOutputStream();

      String line = reader.readLine();
      System.out.println("Received: " + line);
      
      String[] HTTPRequest = line.split(" ", 0);
      System.out.println("Method: " + HTTPRequest[0]);
      System.out.println("request Path: " + HTTPRequest[1]);

      String header;
      String userAgent = null;

      while ((header = reader.readLine()) != null && !header.isEmpty()) {
        if (header.startsWith("User-Agent:")) {
          userAgent = header.substring("User-Agent:".length()).trim();
        }
      }


      String requestPath = HTTPRequest[1];

      if (requestPath.equals("/user-agent") && userAgent != null) {
        String res = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nContent-Length: " +  userAgent.length() +  "\r\n\r\n" + userAgent;
        out.write(res.getBytes());
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
      System.out.println("accepted new connection");
       
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    } finally {
      try {
        if (client != null) client.close();
        if (server != null) server.close();
      } catch (IOException e) {
        System.out.println("IOException during cleanup: " + e.getMessage());
      }
    }
    
  }
}