import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            String directoryString = null;
            if (args.length > 1 && "--directory".equals(args[0])) {
                directoryString = args[1];
            }

            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);

            System.out.println("Server started. Listening on port 4221...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                RequestHandler handler = new RequestHandler(clientSocket, directoryString);
                handler.start();
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                System.out.println("IOException during server socket cleanup: " + e.getMessage());
            }
        }
    }
}

class RequestHandler extends Thread {
    private Socket clientSocket;
    private String fileDir;

    RequestHandler(Socket clientSocket, String fileDir) {
        this.clientSocket = clientSocket;
        this.fileDir = (fileDir == null) ? "" : fileDir + File.separator;
    }

    @Override
    public void run() {
        try (
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            String requestLine = bufferedReader.readLine();
            Map<String, String> requestHeaders = new HashMap<>();

            String header;
            while ((header = bufferedReader.readLine()) != null && !header.isEmpty()) {
                String[] keyVal = header.split(":", 2);
                if (keyVal.length == 2) {
                    requestHeaders.put(keyVal[0].trim(), keyVal[1].trim());
                }
            }

            String[] requestLinePieces = requestLine.split(" ", 3);
            String httpMethod = requestLinePieces[0];
            String requestTarget = requestLinePieces[1];

            if ("POST".equals(httpMethod) && requestTarget.startsWith("/files/")) {
                handlePostRequest(requestTarget.substring(7), bufferedReader, outputStream);
            } else if ("GET".equals(httpMethod) && requestTarget.startsWith("/files/")) {
                handleGetRequest(requestTarget.substring(7), outputStream);
            } else {
                handleDefaultRequest(requestTarget, requestHeaders, outputStream);
            }

        } catch (IOException e) {
            System.out.println("IOException in RequestHandler: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("IOException during client socket cleanup: " + e.getMessage());
            }
        }
    }

    private void handlePostRequest(String filename, BufferedReader reader, OutputStream out) throws IOException {
        String filePath = fileDir + filename;
        FileWriter fileWriter = null;

        try {
            File file = new File(filePath);
            if (file.createNewFile()) {
                fileWriter = new FileWriter(file);
                String line;
                while ((line = reader.readLine()) != null) {
                    fileWriter.write(line);
                }
                out.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
            } else {
                out.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
            }
        } catch (IOException e) {
            out.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
            out.flush();
        }
    }

    private void handleGetRequest(String filename, OutputStream out) throws IOException {
        String filePath = fileDir + filename;
        File file = new File(filePath);

        if (file.exists() && !file.isDirectory()) {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = fileReader.readLine()) != null) {
                    responseBody.append(line).append("\n");
                }
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                        responseBody.toString().getBytes().length + "\r\n\r\n" + responseBody.toString();
                out.write(response.getBytes());
            }
        } else {
            out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
        out.flush();
    }

    private void handleDefaultRequest(String requestTarget, Map<String, String> headers, OutputStream out) throws IOException {
        if ("/".equals(requestTarget)) {
            out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
        } else if (requestTarget.startsWith("/echo/")) {
            String echoString = requestTarget.substring(6);
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                    echoString.getBytes().length + "\r\n\r\n" + echoString;
            out.write(response.getBytes());
        } else if ("/user-agent".equals(requestTarget)) {
            String userAgent = headers.getOrDefault("User-Agent", "");
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                    userAgent.getBytes().length + "\r\n\r\n" + userAgent;
            out.write(response.getBytes());
        } else {
            out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
        out.flush();
    }
}
