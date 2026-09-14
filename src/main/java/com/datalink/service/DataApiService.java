package com.datalink.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.datalink.mapper.DlDataApiLogMapper;
import com.datalink.mapper.DlDataApiMapper;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDataApi;
import com.datalink.model.DlDataApiLog;
import com.datalink.model.DlDatasource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据服务 API — 将 SQL 查询发布为可调用的 REST API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataApiService {

    private final DlDataApiMapper apiMapper;
    private final DlDataApiLogMapper apiLogMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DatasourceConnectionFactory connectionFactory;

    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{(\\w+)\\}");

    // 简单内存缓存：key = apiCode + params hash, value = 缓存结果
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 限流计数器：key = apiCode, value = 每分钟调用时间戳列表
    private final Map<String, List<Long>> rateLimitCounter = new ConcurrentHashMap<>();

    /**
     * 限流检查（滑动窗口，1分钟内调用次数限制）
     * @return true=允许调用, false=超过限流
     */
    private boolean checkRateLimit(DlDataApi api) {
        Integer limit = api.getRateLimit();
        if (limit == null || limit <= 0) return true;

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        String key = api.getApiCode();

        rateLimitCounter.computeIfAbsent(key, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        List<Long> timestamps = rateLimitCounter.get(key);

        // 移除窗口外的时间戳
        timestamps.removeIf(t -> t < windowStart);

        if (timestamps.size() >= limit) {
            return false;
        }
        timestamps.add(now);
        return true;
    }

    /**
     * 保存 API（新增/更新）
     */
    public DlDataApi save(DlDataApi api, String operator) {
        validateApi(api);
        if (api.getId() != null) {
            api.setUpdatedAt(LocalDateTime.now());
            apiMapper.updateById(api);
        } else {
            api.setCreatedBy(operator);
            api.setMethod(api.getMethod() != null ? api.getMethod() : "GET");
            api.setResponseType(api.getResponseType() != null ? api.getResponseType() : "json");
            api.setRowLimit(api.getRowLimit() != null ? api.getRowLimit() : 1000);
            api.setCacheSeconds(api.getCacheSeconds() != null ? api.getCacheSeconds() : 0);
            api.setStatus(api.getStatus() != null ? api.getStatus() : 0);
            api.setCallCount(0L);
            api.setCreatedAt(LocalDateTime.now());
            api.setUpdatedAt(LocalDateTime.now());
            apiMapper.insert(api);
        }
        return api;
    }

    private void validateApi(DlDataApi api) {
        if (api.getApiCode() == null || !api.getApiCode().matches("^[a-zA-Z][a-zA-Z0-9_]{2,63}$")) {
            throw new IllegalArgumentException("API编码必须以字母开头，3-64位字母数字下划线");
        }
        if (api.getApiName() == null || api.getApiName().isEmpty()) {
            throw new IllegalArgumentException("API名称不能为空");
        }
        if (api.getDatasourceId() == null) {
            throw new IllegalArgumentException("请选择数据源");
        }
        if (api.getSqlTemplate() == null || api.getSqlTemplate().trim().isEmpty()) {
            throw new IllegalArgumentException("SQL模板不能为空");
        }
        String upper = api.getSqlTemplate().trim().toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new IllegalArgumentException("仅支持 SELECT 查询语句");
        }
    }

    /**
     * 发布 API
     */
    public DlDataApi publish(Long id) {
        DlDataApi api = apiMapper.selectById(id);
        if (api == null) throw new IllegalArgumentException("API不存在");
        api.setStatus(1);
        api.setUpdatedAt(LocalDateTime.now());
        apiMapper.updateById(api);
        return api;
    }

    /**
     * 下线 API
     */
    public DlDataApi offline(Long id) {
        DlDataApi api = apiMapper.selectById(id);
        if (api == null) throw new IllegalArgumentException("API不存在");
        api.setStatus(0);
        api.setUpdatedAt(LocalDateTime.now());
        apiMapper.updateById(api);
        return api;
    }

    /**
     * 删除 API
     */
    public void delete(Long id) {
        apiMapper.deleteById(id);
    }

    /**
     * 列表查询
     */
    public List<DlDataApi> list(String keyword, Integer status) {
        QueryWrapper<DlDataApi> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("api_code", keyword).or().like("api_name", keyword));
        }
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("created_at");
        return apiMapper.selectList(qw);
    }

    /**
     * 根据 ID 查询
     */
    public DlDataApi getById(Long id) {
        return apiMapper.selectById(id);
    }

    /**
     * 调用 API（核心执行逻辑）
     */
    public Map<String, Object> invoke(String apiCode, Map<String, String> params, String caller) throws Exception {
        long startTime = System.currentTimeMillis();
        DlDataApi api = apiMapper.selectOne(
                new QueryWrapper<DlDataApi>().eq("api_code", apiCode));

        DlDataApiLog apiLog = new DlDataApiLog();
        apiLog.setApiCode(apiCode);
        apiLog.setCaller(caller);
        apiLog.setRequestParams(params != null ? JSON.toJSONString(params) : null);

        try {
            if (api == null) {
                throw new IllegalArgumentException("API不存在: " + apiCode);
            }
            if (api.getStatus() == null || api.getStatus() != 1) {
                throw new IllegalStateException("API未发布: " + apiCode);
            }

            // API Key 鉴权
            String providedKey = params != null ? params.get("_apiKey") : null;
            if (api.getApiKey() != null && !api.getApiKey().isEmpty()) {
                if (providedKey == null || !api.getApiKey().equals(providedKey)) {
                    throw new SecurityException("API Key 无效或缺失");
                }
            }
            // 移除鉴权参数，不参与 SQL 构建
            if (params != null) params.remove("_apiKey");

            // 限流检查
            if (!checkRateLimit(api)) {
                throw new IllegalStateException("API调用频率超限，请稍后再试");
            }

            // 缓存检查
            String cacheKey = buildCacheKey(apiCode, params);
            if (api.getCacheSeconds() != null && api.getCacheSeconds() > 0) {
                CacheEntry entry = cache.get(cacheKey);
                if (entry != null && System.currentTimeMillis() - entry.timestamp < api.getCacheSeconds() * 1000L) {
                    log.debug("API命中缓存: {}", apiCode);
                    apiLog.setResponseStatus("SUCCESS");
                    apiLog.setRowCount(((List<?>) entry.data.get("rows")).size());
                    apiLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
                    saveLog(apiLog, api.getId());
                    return buildResult(entry.data, true);
                }
            }

            // 构建并执行 SQL
            String sql = buildSql(api.getSqlTemplate(), params);
            Map<String, Object> result = executeQuery(api, sql);

            // 写入缓存
            if (api.getCacheSeconds() != null && api.getCacheSeconds() > 0) {
                cache.put(cacheKey, new CacheEntry(result));
            }

            // 更新调用次数
            apiMapper.update(null, new UpdateWrapper<DlDataApi>()
                    .eq("id", api.getId())
                    .setSql("call_count = call_count + 1"));

            apiLog.setApiId(api.getId());
            apiLog.setResponseStatus("SUCCESS");
            apiLog.setRowCount(((List<?>) result.get("rows")).size());
            apiLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            return result;

        } catch (Exception e) {
            log.error("API调用失败: {}", apiCode, e);
            apiLog.setResponseStatus("FAILED");
            apiLog.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
            apiLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            throw e;
        } finally {
            try {
                saveLog(apiLog, api != null ? api.getId() : null);
            } catch (Exception e) {
                log.error("保存API调用日志失败", e);
            }
        }
    }

    private void saveLog(DlDataApiLog apiLog, Long apiId) {
        if (apiId != null) apiLog.setApiId(apiId);
        apiLog.setCreatedAt(LocalDateTime.now());
        apiLogMapper.insert(apiLog);
    }

    private String buildSql(String template, Map<String, String> params) {
        Matcher matcher = PARAM_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        Set<String> usedParams = new HashSet<>();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            usedParams.add(paramName);
            String value = params != null ? params.get(paramName) : null;
            if (value == null) {
                throw new IllegalArgumentException("缺少必填参数: " + paramName);
            }
            // 安全转义：单引号转义，防止 SQL 注入
            String escaped = value.replace("'", "''");
            matcher.appendReplacement(sb, "'" + escaped + "'");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeQuery(DlDataApi api, String sql) throws Exception {
        DlDatasource ds = datasourceMapper.selectById(api.getDatasourceId());
        if (ds == null) throw new IllegalArgumentException("数据源不存在");

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        int limit = api.getRowLimit() != null ? api.getRowLimit() : 1000;

        try (Connection conn = connectionFactory.getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnName(i));
                }
                while (rs.next() && rows.size() < limit) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(columns.get(i - 1), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildResult(Object data, boolean fromCache) {
        Map<String, Object> result = (Map<String, Object>) data;
        result.put("fromCache", fromCache);
        return result;
    }

    private String buildCacheKey(String apiCode, Map<String, String> params) {
        if (params == null || params.isEmpty()) return apiCode;
        TreeMap<String, String> sorted = new TreeMap<>(params);
        return apiCode + ":" + sorted.hashCode();
    }

    /**
     * 调用日志查询
     */
    public List<DlDataApiLog> listLogs(Long apiId, String apiCode, int limit) {
        QueryWrapper<DlDataApiLog> qw = new QueryWrapper<>();
        if (apiId != null) qw.eq("api_id", apiId);
        if (apiCode != null && !apiCode.isEmpty()) qw.eq("api_code", apiCode);
        qw.orderByDesc("created_at").last("LIMIT " + Math.min(limit, 500));
        return apiLogMapper.selectList(qw);
    }

    /**
     * 生成 API Key
     */
    public String generateApiKey() {
        return "dl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * API 调用统计
     */
    public Map<String, Object> getStats(Long apiId, String apiCode, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, Math.min(days, 90)));
        QueryWrapper<DlDataApiLog> qw = new QueryWrapper<>();
        qw.ge("created_at", since);
        if (apiId != null) qw.eq("api_id", apiId);
        if (apiCode != null && !apiCode.isEmpty()) qw.eq("api_code", apiCode);
        List<DlDataApiLog> logs = apiLogMapper.selectList(qw);

        long total = logs.size();
        long success = logs.stream().filter(l -> "SUCCESS".equals(l.getResponseStatus())).count();
        long failed = total - success;
        double avgDuration = logs.stream().mapToInt(l -> l.getDurationMs() != null ? l.getDurationMs() : 0)
                .average().orElse(0);
        long maxDuration = logs.stream().mapToInt(l -> l.getDurationMs() != null ? l.getDurationMs() : 0)
                .max().orElse(0);
        long totalRows = logs.stream().mapToInt(l -> l.getRowCount() != null ? l.getRowCount() : 0).sum();

        // 按天分组
        Map<String, Long> byDay = new LinkedHashMap<>();
        Map<String, Long> successByDay = new LinkedHashMap<>();
        for (DlDataApiLog l : logs) {
            String day = l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate().toString() : "unknown";
            byDay.merge(day, 1L, Long::sum);
            if ("SUCCESS".equals(l.getResponseStatus())) {
                successByDay.merge(day, 1L, Long::sum);
            }
        }

        // 按API分组（仅当未指定单个API时）
        Map<String, Long> byApi = new LinkedHashMap<>();
        if (apiId == null && (apiCode == null || apiCode.isEmpty())) {
            for (DlDataApiLog l : logs) {
                byApi.merge(l.getApiCode(), 1L, Long::sum);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", total);
        result.put("successCalls", success);
        result.put("failedCalls", failed);
        result.put("successRate", total > 0 ? (double) success / total : 0);
        result.put("avgDurationMs", Math.round(avgDuration * 100) / 100.0);
        result.put("maxDurationMs", maxDuration);
        result.put("totalRowsReturned", totalRows);
        result.put("byDay", byDay);
        result.put("successByDay", successByDay);
        result.put("byApi", byApi);
        return result;
    }

    private static class CacheEntry {
        final Map<String, Object> data;
        final long timestamp;

        CacheEntry(Map<String, Object> data) {
            this.data = new LinkedHashMap<>(data);
            this.timestamp = System.currentTimeMillis();
        }
    }
}