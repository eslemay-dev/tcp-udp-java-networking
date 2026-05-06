import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * UDP Client - Bağlantısız datagram soket istemcisi
 * Taşıma Katmanı Analizi: UDP (User Datagram Protocol)
 * 
 * Özellikler: Connectionless, No handshake, Timeout ile kayıp simülasyonu
 */
public class UDPClient {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 6000;
    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT_MS = 3000;
    private static int kayipSayisi = 0;
    private static int gidenSayisi = 0;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                 UDP CLIENT - TAŞIMA KATMANI ANALİZİ          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("  Sunucu      : " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("  Protokol    : UDP (Connectionless / Datagram)");
        System.out.println("  Timeout     : " + TIMEOUT_MS + " ms\n");

        System.out.println("[UDP] TCP'DEN FARKLAR:");
        System.out.println("    El sıkışması (Handshake) YOK");
        System.out.println("    ACK (Onay) mekanizması YOK");
        System.out.println("    Sıra numarası (SEQ) YOK");
        System.out.println("    Yeniden iletim (Retransmission) YOK");
        System.out.println("    Bağlantı durumu (Connection state) YOK");
        System.out.println("    Basit başlık: SADECE 8 byte\n");

        try (DatagramSocket socket = new DatagramSocket()) {

            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            System.out.println("[UDP SOKET BİLGİLERİ]");
            System.out.println("  Yerel Port      : " + socket.getLocalPort());
            System.out.println("  Hedef Adres     : " + SERVER_IP + ":" + SERVER_PORT);
            System.out.println("  Timeout         : " + TIMEOUT_MS + " ms");
            System.out.println("  Tampon Boyutu   : " + BUFFER_SIZE + " byte");
            System.out.println("  Broadcast       : " + socket.getBroadcast() + "\n");

            Scanner scanner = new Scanner(System.in);
            int datagramSayisi = 0;

            System.out.println("Mesaj girin ('quit' ile cikis):");
            System.out.println("─────────────────────────────────────────────────────────────");

            while (true) {
                System.out.print("[SIZ] > ");
                String mesaj = scanner.nextLine();

                if (mesaj.trim().isEmpty()) continue;

                datagramSayisi++;
                gidenSayisi++;
                byte[] sendData = mesaj.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, serverAddress, SERVER_PORT
                );

                String gondermeZamani = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                long baslangic = System.currentTimeMillis();

                System.out.println("\n┌────────────────────────────────────────────────────────────┐");
                System.out.println("│ [" + gondermeZamani + "] DATAGRAM #" + datagramSayisi + " GÖNDERİLİYOR");
                System.out.println("├────────────────────────────────────────────────────────────┤");
                System.out.println("│ UDP Başlık: 8 byte");
                System.out.println("│ Veri Boyutu: " + sendData.length + " byte");
                System.out.println("│ Toplam     : " + (8 + sendData.length) + " byte");
                System.out.println("│ Mesaj      : \"" + mesaj + "\"");
                System.out.println("│ [UDP] UYARI: Paketin ulaşacağı garanti DEĞİL!");
                System.out.println("└────────────────────────────────────────────────────────────┘");

                socket.send(sendPacket);

                // Yanıt bekleme
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivePacket);

                    long rtt = System.currentTimeMillis() - baslangic;
                    String alinisZamani = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                    String yanit = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    System.out.println("\n✅ [" + alinisZamani + "] YANIT ALINDI (RTT: " + rtt + " ms)");
                    System.out.println("   Yanit: \"" + yanit + "\"");
                    System.out.println("   [UDP] Bu sefer paket hedefe ulasti!\n");

                } catch (SocketTimeoutException e) {
                    kayipSayisi++;
                    System.out.println("\n [" + gondermeZamani + "] ZAMAN ASIMI! (Timeout: " + TIMEOUT_MS + "ms)");
                    System.out.println("   Paket KAYBOLDU veya cevap gelmedi!");
                    System.out.println("   [UDP] TCP'den farklı olarak otomatik yeniden gönderim YOK.");
                    System.out.println("   Toplam kayip: " + kayipSayisi + "/" + gidenSayisi + "\n");
                }

                if (mesaj.equalsIgnoreCase("quit")) {
                    System.out.println("[UDP] Cikis: El siksismasi gerekmiyor, direkt cikiliyor.");
                    break;
                }
            }
            scanner.close();
            
            System.out.println("\n═══════════════════════════════════════════════════════════════");
            System.out.println("  UDP İSTATİSTİKLER");
            System.out.println("  Gönderilen datagram: " + gidenSayisi);
            System.out.println("  Kaybolan datagram  : " + kayipSayisi);
            System.out.println("  Başarı oranı       : %" + ((gidenSayisi - kayipSayisi) * 100 / gidenSayisi));
            System.out.println("═══════════════════════════════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("[HATA] UDP hatasi: " + e.getMessage());
        }

        System.out.println("\n[UDP] İstemci sonlandirildi.");
    }
}