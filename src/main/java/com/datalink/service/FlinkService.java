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

    @Value("${flink.jobmanager.address:localhost}")
    private String jobmanagerAddress;

    @Value("${flink.jobmanager.port:6123}")
    private String jobmanagerPort;

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
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("execution.target", "remote");
            props.put("jobmanager.rpc.address", jobmanagerAddress);
            props.put("jobmanager.rpc.port", jobmanagerPort);
            props.put("rest.address", jobmanagerAddress);
            props.put("rest.port", "8081");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sessionName", "datalink-session");
            body.put("properties", props);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

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
     * 拆分多语句 SQL 为单条语句列表
     */
    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            } else if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            statements.add(last);
        }
        return statements;
    }

    /**
     * 判断是否为流式 INSERT 语句（需要持续运行）
     */
    private boolean isStreamingInsert(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT") && upper.contains("SELECT");
    }

    /**
     * 判断是否为 SELECT 查询语句（可能是流式查询）
     */
    private boolean isSelectQuery(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("SELECT") || upper.startsWith("WITH");
    }

    /**
     * 从 Flink Gateway 状态响应中提取错误信息
     */
    private String extractErrorMessage(JsonNode statusNode, String fallbackStatus) {
        try {
            if (statusNode.has("errors") && statusNode.get("errors").isArray() && statusNode.get("errors").size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode err : statusNode.get("errors")) {
                    String msg = err.asText("");
                    if (msg.contains("\n")) {
                        msg = msg.substring(0, msg.indexOf('\n'));
                    }
                    if (msg.length() > 300) msg = msg.substring(0, 300) + "...";
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(msg);
                }
                if (sb.length() > 0) return sb.toString();
            }
            if (statusNode.has("error") && !statusNode.get("error").isNull()) {
                String err = statusNode.get("error").asText("");
                if (err.length() > 300) err = err.substring(0, 300) + "...";
                if (!err.isEmpty()) return err;
            }
            if (statusNode.has("status") && "ERROR".equals(statusNode.get("status").asText())) {
                JsonNode errors = statusNode.get("errors");
                if (errors != null && errors.isArray() && errors.size() > 0) {
                    String msg = errors.get(0).asText("");
                    if (msg.contains("\n")) msg = msg.substring(0, msg.indexOf('\n'));
                    if (msg.length() > 300) msg = msg.substring(0, 300) + "...";
                    return msg;
                }
            }
        } catch (Exception e) {
            log.warn("提取 Flink 错误信息失败: {}", e.getMessage());
        }
        return fallbackStatus;
    }

    /**
     * 执行 Flink SQL（同步），通过 SQL Gateway，支持实时日志回调
     * 支持多语句：按分号拆分后逐条执行；流式 INSERT 提交后立即返回（不等待 FINISHED）
     */
    public Map<String, Object> executeSQL(String sql, LogCallback callback) {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();

        ensureSession();

        List<String> statements = splitSqlStatements(sql);
        if (statements.isEmpty()) {
            result.put("success", false);
            result.put("error", "SQL 内容为空");
            result.put("duration", 0L);
            result.put("hiveLogs", logs);
            return result;
        }

        String infoLog = "共 " + statements.size() + " 条语句";
        logs.add(infoLog);
        pushLog(callback, "INFO", infoLog);

        Map<String, Object> lastResult = null;
        boolean isStreaming = false;

        try {
            for (int i = 0; i < statements.size(); i++) {
                String stmt = statements.get(i);
                String stmtInfo = "[语句 " + (i + 1) + "/" + statements.size() + "] "
                        + (stmt.length() > 80 ? stmt.substring(0, 80) + "..." : stmt);
                logs.add(stmtInfo);
                pushLog(callback, "INFO", stmtInfo);

                lastResult = executeSingleStatement(stmt, callback, logs);
                if (!Boolean.TRUE.equals(lastResult.get("success"))) {
                    return lastResult;
                }

                // 流式 INSERT：提交成功后认为任务已启动，不再等待后续语句
                if (isStreamingInsert(stmt)) {
                    isStreaming = true;
                    String streamLog = "[流式任务] 已提交运行，任务持续执行中";
                    logs.add(streamLog);
                    pushLog(callback, "OK", streamLog);
                    break;
                }
            }

            if (lastResult == null) {
                lastResult = new LinkedHashMap<>();
                lastResult.put("success", true);
                lastResult.put("message", "无有效语句");
            }

            lastResult.put("duration", System.currentTimeMillis() - startTime);
            lastResult.put("hiveLogs", logs);
            if (isStreaming) {
                lastResult.put("type", "streaming");
                lastResult.put("message", "流式任务已启动并持续运行");
            }
            return lastResult;
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
     * 执行单条 SQL 语句
     */
    private Map<String, Object> executeSingleStatement(String sql, LogCallback callback, List<String> logs) {
        return executeSingleStatement(sql, callback, logs, true);
    }

    private Map<String, Object> executeSingleStatement(String sql, LogCallback callback, List<String> logs, boolean allowRetry) {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        boolean streaming = isStreamingInsert(sql);
        boolean isSelect = isSelectQuery(sql);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(
                    Map.of("statement", sql));
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            String url = gatewayUrl + "/v1/sessions/" + sessionHandle + "/statements";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode node = objectMapper.readTree(response.getBody());
            String opHandle = node.get("operationHandle").asText();
            String submitLog = "操作句柄: " + opHandle;
            logs.add(submitLog);
            pushLog(callback, "INFO", submitLog);

            // 流式 INSERT 和 SELECT 查询都用较短超时，SELECT 查询获取流式结果
            long waitMs = (streaming || isSelect) ? 15_000 : 120_000;
            long deadline = System.currentTimeMillis() + waitMs;
            String statusUrl = gatewayUrl + "/v1/sessions/" + sessionHandle
                    + "/operations/" + opHandle + "/status";

            String lastStatus = "";
            while (System.currentTimeMillis() < deadline) {
                ResponseEntity<String> statusResp = restTemplate.getForEntity(statusUrl, String.class);
                JsonNode statusNode = objectMapper.readTree(statusResp.getBody());
                String status = statusNode.get("status").asText();
                lastStatus = status;

                if ("FINISHED".equals(status)) {
                    logs.add("执行完毕");
                    pushLog(callback, "OK", "执行完毕");
                    break;
                } else if ("ERROR".equals(status) || "CANCELED".equals(status) || "CLOSED".equals(status)) {
                    String errorMsg = extractErrorMessage(statusNode, status);
                    logs.add("[错误] " + errorMsg);
                    pushLog(callback, "ERROR", errorMsg);
                    result.put("success", false);
                    result.put("error", errorMsg);
                    result.put("duration", System.currentTimeMillis() - startTime);
                    result.put("hiveLogs", logs);
                    return result;
                } else if (streaming && "RUNNING".equals(status)) {
                    // 流式 INSERT 任务确认启动成功
                    logs.add("流式任务已启动运行");
                    pushLog(callback, "OK", "流式任务已启动运行");
                    result.put("success", true);
                    result.put("message", "流式任务已启动运行");
                    result.put("type", "streaming");
                    result.put("operationHandle", opHandle);
                    result.put("duration", System.currentTimeMillis() - startTime);
                    result.put("hiveLogs", logs);
                    return result;
                } else if (isSelect && "RUNNING".equals(status)) {
                    // SELECT 流式查询：等待几秒让数据产生，然后获取结果
                    logs.add("流式查询运行中，正在获取结果...");
                    pushLog(callback, "INFO", "流式查询运行中，正在获取结果...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 流式 INSERT 超时仍在运行，认为已启动
            if (streaming) {
                logs.add("流式任务持续运行中（最后状态: " + lastStatus + "）");
                pushLog(callback, "OK", "流式任务持续运行中");
                result.put("success", true);
                result.put("message", "流式任务已启动运行");
                result.put("type", "streaming");
                result.put("operationHandle", opHandle);
                result.put("duration", System.currentTimeMillis() - startTime);
                result.put("hiveLogs", logs);
                return result;
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
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Flink SQL 执行失败: {}", errMsg);

            // Session 过期（Flink Gateway 重启等）：重建 session 后重试一次
            if (allowRetry && (errMsg.contains("does not exist") || errMsg.contains("Session"))) {
                log.warn("检测到 Flink Session 过期，正在重建并重试...");
                pushLog(callback, "WARN", "Flink Session 过期，正在重建并重试...");
                sessionHandle = null;
                try {
                    ensureSession();
                    return executeSingleStatement(sql, callback, logs, false);
                } catch (Exception retryEx) {
                    errMsg = retryEx.getMessage() != null ? retryEx.getMessage() : retryEx.getClass().getSimpleName();
                    log.error("重建 Session 后重试仍失败: {}", errMsg);
                }
            }

            result.put("success", false);
            result.put("error", errMsg);
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