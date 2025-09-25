package api.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceMonitor {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    private final Map<String, List<Long>> responseTimes = new ConcurrentHashMap<>();
    private String lastRequestKey; // Store the key of the last request

    public void recordResponseTime(String method, String endpoint, long milliseconds) {
        String key = method.toUpperCase() + " " + endpoint;
        responseTimes.computeIfAbsent(key, _ -> new ArrayList<>()).add(milliseconds);
        lastRequestKey = key; // Store the last request key
        logger.info("Recorded response time for {}: {}ms", key, milliseconds);
    }

    public PerformanceReport generateReport() {
        PerformanceReport report = new PerformanceReport();

        responseTimes.forEach((key, times) -> {
            if (!times.isEmpty()) {
                long sum = times.stream().mapToLong(Long::longValue).sum();
                double average = sum / (double) times.size();
                long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0);

                report.addEndpointStats(key, times.size(), average, min, max);

                logger.info("Generated stats for {}: {} requests, avg={}ms, min={}ms, max={}ms",
                        key, times.size(), String.format("%.2f", average), min, max);
            }
        });

        return report;
    }

    public String getLastRequestKey() {
        return lastRequestKey;
    }

    public static class PerformanceReport {
        private static final Logger logger = LoggerFactory.getLogger(PerformanceReport.class);
        private final Map<String, EndpointStats> stats = new HashMap<>();

        public void addEndpointStats(String key, int requestCount,
                                     double avgResponseTime, long minResponseTime, long maxResponseTime) {
            stats.put(key, new EndpointStats(requestCount, avgResponseTime, minResponseTime, maxResponseTime));
        }

        public void assertMaxResponseTime(String key, long maxAllowedMs) {
            EndpointStats matchingStat = stats.get(key);

            if (matchingStat == null) {
                logger.error("❌ No performance data found for: {}", key);
                throw new AssertionError("No performance data found for: " + key);
            }

            logger.info("📊 Performance check for {}: Max allowed={}ms, Actual max={}ms, Actual avg={}ms",
                    key, maxAllowedMs, matchingStat.maxResponseTime,
                    String.format("%.2f", matchingStat.avgResponseTime));

            if (matchingStat.maxResponseTime > maxAllowedMs) {
                logger.error("❌ Response time exceeded for {}: Max allowed={}ms, Actual={}ms",
                        key, maxAllowedMs, matchingStat.maxResponseTime);
                throw new AssertionError("Response time exceeded for " + key +
                        ". Max allowed: " + maxAllowedMs + "ms, Actual: " + matchingStat.maxResponseTime + "ms");
            }

            logger.info("✅ Response time OK for {}: {}ms <= {}ms",
                    key, matchingStat.maxResponseTime, maxAllowedMs);
        }

        public void assertLastRequestMaxResponseTime(String lastRequestKey, long maxAllowedMs) {
            assertMaxResponseTime(lastRequestKey, maxAllowedMs);
        }

        public void printReport() {
            logger.info("=== PERFORMANCE REPORT ===");
            stats.forEach((key, stat) -> {
                logger.info("Endpoint: {}", key);
                logger.info("  Requests: {}", stat.requestCount);
                logger.info("  Average Response Time: {}ms", String.format("%.2f", stat.avgResponseTime));
                logger.info("  Min Response Time: {}ms", stat.minResponseTime);
                logger.info("  Max Response Time: {}ms", stat.maxResponseTime);
                logger.info("  -------------------------");
            });
        }

        private record EndpointStats(int requestCount, double avgResponseTime, long minResponseTime,
                                     long maxResponseTime) {
        }
    }
}