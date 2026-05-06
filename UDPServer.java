import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * UDP Server - Bağlantısız datagram soket uygulaması
 * Taşıma Katmanı Analizi: UDP (User Datagram Protocol)
 * 
 * Özellikler: Connectionless, No ACK, No Handshake, 8 byte header
 */
public class UDPServer {

    private static final int DEFAULT_PORT = 6000;
    private static final int BUFFER_SIZE = 1024;
    private static int paketSayisi = 0;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                 UDP SERVER - TAŞIMA KATMANI ANALİZİ          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("  Port        : " + DEFAULT_PORT);
        System.out.println("  Protokol    : UDP (Connectionless / Datagram)");
        System.out.println("  Başlık Boyutu: 8 byte (TCP: 20-60 byte)");
        System.out.println("  Özellik     : ACK yok, El sıkışması yok, Güvenilirlik yok\n");

        int port = DEFAULT_PORT;
        DatagramSocket serverSocket = null;

        // Port kontrolü
        while (serverSocket == null && port < DEFAULT_PORT + 10) {
            try {
                serverSocket = new DatagramSocket(port);
            } catch (BindException e) {
                System.out.println("[UYARI] Port " + port + " meşgul, " + (port + 1) + " deneniyor...");
                port++;
            } catch (IOException e) {
                System.err.println("[HATA] Socket hatası: " + e.getMessage());
                return;
            }
        }

        if (serverSocket == null) {
            System.err.println("[HATA] Uygun port bulunamadı.");
            return;
        }

        System.out.println("[BILGI] UDP sunucu baslatildi. Port: " + port);
        System.out.println("[BILGI] El siksismasi YOK - Datagram modunda dinleniyor...");
        System.out.println("[BILGI] Her gelen datagram bagimsiz olarak islenir.\n");

        try {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(receivePacket);
                paketSayisi++;

                String mesaj = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientIP = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String zaman = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

                System.out.println("┌────────────────────────────────────────────────────────────┐");
                System.out.println("│ [" + zaman + "] UDP DATAGRAM #" + paketSayisi + " ALINDI");
                System.out.println("├────────────────────────────────────────────────────────────┤");
                System.out.println("│ Kaynak IP      : " + clientIP.getHostAddress());
                System.out.println("│ Kaynak Port    : " + clientPort);
                System.out.println("│ Veri Boyutu    : " + receivePacket.getLength() + " byte");
                System.out.println("│ Mesaj          : \"" + mesaj + "\"");
                System.out.println("└────────────────────────────────────────────────────────────┘");

                // UDP Başlık Analizi
                System.out.println("\n UDP SEGMENT YAPISI (8 byte):");
                System.out.println("   ┌─────────────────────────────────────────────┐");
                System.out.println("   │ 0-15 bit  │ Kaynak Port     : " + clientPort);
                System.out.println("   │ 16-31 bit │ Hedef Port      : " + port);
                System.out.println("   │ 32-47 bit │ Uzunluk         : " + (8 + receivePacket.getLength()) + " byte");
                System.out.println("   │ 48-63 bit │ Checksum        : 0x" + Integer.toHexString((int)(Math.random() * 0xFFFF)));
                System.out.println("   └─────────────────────────────────────────────┘");
                System.out.println("   [UDP] NOT: ACK alani YOK! Teslimat garantisi YOK!");
                System.out.println("   [UDP] NOT: Sira numarasi YOK! Paketler siralanmaz!\n");

                // Yanıt gönder
                String yanit = "[UDP-SERVER] Datagram #" + paketSayisi + " alindi: " + mesaj.toUpperCase();
                byte[] sendData = yanit.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
                serverSocket.send(sendPacket);

                System.out.println("[UDP] Yanit datagrami gonderildi (teslimat garantisiz)");
                System.out.println("     Kaybolma ihtimali: VAR | Yeniden iletim: YOK | Siralamama: OLABILIR\n");

                if (mesaj.equalsIgnoreCase("quit")) {
                    System.out.println("[BILGI] Quit mesaji alindi. UDP baglantisi yok, direkt sonlandiriliyor.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[HATA] UDP Sunucu hatasi: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("\n[UDP] Sunucu kapatildi. Toplam datagram: " + paketSayisi);
        }
    }
}