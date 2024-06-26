// import java.io.*;
// import java.nio.file.Files;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;

// public class Main {

//   public static void main(String[] args) {
//     System.out.println("Logs from your program will appear here!");
//     ServerSocket serverSocket = null;

//     Socket clientSocket = null;

//     String directory = "";

//     if (args.length > 1 && args[0].equals("--directory")) {

//       directory = args[1];

//       System.out.println(args[1]);

//     }

//     try {
//       serverSocket = new ServerSocket(4221);

//       serverSocket.setReuseAddress(true);

//       while (true) {

//         clientSocket =

//             serverSocket.accept(); // Wait for connection from client.

//         // client side conversion of bytes into data.

//         BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//         String req;
//         ArrayList<String> HttpReq = new ArrayList<String>();
//         while (!(req = clientIn.readLine()).equals(""))
//           HttpReq.add(req);

//         System.out.println(HttpReq);

//         String URL[] = HttpReq.get(0).split(" ", 0);

//         if (URL[1].equals("/")) {

//           String response = "HTTP/1.1 200 OK\r\n\r\n";

//           clientSocket.getOutputStream().write(response.getBytes());

//         } else if (URL[1].startsWith("/echo/")) {

//           String path[] = URL[1].split("/", 0);

//           String response =

//               "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length:" +

//               path[2].length() + "\r\n\r\n" + path[2];

//           clientSocket.getOutputStream().write(response.getBytes());

//         } else if (URL[1].startsWith("/user-agent")) {

//           String user_agent[] = new String[2];

//           for (String s : HttpReq) {

//             if (s.startsWith("User-Agent"))

//               user_agent = s.split(": ");

//           }

//           String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length:" +

//               user_agent[1].length() + "\r\n\r\n" + user_agent[1];

//           clientSocket.getOutputStream().write(response.getBytes());

//         } else if (URL[1].startsWith("/files")) {

//           String filename = URL[1].split("/", 0)[2];

//           File file = new File(directory, filename);

//           System.out.println(file.toPath());

//           if (file.exists()) {
//             byte[] fileContent = Files.readAllBytes(file.toPath());

//             String httpResponse =

//                 "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +

//                 fileContent.length + "\r\n\r\n" + new String(fileContent);

//             clientSocket.getOutputStream().write(

//                 httpResponse.getBytes(StandardCharsets.UTF_8));

//           } else {

//             String response = "HTTP/1.1 404 Not Found\r\n\r\n";

//             clientSocket.getOutputStream().write(response.getBytes());

//           }

//         } else {

//           String response = "HTTP/1.1 404 Not Found\r\n\r\n";

//           clientSocket.getOutputStream().write(response.getBytes());

//         }

//         clientSocket.close();

//         System.out.println("accepted new connection");

//       }

//     } catch (IOException e) {

//       System.out.println("IOException: " + e.getMessage());

//     }

//   }

// }
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Main {
    private static String directoryPath = null;

    public static void main(String[] args) {
        if (args.length == 2 && args[0].equals("--directory")) {
            directoryPath = args[1];
        } else {
            System.err.println("Usage: java Main --directory <directory>");
            System.exit(1);
        }

        System.out.println("Files directory: " + directoryPath);

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started. Listening on port 4221...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("Accepted new connection");

                // Create a new thread for each connection
                new Thread(new ClientHandler(clientSocket, directoryPath)).start();
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}


class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String directoryPath;

    public ClientHandler(Socket clientSocket, String directoryPath) {
        this.clientSocket = clientSocket;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
        try {
            InputStream in = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            OutputStream out = clientSocket.getOutputStream();

            // Read the request line
            String requestLine = reader.readLine();
            System.out.println("Received: " + requestLine);

            // Split the request line into parts
            String[] requestParts = requestLine.split(" ", 3);
            String method = requestParts[0];
            String path = requestParts[1];

            if (method.equals("POST") && path.startsWith("/files/")) {
                handlePostRequest(path.substring("/files/".length()), reader, out);
            } else if (method.equals("GET") && path.startsWith("/files/")) {
                handleGetRequest(path.substring("/files/".length()), out);
            } else {
                // Invalid request method or path
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

    private void handlePostRequest(String filename, BufferedReader reader, OutputStream out) throws IOException {
        String filePath = directoryPath + File.separator + filename;
    
        // Read headers
        String header;
        int contentLength = 0;
        while ((header = reader.readLine()) != null && !header.isEmpty()) {
            if (header.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(header.substring("Content-Length:".length()).trim());
            }
        }
    
        // Read request body
        char[] body = new char[contentLength];
        reader.read(body, 0, contentLength);
    
        // Write body to file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(body);
        }
    
        // Send response
        String response = "HTTP/1.1 201 Created\r\n\r\n";
        out.write(response.getBytes());
    }

    
    private void handleGetRequest(String filename, OutputStream out) throws IOException {
        String filePath = directoryPath + File.separator + filename;
    
        File file = new File(filePath);
    
        if (file.exists() && !file.isDirectory()) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
    
            String httpResponse =
                    "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                    fileContent.length + "\r\n\r\n";
            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            out.write(fileContent);
        } else {
            String response = "HTTP/1.1 404 Not Found\r\n\r\n";
            out.write(response.getBytes());
        }
    }
    
}
