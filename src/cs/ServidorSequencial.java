package cs;

import java.io.*;
import java.net.*;

public class ServidorSequencial {
    static final int PORT = 5001;
    static final int BUFFER = 8192;
    static String FILE = "arquivo_5mb.bin";

    public static void main(String[] args) throws IOException {
        if (args.length > 0) FILE = args[0];
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor sequencial na porta " + PORT + " | arquivo: " + FILE);
            while (true) {
                Socket client = server.accept();
                System.out.println("Cliente: " + client.getInetAddress());
                servir(client);      // bloqueia até terminar este cliente
                client.close();
            }
        }
    }

    static void servir(Socket client) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(FILE));
             OutputStream out = new BufferedOutputStream(client.getOutputStream())) {
            byte[] buf = new byte[BUFFER];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }
}