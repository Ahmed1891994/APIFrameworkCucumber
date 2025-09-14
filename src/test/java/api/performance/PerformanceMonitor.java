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

    public void recordResponseTime(String endpoint, long milliseconds) {
        responseTimes.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(milliseconds);
        logger.info("Recorded response time for {}: {}ms", endpoint, milliseconds);
    }

    public PerformanceReport generateReport() {
        PerformanceReport report = new PerformanceReport();

        responseTimes.forEach((endpoint, times) -> {
            if (!times.isEmpty()) {
                long sum = times.stream().mapToLong(Long::longValue).sum();
                double average = sum / (double) times.size();
                long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0);

                report.addEndpointStats(endpoint, times.size(), average, min, max);

                logger.info("Generated stats for {}: {} requests, avg={}ms, min={}ms, max={}ms",
                        endpoint, times.size(), String.format("%.2f", average), min, max);
            }
        });

        return report;
    }

    public static class PerformanceReport {
        private static final Logger logger = LoggerFactory.getLogger(PerformanceReport.class);
        private final Map<String, EndpointStats> stats = new HashMap<>();

        public void addEndpointStats(String endpoint, int requestCount,
                                     double avgResponseTime, long minResponseTime, long maxResponseTime) {
            stats.put(endpoint, new EndpointStats(requestCount, avgResponseTime, minResponseTime, maxResponseTime));
        }

        public void assertMaxResponseTime(String endpointPattern, long maxAllowedMs) {
            // Find the first endpoint that matches the pattern
            EndpointStats matchingStat = null;
            String matchedEndpoint = null;

            for (Map.Entry<String, EndpointStats> entry : stats.entrySet()) {
                String recordedEndpoint = entry.getKey();
                if (matchesPattern(recordedEndpoint, endpointPattern)) {
                    matchingStat = entry.getValue();
                    matchedEndpoint = recordedEndpoint;
                    break;
                }
            }

            if (matchingStat == null) {
                logger.error("❌ No performance data found for endpoint pattern: {}", endpointPattern);
                throw new AssertionError("No performance data found for endpoint pattern: " + endpointPattern);
            }

            logger.info("📊 Performance check for {} (matched: {}): Max allowed={}ms, Actual max={}ms, Actual avg={}ms",
                    endpointPattern, matchedEndpoint, maxAllowedMs,
                    matchingStat.maxResponseTime, String.format("%.2f", matchingStat.avgResponseTime));

            if (matchingStat.maxResponseTime > maxAllowedMs) {
                logger.error("❌ Response time exceeded for {}: Max allowed={}ms, Actual={}ms",
                        endpointPattern, maxAllowedMs, matchingStat.maxResponseTime);
                throw new AssertionError("Response time exceeded for " + endpointPattern +
                        ". Max allowed: " + maxAllowedMs + "ms, Actual: " + matchingStat.maxResponseTime + "ms");
            }

            logger.info("✅ Response time OK for {}: {}ms <= {}ms",
                    endpointPattern, matchingStat.maxResponseTime, maxAllowedMs);
        }

        private boolean matchesPattern(String actualEndpoint, String pattern) {
            // Simple pattern matching for path parameters
            // Converts pattern like "/authors/{id}" to regex "/authors/\\d+"
            String regexPattern = pattern.replaceAll("\\{[^}]+\\}", "\\\\d+");
            return actualEndpoint.matches(regexPattern);
        }

        public void printReport() {
            logger.info("=== PERFORMANCE REPORT ===");
            stats.forEach((endpoint, stat) -> {
                logger.info("Endpoint: {}", endpoint);
                logger.info("  Requests: {}", stat.requestCount);
                logger.info("  Average Response Time: {}ms", String.format("%.2f", stat.avgResponseTime));
                logger.info("  Min Response Time: {}ms", stat.minResponseTime);
                logger.info("  Max Response Time: {}ms", stat.maxResponseTime);
                logger.info("  -------------------------");
            });
        }

        private static class EndpointStats {
            final int requestCount;
            final double avgResponseTime;
            final long minResponseTime;
            final long maxResponseTime;

            EndpointStats(int requestCount, double avgResponseTime, long minResponseTime, long maxResponseTime) {
                this.requestCount = requestCount;
                this.avgResponseTime = avgResponseTime;
                this.minResponseTime = minResponseTime;
                this.maxResponseTime = maxResponseTime;
            }
        }
    }
}