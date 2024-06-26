import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("--directory")) {
            System.err.println("Usage: java Main --directory <directory>");
            return;
        }

        String directoryPath = args[1];
        System.out.println("Files directory: " + directoryPath);

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(clientSocket, directoryPath));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String directoryPath;

        public ClientHandler(Socket clientSocket, String directoryPath) {
            this.clientSocket = clientSocket;
            this.directoryPath = directoryPath;
        }

        @Override
        public void run() {
            try {
                handleRequest(clientSocket, directoryPath);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRequest(Socket clientSocket, String directoryPath) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine != null) {
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length >= 2 && requestParts[0].equals("GET")) {
                    String requestPath = requestParts[1];
                    if (requestPath.startsWith("/files/")) {
                        String fileName = requestPath.substring("/files/".length());
                        sendFileResponse(fileName, out, directoryPath);
                    } else {
                        send404NotFound(out);
                    }
                }
            }
        }

        private void sendFileResponse(String fileName, OutputStream out, String directoryPath) throws IOException {
            File file = new File(directoryPath, fileName);
            if (file.exists() && file.isFile()) {
                String contentType = "application/octet-stream";
                long contentLength = file.length();

                out.write(("HTTP/1.1 200 OK\r\n" +
                           "Content-Type: " + contentType + "\r\n" +
                           "Content-Length: " + contentLength + "\r\n" +
                           "\r\n").getBytes());

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                send404NotFound(out);
            }
        }

        private void send404NotFound(OutputStream out) throws IOException {
            out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
    }
}
