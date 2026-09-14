package com.datalink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FlinkService {

    @Value("${flink.gateway.url:http://localhost:8083}")
    private String gatewayUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String sessionHandle;
    private final Map<String, String> jobOperationMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            log.info("Flink SQL Gateway 未配置，Flink 相关功能暂不可用");
            return;
        }
        try {
            createSession();
            log.info("Flink SQL Gateway 连接成功: {}", gatewayUrl);
        } catch (Exception e) {
            log.warn("Flink SQL Gateway 连接失败，Flink 功能暂不可用: {}", e.getMessage());
        }
    }

    private synchronized void createSession() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    gatewayUrl + "/v1/sessions", request, String.class);

            JsonNode node = objectMapper.readTree(response.getBody());
            sessionHandle = node.get("sessionHandle").asText();
            log.info("Flink SQL Gateway Session 创建成功: {}", sessionHandle);
        } catch (Exception e) {
            throw new RuntimeException("创建 SQL Gateway Session 失败: " + e.getMessage(), e);
        }
    }

    private void ensureSession() {
        if (sessionHandle == null) {
            createSession();
        }
    }

    public interface LogCallback {
        void onLog(String level, String message);
        void onResult(Map<String, Object> result);
        void onError(String error);
    }

    /**
     * 执行 Flink SQL（同步），通过 SQL Gateway
     */
    public Map<String, Object> executeSQL(String sql) {
        return executeSQL(sql, null);
    }

    /**
     * 执行 Flink SQL（同步），通过 SQL Gateway，支持实时日志回调
     */
    public Map<String, Object> executeSQL(String sql, LogCallback callback) {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();

        ensureSession();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(
                    Map.of("statement", sql));
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            String url = gatewayUrl + "/v1/sessions/" + sessionHandle + "/statements";
            String execLog = "[执行] " + (sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
            logs.add(execLog);
            pushLog(callback, "INFO", execLog);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            String opHandle = node.get("operationHandle").asText();
            String submitLog = "[提交] 操作句柄: " + opHandle;
            logs.add(submitLog);
            pushLog(callback, "INFO", submitLog);

            long deadline = System.currentTimeMillis() + 120_000;
            String statusUrl = gatewayUrl + "/v1/sessions/" + sessionHandle
                    + "/operations/" + opHandle + "/status";

            while (System.currentTimeMillis() < deadline) {
                ResponseEntity<String> statusResp = restTemplate.getForEntity(statusUrl, String.class);
                JsonNode statusNode = objectMapper.readTree(statusResp.getBody());
                String status = statusNode.get("status").asText();

                String runningLog = "[状态] " + status;
                pushLog(callback, "INFO", runningLog);

                if ("FINISHED".equals(status)) {
                    String finishLog = "[完成] 语句执行完毕";
                    logs.add(finishLog);
                    pushLog(callback, "OK", finishLog);
                    break;
                } else if ("ERROR".equals(status) || "CANCELED".equals(status) || "CLOSED".equals(status)) {
                    String errorMsg = statusNode.has("error") ? statusNode.get("error").asText() : status;
                    logs.add("[错误] " + errorMsg);
                    pushLog(callback, "ERROR", errorMsg);
                    result.put("success", false);
                    result.put("error", errorMsg);
                    result.put("duration", System.currentTimeMillis() - startTime);
                    result.put("hiveLogs", logs);
                    return result;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 等待结果完全就绪
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String resultUrl = gatewayUrl + "/v1/sessions/" + sessionHandle
                    + "/operations/" + opHandle + "/result/0";
            ResponseEntity<String> resultResp = restTemplate.getForEntity(resultUrl, String.class);
            JsonNode resultNode = objectMapper.readTree(resultResp.getBody());

            boolean isQuery = resultNode.has("isQueryResult") && resultNode.get("isQueryResult").asBoolean();
            if (isQuery && resultNode.has("results")) {
                JsonNode results = resultNode.get("results");
                List<String> columns = new ArrayList<>();
                if (results.has("columns")) {
                    for (JsonNode col : results.get("columns")) {
                        columns.add(col.get("name").asText());
                    }
                }
                List<List<Object>> rows = new ArrayList<>();
                if (results.has("data")) {
                    int maxRows = 1000;
                    int count = 0;
                    for (JsonNode row : results.get("data")) {
                        if (count >= maxRows) break;
                        List<Object> rowList = new ArrayList<>();
                        if (row.has("fields")) {
                            for (JsonNode field : row.get("fields")) {
                                rowList.add(jsonNodeToValue(field));
                            }
                        }
                        rows.add(rowList);
                        count++;
                    }
                }
                result.put("success", true);
                result.put("columns", columns);
                result.put("rows", rows);
                result.put("rowCount", rows.size());
                result.put("type", "query");
            } else {
                result.put("success", true);
                result.put("message", "执行完成");
                result.put("columns", Collections.emptyList());
                result.put("rows", Collections.emptyList());
                result.put("type", "ddl");
            }

            result.put("duration", System.currentTimeMillis() - startTime);
            result.put("hiveLogs", logs);
            return result;
        } catch (Exception e) {
            log.error("Flink SQL 执行失败: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("duration", System.currentTimeMillis() - startTime);
            result.put("hiveLogs", logs);
            return result;
        }
    }

    /**
     * 异步执行 Flink SQL，通过回调推送日志和结果
     */
    public void executeSQLAsync(String sql, String taskId, LogCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                callback.onLog("INFO", "开始执行 Flink SQL 任务（SQL Gateway）...");
                Map<String, Object> result = executeSQL(sql, callback);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    callback.onResult(result);
                } else {
                    callback.onError(String.valueOf(result.getOrDefault("error", "未知错误")));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
    }

    private void pushLog(LogCallback callback, String level, String message) {
        if (callback != null) {
            try {
                callback.onLog(level, message);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取 SQL 执行计划（通过 SQL Gateway 执行 EXPLAIN）
     */
    public String explainSQL(String sql) {
        ensureSession();

        try {
            String explainSql = "EXPLAIN " + sql;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(
                    Map.of("statement", explainSql));
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            String url = gatewayUrl + "/v1/sessions/" + sessionHandle + "/statements";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            String opHandle = node.get("operationHandle").asText();

            long deadline = System.currentTimeMillis() + 60_000;
            String statusUrl = gatewayUrl + "/v1/sessions/" + sessionHandle
                    + "/operations/" + opHandle + "/status";

            while (System.currentTimeMillis() < deadline) {
                ResponseEntity<String> statusResp = restTemplate.getForEntity(statusUrl, String.class);
                JsonNode statusNode = objectMapper.readTree(statusResp.getBody());
                String status = statusNode.get("status").asText();

                if ("FINISHED".equals(status)) break;
                if ("ERROR".equals(status) || "CANCELED".equals(status) || "CLOSED".equals(status)) {
                    return "获取执行计划失败: " + status;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String resultUrl = gatewayUrl + "/v1/sessions/" + sessionHandle
                    + "/operations/" + opHandle + "/result/0";
            ResponseEntity<String> resultResp = restTemplate.getForEntity(resultUrl, String.class);
            JsonNode resultNode = objectMapper.readTree(resultResp.getBody());

            if (resultNode.has("results") && resultNode.get("results").has("data")) {
                StringBuilder plan = new StringBuilder();
                for (JsonNode row : resultNode.get("results").get("data")) {
                    if (row.has("fields")) {
                        for (JsonNode field : row.get("fields")) {
                            plan.append(field.asText()).append("\n");
                        }
                    }
                }
                return plan.toString();
            }
            return "执行计划为空";
        } catch (Exception e) {
            log.error("获取执行计划失败: {}", e.getMessage());
            return "获取执行计划失败: " + e.getMessage();
        }
    }

    /**
     * 取消正在运行的任务
     */
    public boolean cancelJob(String jobId) {
        // SQL Gateway 的取消通过关闭操作实现
        return false;
    }

    public List<String> getRunningJobIds() {
        return new ArrayList<>();
    }

    private Object jsonNodeToValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isTextual()) return node.asText();
        return node.asText();
    }
}