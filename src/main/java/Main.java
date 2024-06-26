import java.io.*;
import java.net.*;
import java.nio.file.Files;

// public class Main {
//     private static String directoryPath = null;

//     public static void main(String[] args) {
//         if (args.length != 2 || !args[0].equals("--directory")) {
//             System.err.println("Usage: java Main --directory <directory>");
//             return;
//         }

//         directoryPath = args[1];
//         System.out.println("Files directory: " + directoryPath);

//         ServerSocket serverSocket = null;

//         try {
//             serverSocket = new ServerSocket(4221);
//             serverSocket.setReuseAddress(true);

//             while (true) {
//                 Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
//                 System.out.println("Accepted new connection");

//                 // Create a new thread for each connection
//                 new Thread(new ClientHandler(clientSocket, directoryPath)).start();
//             }

//         } catch (IOException e) {
//             System.out.println("IOException: " + e.getMessage());
//         } finally {
//             try {
//                 if (serverSocket != null) serverSocket.close();
//             } catch (IOException e) {
//                 System.out.println("IOException during cleanup: " + e.getMessage());
//             }
//         }
//     }
// }


// class ClientHandler implements Runnable {
//     private Socket clientSocket;
//     private String directoryPath;

//     public ClientHandler(Socket clientSocket, String directoryPath) {
//         this.clientSocket = clientSocket;
//         this.directoryPath = directoryPath;
//     }

//     @Override
//     public void run() {
//         try (InputStream in = clientSocket.getInputStream();
//              BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//              OutputStream out = clientSocket.getOutputStream()) {

//             // Read the request line
//             String line = reader.readLine();
//             if (line == null) {
//                 return;
//             }
//             System.out.println("Received: " + line);

//             // Split the request line into parts
//             String[] HTTPRequest = line.split(" ", 0);
//             if (HTTPRequest.length < 2) {
//                 return;
//             }
//             System.out.println("Request Path: " + HTTPRequest[1]);

//             // Continue reading headers
//             String header;
//             String userAgent = null;
//             while ((header = reader.readLine()) != null && !header.isEmpty()) {
//                 if (header.startsWith("User-Agent:")) {
//                     userAgent = header.substring("User-Agent:".length()).trim();
//                 }
//             }

//             // Handle the request
//             String requestPath = HTTPRequest[1];

//             if (requestPath.startsWith("/files/")) {
//                 String filename = requestPath.substring("/files/".length());
//                 String filePath = directoryPath + File.separator + filename;

//                 File file = new File(filePath);
//                 if (file.exists() && !file.isDirectory()) {
//                     // File found, prepare to send response
//                     byte[] fileContent = Files.readAllBytes(file.toPath());
//                     String contentType = "application/octet-stream";
//                     String contentLength = String.valueOf(fileContent.length);

//                     String response = "HTTP/1.1 200 OK\r\n" +
//                                       "Content-Type: " + contentType + "\r\n" +
//                                       "Content-Length: " + contentLength + "\r\n\r\n";

//                     out.write(response.getBytes());
//                     out.write(fileContent);
//                 } else {
//                     // File not found
//                     String response = "HTTP/1.1 404 Not Found\r\n\r\n";
//                     out.write(response.getBytes());
//                 }
//             } else {
//                 // Invalid request path
//                 String response = "HTTP/1.1 404 Not Found\r\n\r\n";
//                 out.write(response.getBytes());
//             }

//             out.flush();
//         } catch (IOException e) {
//             System.out.println("IOException: " + e.getMessage());
//         } finally {
//             try {
//                 if (clientSocket != null) clientSocket.close();
//             } catch (IOException e) {
//                 System.out.println("IOException during cleanup: " + e.getMessage());
//             }
//         }
//     }
// }

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Main {

  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;

    Socket clientSocket = null;

    String directory = "";

    if (args.length > 1 && args[0].equals("--directory")) {

      directory = args[1];

      System.out.println(args[1]);

    }

    try {
      serverSocket = new ServerSocket(4221);

      serverSocket.setReuseAddress(true);

      while (true) {

        clientSocket =

            serverSocket.accept(); // Wait for connection from client.

        // client side conversion of bytes into data.

        BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String req;
        ArrayList<String> HttpReq = new ArrayList<String>();
        while (!(req = clientIn.readLine()).equals(""))
          HttpReq.add(req);

        System.out.println(HttpReq);

        String URL[] = HttpReq.get(0).split(" ", 0);

        if (URL[1].equals("/")) {

          String response = "HTTP/1.1 200 OK\r\n\r\n";

          clientSocket.getOutputStream().write(response.getBytes());

        } else if (URL[1].startsWith("/echo/")) {

          String path[] = URL[1].split("/", 0);

          String response =

              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length:" +

              path[2].length() + "\r\n\r\n" + path[2];

          clientSocket.getOutputStream().write(response.getBytes());

        } else if (URL[1].startsWith("/user-agent")) {

          String user_agent[] = new String[2];

          for (String s : HttpReq) {

            if (s.startsWith("User-Agent"))

              user_agent = s.split(": ");

          }

          String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length:" +

              user_agent[1].length() + "\r\n\r\n" + user_agent[1];

          clientSocket.getOutputStream().write(response.getBytes());

        } else if (URL[1].startsWith("/files")) {

          String filename = URL[1].split("/", 0)[2];

          File file = new File(directory, filename);

          System.out.println(file.toPath());

          if (file.exists()) {
            byte[] fileContent = Files.readAllBytes(file.toPath());

            String httpResponse =

                "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +

                fileContent.length + "\r\n\r\n" + new String(fileContent);

            clientSocket.getOutputStream().write(

                httpResponse.getBytes(StandardCharsets.UTF_8));

          } else {

            String response = "HTTP/1.1 404 Not Found\r\n\r\n";

            clientSocket.getOutputStream().write(response.getBytes());

          }

        } else {

          String response = "HTTP/1.1 404 Not Found\r\n\r\n";

          clientSocket.getOutputStream().write(response.getBytes());

        }

        clientSocket.close();

        System.out.println("accepted new connection");

      }

    } catch (IOException e) {

      System.out.println("IOException: " + e.getMessage());

    }

  }

}