package p2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class P2PPeer {
    static final int CHUNK_SIZE = 1024 * 1024;
    static int NUM_CHUNKS = 50;
    static final int BUFFER = 8192;
    static final int PARALLEL = 8; // downloads simultaneos

    static final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();
    static List<String> peers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int myPort = args.length > 0 ? Integer.parseInt(args[0]) : 6100;
        for (int i = 1; i < args.length; i++) peers.add(args[i]);
        if (peers.isEmpty()) {
            System.err.println("Uso: java P2PPeer <minhaPorta> <host:porta> [host:porta ...]");
            System.exit(1);
        }

        long start = System.nanoTime();

        new Thread(() -> servir(myPort), "server").start();
        Thread.sleep(200);

        // Baixa todas as chunks em paralelo
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_CHUNKS; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> baixarChunk(idx)));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        double s = (System.nanoTime() - start) / 1e9;
        System.out.printf("Peer %d: %d chunks em %.4f s (%.2f MB/s)%n",
                myPort, NUM_CHUNKS, s, (NUM_CHUNKS * (double) CHUNK_SIZE / 1e6) / s);
        System.exit(0); // Descarte
    }

    static void baixarChunk(int idx) {
        if (chunks.containsKey(idx)) return;
        List<String> shuffled = new ArrayList<>(peers);
        Collections.shuffle(shuffled);
        for (String p : shuffled) {
            try {
                String[] hp = p.split(":");
                try (Socket s = new Socket(hp[0], Integer.parseInt(hp[1]));
                     DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                     DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
                    out.writeInt(idx);
                    out.flush();
                    int len = in.readInt();
                    if (len <= 0) continue;
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    chunks.put(idx, buf);
                    return;
                }
            } catch (IOException e) {
                // tenta próximo peer
            }
        }
        System.err.println("Falha ao baixar chunk " + idx);
    }

    static void servir(int port) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket s = server.accept();
                pool.submit(() -> {
                    try (DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                         DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()))) {
                        int idx = in.readInt();
                        byte[] buf = chunks.get(idx);
                        if (buf == null) {
                            out.writeInt(-1);
                            out.flush();
                            return;
                        }
                        out.writeInt(buf.length);
                        out.write(buf);
                        out.flush();
                    } catch (IOException e) { }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}