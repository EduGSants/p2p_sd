package cs;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServidorThreadPool {
    static final int PORT = 5003;
    static final int BUFFER = 8192;
    static final int N = 4;                 // máximo de clientes simultâneos
    static String FILE = "arquivo_5mb.bin";

    public static void main(String[] args) throws IOException {
        if (args.length > 0) FILE = args[0];
        ExecutorService pool = Executors.newFixedThreadPool(N);
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor com pool de " + N + " threads na porta " + PORT);
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> {
                    try {
                        servir(client);
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
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
