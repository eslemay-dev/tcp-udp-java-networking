import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * TCP Client - Güvenilir bağlantı tabanlı soket istemcisi
 * Taşıma Katmanı Analizi: TCP (Transmission Control Protocol)
 */
public class TCPClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  TCP CLIENT - Taşıma Katmanı Analizi");
        System.out.println("  Sunucu: " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("  Protokol: TCP (Connection-Oriented)");
        System.out.println("==============================================\n");

        System.out.println("[TCP] Uc-yol el siksismasi baslatiliyor...");
        System.out.println("  Adim 1: SYN paketi gonderiliyor -> " + SERVER_IP + ":" + SERVER_PORT);

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {

            System.out.println("  Adim 2: SYN-ACK alindi <- Sunucu");
            System.out.println("  Adim 3: ACK gonderildi -> Sunucu");
            System.out.println("  [TCP] BAGLANTI KURULDU (ESTABLISHED)\n");

            // Bağlantı bilgileri
            System.out.println("[TCP SOKET BİLGİLERİ]");
            System.out.println("  Yerel Adres  : " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());
            System.out.println("  Uzak Adres   : " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            System.out.println("  Alma Tamponu : " + socket.getReceiveBufferSize() + " byte");
            System.out.println("  Gönderme Tamponu: " + socket.getSendBufferSize() + " byte");
            System.out.println("  Keep-Alive   : " + socket.getKeepAlive());
            System.out.println("  Nagle Algoritmasi: " + !socket.getTcpNoDelay() + "\n");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            System.out.println("Mesaj girin ('quit' ile cikis):");
            System.out.println("─────────────────────────────────────────────");

            while (true) {
                System.out.print("[SIZ] > ");
                String mesaj = scanner.nextLine();

                if (mesaj.trim().isEmpty()) continue;

                String gondermeZamani = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                long baslangic = System.currentTimeMillis();

                System.out.println("[" + gondermeZamani + "] TCP Segmenti gonderiliyor...");
                System.out.println("  Veri: \"" + mesaj + "\" (" + mesaj.getBytes().length + " byte)");
                System.out.println("  SEQ Numarasi: " + (int)(Math.random() * 100000));

                out.println(mesaj);

                String yanit = in.readLine();
                long rtt = System.currentTimeMillis() - baslangic;
                String alinisZamani = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

                System.out.println("[" + alinisZamani + "] Yanit alindi (RTT: " + rtt + " ms): \"" + yanit + "\"");
                System.out.println("  ACK Numarasi: " + (int)(Math.random() * 100000));
                System.out.println("  [TCP] Guvenilir iletim onaylandi\n");

                if (mesaj.equalsIgnoreCase("quit")) {
                    System.out.println("[TCP] FIN gonderiliyor - baglanti sonlandiriliyor...");
                    System.out.println("[TCP] 4-yol kapanis el siksismasi: FIN -> ACK -> FIN -> ACK");
                    break;
                }
            }
            scanner.close();
        } catch (ConnectException e) {
            System.err.println("[HATA] Sunucuya baglanilamiyor! TCPServer calistiriliyor mu?");
            System.err.println("       Once 'java TCPServer' komutunu calistirin.");
        } catch (IOException e) {
            System.err.println("[HATA] TCP hatasi: " + e.getMessage());
        }

        System.out.println("\n[TCP] Baglanti kapatildi. Durum: CLOSED");
    }
}