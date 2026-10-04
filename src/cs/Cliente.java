package cs;

import java.io.*;
import java.net.*;

public class Cliente {
    static final int BUFFER = 8192;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5001;

        try (Socket socket = new Socket(host, port)) {
            long start = System.nanoTime();
            InputStream in = new BufferedInputStream(socket.getInputStream());
            byte[] buf = new byte[BUFFER];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;   // apenas conta os bytes
            }
            double s = (System.nanoTime() - start) / 1e9;
            System.out.printf("Bytes: %d | Tempo: %.4f s | Throughput: %.2f MB/s%n",
                    total, s, (total / 1e6) / s);
        }
    }
}