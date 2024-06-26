import java.io.*;
import java.net.*;

public class FileServer {
    private static final int PORT = 4221;
    private static String directoryPath;

    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("--directory")) {
            System.err.println("Usage: ./your_server.sh --directory /tmp/");
            return;
        }
        directoryPath = args[1];

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length != 3 || !requestParts[0].equals("GET")) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String filePath = directoryPath + requestParts[1];

            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                long length = file.length();
                out.write(("HTTP/1.1 200 OK\r\n" +
                           "Content-Type: application/octet-stream\r\n" +
                           "Content-Length: " + length + "\r\n\r\n").getBytes());

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            } catch (IOException e) {
                sendErrorResponse(out, 500, "Internal Server Error");
            }
        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static void sendErrorResponse(OutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}
