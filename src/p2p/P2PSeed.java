package p2p;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class P2PSeed {
    static final int PORT = 6000;
    static final int CHUNK_SIZE = 1024 * 1024;
    static String FILE = "arquivo_50mb.bin";
    static int NUM_CHUNKS;
    static long FILE_LEN;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) FILE = args[0];
        File f = new File(FILE);
        FILE_LEN = f.length();
        NUM_CHUNKS = (int) Math.ceil((double) FILE_LEN / CHUNK_SIZE);
        System.out.println("Seed: " + FILE + " | " + NUM_CHUNKS + " chunks de " + CHUNK_SIZE + " bytes");

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Seed P2P na porta " + PORT);
            while (true) {
                Socket s = server.accept();
                pool.submit(() -> handle(s));
            }
        }
    }

    static void handle(Socket s) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
             RandomAccessFile raf = new RandomAccessFile(FILE, "r")) {

            int chunkIdx = in.readInt();
            if (chunkIdx < 0 || chunkIdx >= NUM_CHUNKS) {
                out.writeInt(-1);
                out.flush();
                return;
            }
            long offset = (long) chunkIdx * CHUNK_SIZE;
            int len = (int) Math.min(CHUNK_SIZE, FILE_LEN - offset);
            raf.seek(offset);
            byte[] buf = new byte[len];
            raf.readFully(buf);
            out.writeInt(len);
            out.write(buf);
            out.flush();
        } catch (IOException e) {
            // nada (so botei o comentario para tirar o warning que estava dando)
        }
    }
}