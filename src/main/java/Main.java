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

import java.io.BufferedReader;

import java.io.File;

import java.io.FileNotFoundException;

import java.io.FileReader;

import java.io.FileWriter;

import java.io.IOException;

import java.io.InputStream;

import java.io.InputStreamReader;

import java.io.OutputStream;

import java.net.ServerSocket;

import java.net.Socket;

import java.util.HashMap;

import java.util.Map;

public class Main {

  public static void main(String[] args) {

    ServerSocket serverSocket = null;

    Socket clientSocket = null;

    try {

      String directoryString = null;

      if (args.length > 1 && "--directory".equals(args[0])) {

        directoryString = args[1];

      }

      // Connect

      serverSocket = new ServerSocket(4221);

      serverSocket.setReuseAddress(true);

      while (true) {

        clientSocket = serverSocket.accept();

        System.out.println("accepted new connection");

        RequestHandler handler =

            new RequestHandler(clientSocket.getInputStream(),

                               clientSocket.getOutputStream(), directoryString);

        handler.start();

      }

    } catch (IOException e) {

      System.out.println("IOException: " + e.getMessage());

    }

  }

}

class RequestHandler extends Thread {

  private InputStream inputStream;

  private OutputStream outputStream;

  private String fileDir;

  RequestHandler(InputStream inputStream, OutputStream outputStream,

                 String fileDir) {

    this.inputStream = inputStream;

    this.outputStream = outputStream;

    this.fileDir = fileDir == null ? "" : fileDir + File.separator;

  }

  public void run() {

    try {

      // Read

      BufferedReader bufferedReader =

          new BufferedReader(new InputStreamReader(inputStream));

      String requestLine = bufferedReader.readLine();

      Map<String, String> requestHeaders = new HashMap<String, String>();

      String header = null;

      while ((header = bufferedReader.readLine()) != null &&

             !header.isEmpty()) {

        String[] keyVal = header.split(":", 2);

        if (keyVal.length == 2) {

          requestHeaders.put(keyVal[0], keyVal[1].trim());

        }

      }

      // Read body

      StringBuffer bodyBuffer = new StringBuffer();

      while (bufferedReader.ready()) {

        bodyBuffer.append((char)bufferedReader.read());

      }

      String body = bodyBuffer.toString();

      // Process

      String[] requestLinePieces = requestLine.split(" ", 3);

      String httpMethod = requestLinePieces[0];

      String requestTarget = requestLinePieces[1];

      String httpVersion = requestLinePieces[2];

      // Write

      if ("POST".equals(httpMethod)) {

        if (requestTarget.startsWith("/files/")) {

          File file = new File(fileDir + requestTarget.substring(7));

          if (file.createNewFile()) {

            FileWriter fileWriter = new FileWriter(file);

            fileWriter.write(body);

            fileWriter.close();

          }

          outputStream.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());

        } else {

          outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());

        }

        outputStream.flush();

        outputStream.close();

        return;

      }

      if (requestTarget.equals("/")) {

        outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());

      } else if (requestTarget.startsWith("/echo/")) {

        String echoString = requestTarget.substring(6);

        String outputString = "HTTP/1.1 200 OK\r\n"

                              + "Content-Type: text/plain\r\n"

                              + "Content-Length: " + echoString.length() +

                              "\r\n"

                              + "\r\n" + echoString;

        outputStream.write(outputString.getBytes());

      } else if (requestTarget.equals("/user-agent")) {

        String outputString =

            "HTTP/1.1 200 OK\r\n"

            + "Content-Type: text/plain\r\n"

            + "Content-Length: " + requestHeaders.get("User-Agent").length() +

            "\r\n"

            + "\r\n" + requestHeaders.get("User-Agent");

        outputStream.write(outputString.getBytes());

      } else if (requestTarget.startsWith("/files/")) {

        String fileName = requestTarget.substring(7);

        FileReader fileReader;

        try {

          fileReader = new FileReader(fileDir + fileName);

        } catch (FileNotFoundException e) {

          outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());

          outputStream.flush();

          outputStream.close();

          return;

        }

        BufferedReader bufferedFileReader = new BufferedReader(fileReader);

        StringBuffer stringBuffer = new StringBuffer();

        String line;

        while ((line = bufferedFileReader.readLine()) != null) {

          stringBuffer.append(line);

        }

        bufferedFileReader.close();

        fileReader.close();

        String outputString = "HTTP/1.1 200 OK\r\n"

                              + "Content-Type: application/octet-stream\r\n"

                              + "Content-Length: " + stringBuffer.length() +

                              "\r\n"

                              + "\r\n" + stringBuffer.toString();

        outputStream.write(outputString.getBytes());

      } else {

        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());

      }

      outputStream.flush();

      outputStream.close();

    } catch (IOException e) {

      System.out.println("IOException: " + e.getMessage());

    }

  }

}