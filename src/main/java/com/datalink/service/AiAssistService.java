package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlAiConfigMapper;
import com.datalink.mapper.DlSystemConfigMapper;
import com.datalink.model.DlAiConfig;
import com.datalink.model.DlSystemConfig;
import com.datalink.model.dto.GenerateTableNameRequest;
import com.datalink.util.CryptoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI 辅助开发服务 — 调用 Claude API 实现 NL2SQL、SQL 解释等智能功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistService {

    private final ObjectMapper objectMapper;
    private final DlSystemConfigMapper systemConfigMapper;
    private final DlAiConfigMapper aiConfigMapper;

    @Value("${datalink.ai.api-key:}")
    private String envApiKey;

    @Value("${datalink.ai.model:claude-sonnet-4-6}")
    private String envModel;

    @Value("${datalink.ai.base-url:https://api.anthropic.com}")
    private String envBaseUrl;

    @Value("${datalink.ai.provider:anthropic}")
    private String envProvider;

    @Value("${datalink.crypto.key:}")
    private String cryptoKey;

    @Value("${datalink.ai.agent-url:http://localhost:8000}")
    private String agentUrl;

    // 运行时配置（数据库优先，环境变量兜底）
    private String apiKey;
    private String model;
    private String baseUrl;
    private String provider;  // anthropic / openai / deepseek / bailian / custom

    @PostConstruct
    public void reloadConfig() {
        // 1. 优先从 dl_ai_config 表读取 is_default=1 的配置（API Key 加密存储）
        DlAiConfig defaultCfg = null;
        try {
            List<DlAiConfig> defaults = aiConfigMapper.selectList(
                    new QueryWrapper<DlAiConfig>().eq("is_default", 1).last("LIMIT 1"));
            if (defaults != null && !defaults.isEmpty()) {
                defaultCfg = defaults.get(0);
            }
        } catch (Exception e) {
            log.warn("读取默认 AI 配置失败，回退到 system_config", e);
        }

        if (defaultCfg != null) {
            this.provider = defaultCfg.getProvider() != null ? defaultCfg.getProvider() : envProvider;
            this.baseUrl = defaultCfg.getBaseUrl() != null ? defaultCfg.getBaseUrl() : envBaseUrl;
            this.model = defaultCfg.getModel() != null ? defaultCfg.getModel() : envModel;
            if (defaultCfg.getApiKey() != null && !defaultCfg.getApiKey().isEmpty()) {
                try {
                    this.apiKey = CryptoUtil.decryptSafe(defaultCfg.getApiKey(), cryptoKey);
                } catch (Exception e) {
                    log.warn("解密默认配置的 API Key 失败，回退到环境变量", e);
                    this.apiKey = envApiKey;
                }
            } else {
                this.apiKey = envApiKey;
            }
        } else {
            // 2. 兜底：从 dl_system_config 读取（单套配置入口保存的）
            String dbKey = getDbConfig("ai.api-key");
            if (dbKey != null && !dbKey.isEmpty()) {
                try {
                    this.apiKey = CryptoUtil.decryptSafe(dbKey, cryptoKey);
                } catch (Exception e) {
                    log.warn("解密数据库中的 API Key 失败，回退到环境变量");
                    this.apiKey = envApiKey;
                }
            } else {
                this.apiKey = envApiKey;
            }
            String dbModel = getDbConfig("ai.model");
            this.model = (dbModel != null && !dbModel.isEmpty()) ? dbModel : envModel;
            String dbUrl = getDbConfig("ai.base-url");
            this.baseUrl = (dbUrl != null && !dbUrl.isEmpty()) ? dbUrl : envBaseUrl;
            String dbProvider = getDbConfig("ai.provider");
            this.provider = (dbProvider != null && !dbProvider.isEmpty()) ? dbProvider : envProvider;
        }

        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.warn("未配置 AI API Key —— 请在「系统设置 > AI 配置」中填写，或在 ai-service/.env 中设置 QWEN_API_KEY");
        }
        log.info("AI config loaded: provider={}, model={}, baseUrl={}, keyConfigured={}", provider, model, baseUrl, apiKey != null && !apiKey.isEmpty());
    }

    /** 当前生效的 API Key（来自 ai-service/.env）。仅供服务端内部使用，不对外返回。 */
    public String getApiKey() {
        return apiKey;
    }

    /** 是否已配置 API Key —— 页面用它显示配置状态，不暴露密钥本身。 */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 判断是否使用 OpenAI 兼容格式（百炼/OpenAI/DeepSeek 都走这个格式）
     */
    private boolean isOpenAiCompatible(String p) {
        return "openai".equals(p) || "deepseek".equals(p) || "bailian".equals(p) || "custom".equals(p);
    }

    private String getDbConfig(String key) {
        try {
            DlSystemConfig cfg = systemConfigMapper.selectById(key);
            return cfg != null ? cfg.getConfigValue() : null;
        } catch (Exception e) {
            return null;  // 表不存在等情况，静默降级
        }
    }

    private static final String SYSTEM_PROMPT =
            "你是一个专业的数据工程师 AI 助手，专注于 SQL 开发和数据分析。\n" +
            "你的职责：\n" +
            "1. 将自然语言需求转换为准确的 SQL 语句\n" +
            "2. 解释复杂的 SQL 语句含义\n" +
            "3. 优化 SQL 性能\n" +
            "4. 回答数据工程相关问题\n\n" +
            "规则：\n" +
            "- 默认使用 HiveSQL 语法（支持分区表、ORC 格式等）\n" +
            "- SQL 语句用 ```sql 代码块包裹\n" +
            "- 回答简洁专业，中文回复\n" +
            "- 默认只回答和用户问题直接相关的内容，控制在 3-6 行\n" +
            "- 不主动展开背景、步骤、下一步建议、长表格，除非用户明确要求";

    /**
     * 调用 Claude API 进行对话
     *
     * @param userMessage 用户消息
     * @param context     上下文信息（如表结构、历史对话等）
     * @return AI 回复文本
     */
    public String chat(String userMessage, String context) {
        String agentReply = callAssistAgent("chat", userMessage, context);
        if (agentReply != null && !agentReply.isEmpty()) {
            return agentReply;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            return "AI 功能未配置。请在【系统配置 → AI 配置】中设置 API Key。";
        }

        try {
            String fullMessage = userMessage;
            if (context != null && !context.isEmpty()) {
                fullMessage = "当前上下文：\n" + context + "\n\n用户问题：" + userMessage;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            if (isOpenAiCompatible(provider)) {
                // OpenAI 兼容格式（百炼/OpenAI/DeepSeek）：system 作为 message
                requestBody.put("max_tokens", 4096);
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", SYSTEM_PROMPT);
                messages.add(sysMsg);
            } else {
                // Anthropic 格式：system 是顶层字段
                requestBody.put("max_tokens", 4096);
                requestBody.put("system", SYSTEM_PROMPT);
            }
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", fullMessage);
            messages.add(msg);
            requestBody.put("messages", messages);

            String responseBody = callApi(objectMapper.writeValueAsString(requestBody), provider, apiKey, baseUrl);
            JsonNode root = objectMapper.readTree(responseBody);

            // Anthropic 格式响应
            if (root.has("content") && root.get("content").isArray() && root.get("content").size() > 0) {
                return root.get("content").get(0).get("text").asText();
            }
            // OpenAI 兼容格式响应
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }
            // 错误处理
            if (root.has("error")) {
                String errorMsg = root.get("error").has("message")
                        ? root.get("error").get("message").asText()
                        : root.get("error").toString();
                log.error("AI API 错误: {}", errorMsg);
                return "AI 请求失败: " + errorMsg;
            }
            return "AI 返回格式异常";
        } catch (Exception e) {
            log.error("AI 助手调用异常", e);
            return "AI 请求失败: " + e.getMessage();
        }
    }

    /**
     * NL2SQL：自然语言转 SQL
     */
    public String nl2sql(String question, String tableSchema) {
        String agentReply = callAssistAgent("nl2sql", question, tableSchema);
        if (agentReply != null && !agentReply.isEmpty()) {
            return agentReply;
        }

        String context = "以下是可用的表结构信息：\n" + tableSchema;
        String prompt = "请根据以下需求生成 SQL 语句：\n" + question + "\n\n要求：只返回可执行的 SQL，用 ```sql 包裹。";
        return chat(prompt, context);
    }

    /**
     * SQL 解释
     */
    public String explainSql(String sql) {
        String agentReply = callAssistAgent("explain", "请解释以下 SQL 的含义：\n```sql\n" + sql + "\n```", "");
        if (agentReply != null && !agentReply.isEmpty()) {
            return agentReply;
        }

        String prompt = "请解释以下 SQL 的含义，包括每个部分的作用：\n```sql\n" + sql + "\n```";
        return chat(prompt, null);
    }

    /**
     * SQL 优化建议
     */
    public String optimizeSql(String sql) {
        String agentReply = callAssistAgent("optimize", "请分析以下 SQL 的性能问题并给出优化建议：\n```sql\n" + sql + "\n```", "");
        if (agentReply != null && !agentReply.isEmpty()) {
            return agentReply;
        }

        String prompt = "请分析以下 SQL 的性能问题并给出优化建议：\n```sql\n" + sql + "\n```";
        return chat(prompt, null);
    }

    /**
     * 新建 SQL 脚本时生成标准模型表名/任务名：优先走 Python Agent 的 LLM + RAG 命名规则。
     */
    public Map<String, Object> generateTableName(GenerateTableNameRequest req) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("layer", nz(req.getLayer()));
            body.put("tableType", nz(req.getTableType()));
            body.put("subject", nz(req.getSubject()));
            body.put("subSubject", nz(req.getSubSubject()));
            body.put("description", nz(req.getDescription()));
            body.put("dbName", nz(req.getDbName()));
            String response = callAgentEndpoint("/generate-table-name", objectMapper.writeValueAsString(body), 120000);
            if (response != null && !response.isEmpty()) {
                JsonNode root = objectMapper.readTree(response);
                String tableName = text(root, "table_name");
                if (tableName == null || tableName.isEmpty()) tableName = text(root, "tableName");
                if (tableName != null && !tableName.isEmpty()) {
                    tableName = sanitizeTableName(tableName);
                    result.put("tableName", tableName);
                    result.put("reason", text(root, "reason"));
                    result.put("source", "agent-rag");
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("AI Agent 表名生成失败，降级直连 LLM: {}", e.getMessage());
        }

        String prompt = "你是一个数据仓库命名规范专家。请根据以下信息生成一个标准化的Hive表名：\n"
                + "- 数仓分层：" + req.getLayer() + "\n"
                + "- 表类型：" + req.getTableType() + "\n"
                + "- 主题域：" + req.getSubject() + "\n"
                + "- 二级主题：" + (req.getSubSubject() != null ? req.getSubSubject() : "无") + "\n"
                + "- 模型描述：" + req.getDescription() + "\n"
                + "- 数据库名：" + (req.getDbName() != null ? req.getDbName() : "default") + "\n\n"
                + "命名规范：{分层}_{库名}_{主题}_{描述}_{full/incr}\n"
                + "例如：dwd_mall_trade_order_detail_full\n\n"
                + "请只返回一个表名，不要其他解释。表名全部小写，用下划线连接。";
        result.put("tableName", sanitizeTableName(chat(prompt, null)));
        result.put("source", "direct-llm-fallback");
        return result;
    }

    private String callAssistAgent(String mode, String message, String context) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("session_id", "datalink-dev-" + (mode == null ? "chat" : mode));
            body.put("mode", mode);
            body.put("message", message);
            body.put("context", context == null ? "" : context);
            String response = callAgentEndpoint("/assist", objectMapper.writeValueAsString(body), 120000);
            if (response == null || response.isEmpty()) return null;
            JsonNode root = objectMapper.readTree(response);
            return text(root, "reply");
        } catch (Exception e) {
            log.warn("AI Agent 调用失败，准备降级直连 LLM: {}", e.getMessage());
            return null;
        }
    }

    private String callAgentEndpoint(String path, String body, int readTimeoutMs) throws Exception {
        String base = agentUrl == null || agentUrl.trim().isEmpty() ? "http://localhost:8000" : agentUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        URL url = new URL(base + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            throw new IllegalStateException("Agent 返回空响应，HTTP " + code);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        is.close();
        String response = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        if (code >= 400) {
            throw new IllegalStateException("Agent HTTP " + code + ": " + response);
        }
        return response;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private String sanitizeTableName(String tableName) {
        if (tableName == null) return "";
        return tableName.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private String callApi(String body, String prov, String key, String base) throws Exception {
        boolean openai = isOpenAiCompatible(prov);
        String endpoint = openai ? "/v1/chat/completions" : "/v1/messages";
        URL url = new URL(base + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("Content-Type", "application/json");
        if (openai) {
            conn.setRequestProperty("Authorization", "Bearer " + key);
        } else {
            conn.setRequestProperty("x-api-key", key);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        byte[] bytes = new byte[0];
        if (is != null) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            bytes = baos.toByteArray();
            is.close();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 检查 AI 功能是否可用
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 测试 AI 连接是否正常
     */
    public boolean testConnection(String prov, String testKey, String testBaseUrl, String testModel) {
        if (testKey == null || testKey.isEmpty()) return false;
        try {
            boolean openai = isOpenAiCompatible(prov);
            String base = testBaseUrl != null && !testBaseUrl.isEmpty() ? testBaseUrl : "https://api.anthropic.com";
            String endpoint = openai ? "/v1/chat/completions" : "/v1/messages";
            HttpURLConnection conn = (HttpURLConnection) new URL(base + endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (openai) {
                conn.setRequestProperty("Authorization", "Bearer " + testKey);
            } else {
                conn.setRequestProperty("x-api-key", testKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            String m = testModel != null && !testModel.isEmpty() ? testModel : "claude-sonnet-4-6";
            String body;
            if (openai) {
                body = "{\"model\":\"" + m + "\",\"max_tokens\":10,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
            } else {
                body = "{\"model\":\"" + m + "\",\"max_tokens\":10,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            log.warn("AI connection test failed: {}", e.getMessage());
            return false;
        }
    }

    // ======================== Agent 意图分类 ========================

    /** 意图枚举 */
    public enum Intent {
        CHART,   // 生成图表（对比/趋势/占比）
        REPORT,  // 生成文字报告/分析
        UNKNOWN  // 其他/聊天
    }

    /**
     * 意图分类：先用关键词快速判断，不确定时走 LLM 分类。
     * 关键词匹配是瞬时返回，LLM 分类需要一次 API 调用但更准确。
     */
    public Intent classifyIntent(String message) {
        if (message == null || message.trim().isEmpty()) return Intent.UNKNOWN;

        String m = message.trim();
        // 快速关键词匹配（无需 API 调用）
        if (m.matches(".*(图表|柱状图|折线图|饼图|散点图|趋势图|可视化|用图|画图|图.*对比|图.*占比"
                + "|绘制|渲染|展示.*图|图.*展示|echarts|chart|graph).*")) {
            return Intent.CHART;
        }
        if (m.matches(".*(报告|分析报告|数据分析|汇总报告|总结报告|周报|月报|日报"
                + "|数据报告|报表|报告.*分析|分析.*报告).*")) {
            return Intent.REPORT;
        }
        if (m.matches(".*(你好|谢谢|再见|帮助|你是谁|介绍|功能|能做什么|hello|hi|hey).*")) {
            return Intent.UNKNOWN;
        }

        // 关键词不明确时，用一个极短的 LLM 调用做分类（几乎无延迟）
        if (apiKey == null || apiKey.isEmpty()) {
            // 没配 API Key 时，默认当聊天处理
            return Intent.UNKNOWN;
        }
        try {
            String classifyPrompt = "判断用户意图，只回复一个单词：\n"
                + "- 如果用户想要数据可视化图表，回复 CHART\n"
                + "- 如果用户想要文字分析报告/数据总结，回复 REPORT\n"
                + "- 其他（聊天/问候/知识问答/功能咨询），回复 CHAT\n\n"
                + "用户消息：" + m + "\n\n回复：";

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 10);
            body.put("temperature", 0.0);

            List<Map<String, String>> msgs = new ArrayList<>();
            Map<String, String> um = new HashMap<>();
            um.put("role", "user");
            um.put("content", classifyPrompt);
            msgs.add(um);
            body.put("messages", msgs);

            String raw = callApi(objectMapper.writeValueAsString(body), provider, apiKey, baseUrl);
            JsonNode root = objectMapper.readTree(raw);
            String reply = null;
            if (root.has("choices") && root.get("choices").size() > 0) {
                reply = root.get("choices").get(0).get("message").get("content").asText().trim().toUpperCase();
            } else if (root.has("content") && root.get("content").isArray() && root.get("content").size() > 0) {
                reply = root.get("content").get(0).get("text").asText().trim().toUpperCase();
            }
            if (reply != null) {
                if (reply.contains("CHART")) return Intent.CHART;
                if (reply.contains("REPORT")) return Intent.REPORT;
            }
        } catch (Exception e) {
            log.debug("意图分类 LLM 调用失败，回退到 UNKNOWN: {}", e.getMessage());
        }

        // 默认：图表请求通常包含对比/趋势/占比关键词
        if (m.matches(".*(对比|趋势|占比|排行榜|排名|分布|统计|多少|几个|哪些|分组|汇总).*")) {
            return Intent.CHART;
        }
        return Intent.UNKNOWN;
    }

    /**
     * 生成数据报告：LLM 生成结构化的文字分析报告，
     * 必要时自动执行 SQL 获取实时数据填入报告。
     */
    public String generateReport(String message, String tableSchemas) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI功能未配置，无法生成报告。请在【系统配置 → AI 配置】中设置 API Key。";
        }

        String prompt = "你是一个资深数据分析师。请根据用户需求生成一份专业的数据分析报告。\n\n"
            + "=== 可用的数据表 ===\n" + tableSchemas + "\n"
            + "===========================\n\n"
            + "用户需求：" + message + "\n\n"
            + "输出要求：\n"
            + "1. 用 Markdown 格式输出报告\n"
            + "2. 包含：报告标题、数据概览、关键指标、分析结论\n"
            + "3. 如果有合适的 SQL 能查询数据，在报告中插入 SQL 语句（用 ```sql 包裹）\n"
            + "4. 报告用中文，专业但易读\n"
            + "5. 不要编造数据，需要实际数据的部分标注【需执行SQL获取】\n"
            + "6. 整体控制在 500 字以内";

        return chat(prompt, tableSchemas);
    }

    // ======================== BI 图表 ========================

    private static final String BI_CHART_PROMPT =
        "你是一个Hive数据分析专家。根据用户需求和下面列出的真实数据表，生成HiveSQL查询和图表配置。\n\n"
        + "=== 可用的数据表（只能从这里面选，严禁编造表名或数据） ===\n%s\n"
        + "===========================================================\n\n"
        + "用户需求：%s\n\n"
        + "规则（必须遵守）：\n"
        + "1. 只使用上面列出的真实表名和字段名，严禁编造任何表或字段\n"
        + "2. 如果没有合适的表能满足用户需求，在explanation中诚实说明，sql用空字符串\n"
        + "3. SQL必须是合法HiveSQL，以分号结尾\n"
        + "4. 对比用bar、趋势用line、占比用pie、关联用scatter\n"
        + "5. 未指定时间范围时默认查全量，LIMIT最多500\n"
        + "6. 只返回纯JSON，不要markdown代码块包裹\n"
        + "7. 不要在WHERE中使用子查询（Hive不支持）\n"
        + "8. 不需要额外过滤dt分区，直接查全表即可\n\n"
        + "返回格式：\n"
        + "{\n"
        + "  \"sql\": \"HiveSQL语句\",\n"
        + "  \"chartType\": \"bar/line/pie/scatter\",\n"
        + "  \"title\": \"图表标题\",\n"
        + "  \"xAxis\": \"X轴字段名\",\n"
        + "  \"yAxis\": \"Y轴字段名\",\n"
        + "  \"explanation\": \"解释\"\n"
        + "}";

    public String generateBiChart(String message, String tableSchemas) {
        String prompt = String.format(BI_CHART_PROMPT, tableSchemas, message);

        if (apiKey == null || apiKey.isEmpty()) {
            return "{\"error\":\"AI功能未配置\"}";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            if (isOpenAiCompatible(provider)) {
                requestBody.put("max_tokens", 2048);
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", "你只返回JSON格式的响应，不要有任何其他文字。");
                messages.add(sysMsg);
            } else {
                requestBody.put("max_tokens", 2048);
                requestBody.put("system", "你只返回JSON格式的响应，不要有任何其他文字。");
            }
            Map<String, String> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.add(msg);
            requestBody.put("messages", messages);

            if (isOpenAiCompatible(provider)) {
                requestBody.put("temperature", 1.0);
            }

            String responseBody = callApi(objectMapper.writeValueAsString(requestBody), provider, apiKey, baseUrl);
            log.info("generateBiChart raw (first 300): {}", responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("content") && root.get("content").isArray() && root.get("content").size() > 0) {
                return root.get("content").get(0).get("text").asText();
            }
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }
            return "{\"error\":\"AI返回格式异常\"}";
        } catch (Exception e) {
            log.error("BI chart generation failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}