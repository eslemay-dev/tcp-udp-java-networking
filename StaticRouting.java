import java.util.*;

/**
 * Statik IP Yönlendirme Simülasyonu
 * Ağ Katmanı - Manuel Yönlendirme Tablosu (Routing Table)
 *
 * Statik yönlendirmede, ağ yöneticisi rotaları elle tanımlar.
 * Avantaj  : Düşük overhead, öngörülebilir, güvenli
 * Dezavantaj: Ölçeklenemez, topoloji değişimlerine adapte olmaz
 */
public class StaticRouting {

    // Yönlendirme tablosu girişi
    static class RouteEntry {
        String networkAddress;    // Hedef ağ adresi
        String subnetMask;        // Alt ağ maskesi
        String nextHop;           // Sonraki atlama (next hop) IP
        String exitInterface;     // Çıkış arayüzü
        int    metrik;            // Metrik (hop sayısı)
        String tur;               // Rota türü (Statik/Doğrudan Bağlı)

        RouteEntry(String network, String mask, String nextHop, String iface, int metrik, String tur) {
            this.networkAddress = network;
            this.subnetMask     = mask;
            this.nextHop        = nextHop;
            this.exitInterface  = iface;
            this.metrik         = metrik;
            this.tur            = tur;
        }
    }

    // Paket (simülasyon)
    static class Packet {
        String sourceIP;
        String destIP;
        String payload;

        Packet(String src, String dst, String data) {
            this.sourceIP = src;
            this.destIP   = dst;
            this.payload  = data;
        }
    }

    // Yönlendirici
    static class StaticRouter {
        String routerName;
        List<RouteEntry> routingTable = new ArrayList<>();

        StaticRouter(String name) {
            this.routerName = name;
        }

        // Statik rota ekle (ağ yöneticisi tarafından elle yapılır)
        void addStaticRoute(String network, String mask, String nextHop, String iface, int metrik) {
            routingTable.add(new RouteEntry(network, mask, nextHop, iface, metrik, "Statik"));
            System.out.println("[" + routerName + "] Statik rota eklendi: " + network + "/" + maskToCidr(mask) +
                               " via " + nextHop + " (metrik: " + metrik + ")");
        }

        // Doğrudan bağlı ağ ekle
        void addConnectedRoute(String network, String mask, String iface) {
            routingTable.add(new RouteEntry(network, mask, "0.0.0.0", iface, 0, "Bagli"));
            System.out.println("[" + routerName + "] Doğrudan bağlı ağ: " + network + "/" + maskToCidr(mask) +
                               " (" + iface + ")");
        }

        // En uzun önek eşleşmesi (Longest Prefix Match)
        RouteEntry findBestRoute(String destIP) {
            RouteEntry bestMatch = null;
            int longestPrefix = -1;

            for (RouteEntry entry : routingTable) {
                if (isInNetwork(destIP, entry.networkAddress, entry.subnetMask)) {
                    int prefixLen = maskToCidr(entry.subnetMask);
                    if (prefixLen > longestPrefix) {
                        longestPrefix = prefixLen;
                        bestMatch = entry;
                    }
                }
            }
            return bestMatch;
        }

        // Paketi yönlendir
        void routePacket(Packet packet) {
            System.out.println("\n  ──────────────────────────────────────────");
            System.out.println("  [" + routerName + "] PAKET ALINDI");
            System.out.println("  Kaynak IP : " + packet.sourceIP);
            System.out.println("  Hedef IP  : " + packet.destIP);
            System.out.println("  Payload   : " + packet.payload);
            System.out.println("  [" + routerName + "] Yönlendirme tablosu araştırılıyor...");

            RouteEntry route = findBestRoute(packet.destIP);

            if (route != null) {
                System.out.println("  ✓ Eşleşen rota bulundu!");
                System.out.println("    Hedef Ağ    : " + route.networkAddress + "/" + maskToCidr(route.subnetMask));
                System.out.println("    Rota Türü   : " + route.tur);
                System.out.println("    Sonraki Hop : " + (route.nextHop.equals("0.0.0.0") ? "Doğrudan teslim" : route.nextHop));
                System.out.println("    Çıkış Arayz.: " + route.exitInterface);
                System.out.println("    Metrik      : " + route.metrik);
                System.out.println("  → Paket iletiliyor: " + route.exitInterface + " arayüzünden");
            } else {
                System.out.println("  ✗ Eşleşen rota BULUNAMADI!");
                System.out.println("    Paket atılıyor (ICMP Destination Unreachable gönderilecek)");
            }
        }

        // Yönlendirme tablosunu göster
        void printRoutingTable() {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════════════╗");
            System.out.println("║         [" + routerName + "] STATİK YÖNLENDİRME TABLOSU                          ║");
            System.out.println("╠══════════════╦═══════════════╦════════════════╦═════════╦═══════╦══════════╣");
            System.out.printf("║ %-14s║ %-15s║ %-16s║ %-9s║ %-7s║ %-8s║%n",
                "Ağ Adresi", "Alt Ağ Maskesi", "Sonraki Hop", "Çıkış Arayz", "Metrik", "Tür");
            System.out.println("╠══════════════╬═══════════════╬════════════════╬═════════╬═══════╬══════════╣");

            for (RouteEntry e : routingTable) {
                System.out.printf("║ %-14s║ %-15s║ %-16s║ %-9s║ %-7d║ %-8s║%n",
                    e.networkAddress, e.subnetMask,
                    e.nextHop.equals("0.0.0.0") ? "Dogrudan" : e.nextHop,
                    e.exitInterface, e.metrik, e.tur);
            }
            System.out.println("╚══════════════╩═══════════════╩════════════════╩═════════╩═══════╩══════════╝");
        }

        // Yardımcı: IP ağda mı?
        boolean isInNetwork(String ip, String network, String mask) {
            try {
                long ipLong      = ipToLong(ip);
                long networkLong = ipToLong(network);
                long maskLong    = ipToLong(mask);
                return (ipLong & maskLong) == (networkLong & maskLong);
            } catch (Exception e) {
                return false;
            }
        }

        long ipToLong(String ip) {
            String[] parts = ip.split("\\.");
            long result = 0;
            for (String part : parts) {
                result = result * 256 + Long.parseLong(part);
            }
            return result;
        }

        int maskToCidr(String mask) {
            long m = ipToLong(mask);
            int count = 0;
            while ((m & 0x80000000L) != 0) {
                count++;
                m <<= 1;
            }
            return count;
        }
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       STATİK IP YÖNLENDİRME SİMÜLASYONU                     ║");
        System.out.println("║  Ağ Topolojisi:                                              ║");
        System.out.println("║  PC1(192.168.1.x) -- R1 -- R2 -- PC2(10.0.0.x)              ║");
        System.out.println("║                       |                                      ║");
        System.out.println("║                      R3 -- PC3(172.16.0.x)                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // R1 Yönlendiricisi
        System.out.println("═══ R1 YÖNLENDİRİCİSİ YAPILANDIRMASI ═══");
        StaticRouter r1 = new StaticRouter("R1");
        r1.addConnectedRoute("192.168.1.0", "255.255.255.0", "eth0");  // LAN 1
        r1.addConnectedRoute("203.0.113.0", "255.255.255.252", "eth1"); // R1-R2 arası bağlantı
        r1.addConnectedRoute("198.51.100.0", "255.255.255.252", "eth2");// R1-R3 arası bağlantı
        r1.addStaticRoute("10.0.0.0",   "255.255.255.0", "203.0.113.2", "eth1", 1); // R2 üzerinden
        r1.addStaticRoute("172.16.0.0", "255.255.0.0",   "198.51.100.2","eth2", 1); // R3 üzerinden
        r1.addStaticRoute("0.0.0.0",    "0.0.0.0",       "203.0.113.2", "eth1", 10);// Varsayılan rota

        r1.printRoutingTable();

        // R2 Yönlendiricisi
        System.out.println("\n═══ R2 YÖNLENDİRİCİSİ YAPILANDIRMASI ═══");
        StaticRouter r2 = new StaticRouter("R2");
        r2.addConnectedRoute("10.0.0.0",    "255.255.255.0",   "eth0");
        r2.addConnectedRoute("203.0.113.0", "255.255.255.252", "eth1");
        r2.addStaticRoute("192.168.1.0", "255.255.255.0", "203.0.113.1", "eth1", 1);
        r2.addStaticRoute("172.16.0.0",  "255.255.0.0",   "203.0.113.1", "eth1", 2);

        r2.printRoutingTable();

        // Paket Yönlendirme Simülasyonu
        System.out.println("\n\n═══════════════════════════════════════════════════");
        System.out.println(" PAKET YÖNLENDİRME SİMÜLASYONU");
        System.out.println("═══════════════════════════════════════════════════");

        // Test 1: PC1 → PC2 (farklı ağlar arası)
        System.out.println("\n[TEST 1] PC1 (192.168.1.100) → PC2 (10.0.0.50)");
        r1.routePacket(new Packet("192.168.1.100", "10.0.0.50", "Merhaba PC2!"));

        // Test 2: PC1 → PC3
        System.out.println("\n[TEST 2] PC1 (192.168.1.100) → PC3 (172.16.5.20)");
        r1.routePacket(new Packet("192.168.1.100", "172.16.5.20", "Merhaba PC3!"));

        // Test 3: Rota bulunamaz
        System.out.println("\n[TEST 3] Bilinmeyen Hedef (192.168.1.100 → 8.8.8.8)");
        StaticRouter r1NoDefault = new StaticRouter("R1-NoDefault");
        r1NoDefault.addConnectedRoute("192.168.1.0", "255.255.255.0", "eth0");
        r1NoDefault.routePacket(new Packet("192.168.1.100", "8.8.8.8", "Google DNS"));

        System.out.println("\n[TEST 4] Varsayılan rota ile (R1) → İnternet");
        r1.routePacket(new Packet("192.168.1.100", "8.8.8.8", "Google DNS - Varsayılan rota üzerinden"));

        System.out.println("\n\n═══════════════════════════════════════════════════");
        System.out.println(" STATİK YÖNLENDİRME ANALİZİ TAMAMLANDI");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println(" + Manuel yapılandırma: Ağ yöneticisi her rotayı elle tanımlar");
        System.out.println(" + Düşük CPU/bellek kullanımı: Yönlendirme protokolü çalışmaz");
        System.out.println(" + Güvenlik: Yalnızca tanımlı rotalar kullanılır");
        System.out.println(" - Ölçeklenebilirlik: Büyük ağlarda yönetim zorlaşır");
        System.out.println(" - Arıza toleransı: Bağlantı kesildiğinde rota güncellenmez");
    }
}
