import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TCP Server - Güvenilir bağlantı tabanlı soket uygulaması
 * Taşıma Katmanı Analizi: TCP (Transmission Control Protocol)
 */
public class TCPServer {

    private static final int PORT = 5000;
    private static int connectionCount = 0;

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  TCP SERVER - Taşıma Katmanı Analizi");
        System.out.println("  Port: " + PORT);
        System.out.println("  Protokol: TCP (Connection-Oriented)");
        System.out.println("==============================================\n");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[BILGI] Sunucu baslatildi. Baglanti bekleniyor...");
            System.out.println("[BILGI] TCP UC-YOL EL SIKISMASI aktif (SYN -> SYN-ACK -> ACK)\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                connectionCount++;
                int currentConn = connectionCount;

                // Her istemci için ayrı thread
                Thread clientThread = new Thread(() -> handleClient(clientSocket, currentConn));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("[HATA] Sunucu hatasi: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, int connId) {
        String clientIP = socket.getInetAddress().getHostAddress();
        int clientPort = socket.getPort();
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

        System.out.println("┌─────────────────────────────────────────────");
        System.out.println("│ [" + timestamp + "] Yeni TCP Baglantisi #" + connId);
        System.out.println("│ İstemci IP  : " + clientIP);
        System.out.println("│ İstemci Port: " + clientPort);
        System.out.println("│ Yerel Port  : " + socket.getLocalPort());
        System.out.println("│ TCP Durumu  : ESTABLISHED");
        System.out.println("└─────────────────────────────────────────────");

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // TCP Başlık Bilgileri (simülasyon)
            System.out.println("\n[TCP PAKET ANALİZİ] Bağlantı #" + connId);
            System.out.println("  Kaynak Port    : " + clientPort);
            System.out.println("  Hedef Port     : " + PORT);
            System.out.println("  Sıra Numarası  : " + (int)(Math.random() * 100000));
            System.out.println("  Onay Numarası  : " + (int)(Math.random() * 100000));
            System.out.println("  Bayraklar      : ACK=1, PSH=1");
            System.out.println("  Pencere Boyutu : 65535 byte");
            System.out.println("  Checksum       : 0x" + Integer.toHexString((int)(Math.random() * 0xFFFF)));

            String mesaj;
            while ((mesaj = in.readLine()) != null) {
                String zaman = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                System.out.println("\n[" + zaman + "] ALINAN MESAJ #" + connId + ": \"" + mesaj + "\"");
                System.out.println("  [TCP] ACK gonderiliyor... (Guvenirlilk onaylandi)");

                String yanit = "[TCP-SERVER] Mesajiniz alindi (Baglanti #" + connId + "): " + mesaj.toUpperCase();
                out.println(yanit);
                System.out.println("  [TCP] Yanit gonderildi: \"" + yanit + "\"");

                if (mesaj.equalsIgnoreCase("quit")) {
                    System.out.println("\n[TCP] FIN paketi alindi. Baglanti sonlandiriliyor...");
                    System.out.println("[TCP] Dort-yol el siksismasi: FIN -> ACK -> FIN -> ACK");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[HATA] Bağlantı #" + connId + " hatası: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("\n[BILGI] Baglanti #" + connId + " kapatildi. Durum: CLOSED\n");
            } catch (IOException e) {
                System.err.println("[HATA] Socket kapatma hatası: " + e.getMessage());
            }
        }
    }
}
