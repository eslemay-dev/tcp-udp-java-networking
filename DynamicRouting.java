import java.util.*;

/**
 * Dinamik IP Yönlendirme Simülasyonu - RIP (Routing Information Protocol)
 * Ağ Katmanı - Mesafe Vektör Algoritması (Distance Vector / Bellman-Ford)
 *
 * RIP, ağ topolojisini otomatik öğrenir ve güncellemeleri komşu yönlendiricilere
 * 30 saniyede bir yayınlar. Maksimum 15 hop (sonsuzluk = 16).
 *
 * Dinamik yönlendirme avantajları:
 * - Topoloji değişikliklerine otomatik adapte olur
 * - Yedekli yollar otomatik keşfedilir
 * - Büyük ağlarda yönetim kolaylığı
 */
public class DynamicRouting {

    static class RouteEntry {
        String network;
        String mask;
        int    metric;       // RIP metriki = hop sayısı
        String nextHop;
        String learnedFrom;  // Hangi yönlendiriciden öğrenildi
        long   timestamp;    // Rota yaşı

        RouteEntry(String network, String mask, int metric, String nextHop, String learnedFrom) {
            this.network     = network;
            this.mask        = mask;
            this.metric      = metric;
            this.nextHop     = nextHop;
            this.learnedFrom = learnedFrom;
            this.timestamp   = System.currentTimeMillis();
        }

        boolean isExpired() {
            // RIP: 180 saniye görülmezse invalid, 240 saniye flush
            return (System.currentTimeMillis() - timestamp) > 180000;
        }
    }

    static class RIPRouter {
        String name;
        Map<String, RouteEntry> routingTable = new LinkedHashMap<>();
        List<RIPRouter> neighbors = new ArrayList<>();
        int ripUpdateCount = 0;

        RIPRouter(String name) {
            this.name = name;
        }

        void addNeighbor(RIPRouter neighbor) {
            if (!neighbors.contains(neighbor)) {
                neighbors.add(neighbor);
                System.out.println("[" + name + "] Komşu eklendi: " + neighbor.name);
            }
        }

        // Doğrudan bağlı ağ (metric=0)
        void addDirectlyConnected(String network, String mask) {
            routingTable.put(network, new RouteEntry(network, mask, 0, "0.0.0.0", "Doğrudan"));
            System.out.println("[" + name + "] Doğrudan bağlı ağ: " + network);
        }

        // RIP güncellemesi al ve işle (Bellman-Ford)
        boolean receiveRIPUpdate(String fromRouter, List<RouteEntry> advertisedRoutes) {
            boolean tableChanged = false;
            ripUpdateCount++;

            System.out.println("\n  [" + name + "] RIP güncellemesi alındı: " + fromRouter +
                               " (" + advertisedRoutes.size() + " rota)");

            for (RouteEntry advertised : advertisedRoutes) {
                // Split Horizon: kendi yönünden gelen rotaları kabul etme
                if (advertised.learnedFrom.equals(name)) {
                    System.out.println("    [Split Horizon] " + advertised.network + " atlandı (döngü önleme)");
                    continue;
                }

                int newMetric = advertised.metric + 1; // Her hop +1

                // RIP sonsuzluk kontrolü (15 = max, 16 = ulaşılamaz)
                if (newMetric > 15) {
                    System.out.println("    [RIP] " + advertised.network + " metrik=16, ulaşılamaz!");
                    if (routingTable.containsKey(advertised.network)) {
                        routingTable.remove(advertised.network);
                        System.out.println("    [RIP] Rota silindi (sonsuzluk kuralı)");
                        tableChanged = true;
                    }
                    continue;
                }

                RouteEntry existing = routingTable.get(advertised.network);

                if (existing == null) {
                    // Yeni rota öğrenildi!
                    routingTable.put(advertised.network,
                        new RouteEntry(advertised.network, advertised.mask, newMetric, fromRouter, fromRouter));
                    System.out.println("    ✓ YENİ ROTA öğrenildi: " + advertised.network +
                                       " via " + fromRouter + " (metric=" + newMetric + ")");
                    tableChanged = true;

                } else if (newMetric < existing.metric) {
                    // Daha iyi yol bulundu!
                    System.out.println("    ✓ Daha İYİ YOL: " + advertised.network +
                                       " metric " + existing.metric + " → " + newMetric);
                    routingTable.put(advertised.network,
                        new RouteEntry(advertised.network, advertised.mask, newMetric, fromRouter, fromRouter));
                    tableChanged = true;

                } else if (existing.nextHop.equals(fromRouter)) {
                    // Aynı komşudan güncelleme — timestamp yenile
                    existing.timestamp = System.currentTimeMillis();
                    existing.metric    = newMetric;
                }
            }

            return tableChanged;
        }

        // RIP güncellemesi yayınla (tüm komşulara)
        void sendRIPUpdate() {
            List<RouteEntry> toAdvertise = new ArrayList<>(routingTable.values());

            System.out.println("\n[" + name + "] RIP güncelleme yayını → " + neighbors.size() + " komşuya");
            for (RIPRouter neighbor : neighbors) {
                System.out.println("  → " + neighbor.name + " (" + toAdvertise.size() + " rota gönderiliyor)");
                neighbor.receiveRIPUpdate(name, toAdvertise);
            }
        }

        // Yönlendirme tablosunu yazdır
        void printRoutingTable() {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.printf( "║ %-64s║%n", " [" + name + "] DİNAMİK YÖNLENDİRME TABLOSU (RIP)");
            System.out.println("╠═══════════════╦═══════════════╦══════════╦══════════════╦═════════╣");
            System.out.printf("║ %-15s║ %-15s║ %-10s║ %-14s║ %-9s║%n",
                "Ağ", "Maske", "Metrik", "Sonraki Hop", "Kaynak");
            System.out.println("╠═══════════════╬═══════════════╬══════════╬══════════════╬═════════╣");
            for (RouteEntry e : routingTable.values()) {
                System.out.printf("║ %-15s║ %-15s║ %-10s║ %-14s║ %-9s║%n",
                    e.network, e.mask,
                    e.metric == 0 ? "0 (Bağlı)" : String.valueOf(e.metric),
                    e.metric == 0 ? "Dogrudan" : e.nextHop,
                    e.learnedFrom);
            }
            System.out.println("╚═══════════════╩═══════════════╩══════════╩══════════════╩═════════╝");
            System.out.println("  Toplam rota sayısı: " + routingTable.size() +
                               " | RIP güncelleme sayısı: " + ripUpdateCount);
        }
    }

    // RIP Convergence simülasyonu
    static void simulateRIPConvergence(RIPRouter r1, RIPRouter r2, RIPRouter r3) {
        System.out.println("\n┌────────────────────────────────────────────────────────────────┐");
        System.out.println("│             RIP CONVERGENCE SİMÜLASYONU                       │");
        System.out.println("│  Topoloji: R1 ─── R2 ─── R3                                   │");
        System.out.println("└────────────────────────────────────────────────────────────────┘");

        System.out.println("\n══ TUR 1: İlk RIP güncellemeleri (t=0s) ══");
        r1.sendRIPUpdate();
        r2.sendRIPUpdate();
        r3.sendRIPUpdate();

        System.out.println("\n══ TUR 2: İkinci RIP güncellemeleri (t=30s) ══");
        r1.sendRIPUpdate();
        r2.sendRIPUpdate();
        r3.sendRIPUpdate();

        System.out.println("\n══ TUR 3: Yakınsama tamamlandı (t=60s) ══");
        r1.sendRIPUpdate();
        r2.sendRIPUpdate();
        r3.sendRIPUpdate();
    }

    // Bağlantı kesme (link failure) simülasyonu
    static void simulateLinkFailure(RIPRouter r1, RIPRouter r2) {
        System.out.println("\n\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           BAĞLANTI KESİLME SİMÜLASYONU                      ║");
        System.out.println("║  R1-R2 arası bağlantı kesildi!                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("[DİNAMİK] Statik yönlendirmeden farklı olarak, RIP otomatik");
        System.out.println("[DİNAMİK] olarak topoloji değişikliğini algılar ve tabloyu günceller.");
        System.out.println("[DİNAMİK] Metric=16 (Triggered Update) gönderiliyor...");

        // R2'deki R1 üzerinden öğrenilen rotaları geçersiz kıl
        for (Map.Entry<String, RouteEntry> entry : r2.routingTable.entrySet()) {
            if (entry.getValue().nextHop.equals(r1.name)) {
                entry.getValue().metric = 16; // Sonsuzluk
                System.out.println("  [R2] " + entry.getKey() + " → metric=16 (ulaşılamaz)");
            }
        }
        System.out.println("[DİNAMİK] Triggered Update yayınlanıyor → tüm komşular haberdar ediliyor");
        System.out.println("[DİNAMİK] Ağ, ~30-60 saniye içinde yeni topolojiye adapte olur.");
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     DİNAMİK IP YÖNLENDİRME SİMÜLASYONU - RIP v1                ║");
        System.out.println("║     Algoritma: Bellman-Ford (Mesafe Vektörü)                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        // Yönlendiriciler
        System.out.println("═══ YÖNLENDİRİCİLER OLUŞTURULUYOR ═══");
        RIPRouter r1 = new RIPRouter("R1");
        RIPRouter r2 = new RIPRouter("R2");
        RIPRouter r3 = new RIPRouter("R3");

        // Komşuluk ilişkileri
        System.out.println("\n═══ KOMŞULUK TANIMLANIYOR ═══");
        r1.addNeighbor(r2);
        r2.addNeighbor(r1);
        r2.addNeighbor(r3);
        r3.addNeighbor(r2);

        // Doğrudan bağlı ağlar
        System.out.println("\n═══ DOĞRUDAN BAĞLI AĞLAR ═══");
        r1.addDirectlyConnected("192.168.1.0", "255.255.255.0");  // R1 LAN
        r1.addDirectlyConnected("10.10.1.0",   "255.255.255.252"); // R1-R2 bağlantı
        r2.addDirectlyConnected("10.10.1.0",   "255.255.255.252"); // R1-R2 bağlantı
        r2.addDirectlyConnected("10.10.2.0",   "255.255.255.252"); // R2-R3 bağlantı
        r3.addDirectlyConnected("10.10.2.0",   "255.255.255.252"); // R2-R3 bağlantı
        r3.addDirectlyConnected("172.16.0.0",  "255.255.0.0");     // R3 LAN

        // Başlangıç tabloları (sadece bağlı ağlar)
        System.out.println("\n═══ BAŞLANGIÇ YÖNLENDİRME TABLOLARI (Yakınsama Öncesi) ═══");
        r1.printRoutingTable();
        r2.printRoutingTable();
        r3.printRoutingTable();

        // RIP Convergence (yakınsama)
        simulateRIPConvergence(r1, r2, r3);

        // Yakınsama sonrası tablolar
        System.out.println("\n\n═══ YAKINSAMA SONRASI YÖNLENDİRME TABLOLARI ═══");
        r1.printRoutingTable();
        r2.printRoutingTable();
        r3.printRoutingTable();

        // Bağlantı kesme senaryosu
        simulateLinkFailure(r1, r2);

        System.out.println("\n\n═══════════════════════════════════════════════════");
        System.out.println(" DİNAMİK YÖNLENDİRME ANALİZİ (RIP)");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println(" + Otomatik öğrenme   : Rotalar Bellman-Ford ile keşfedildi");
        System.out.println(" + Adapte olabilirlik : Bağlantı kesimlerine otomatik tepki");
        System.out.println(" + Kolay yönetim      : Elle rota tanımı gerekmez");
        System.out.println(" - Overhead           : 30 sn'de bir RIP broadcast yayını");
        System.out.println(" - Yakınsama süresi   : Büyük ağlarda dakikalar alabilir");
        System.out.println(" - Döngü riski        : Split Horizon + Poisoned Reverse ile azaltılır");
        System.out.println(" - Ölçek sınırı       : Max 15 hop (büyük ağlar için OSPF önerilir)");
    }
}
