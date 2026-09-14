package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlSqlFavoriteMapper;
import com.datalink.mapper.DlSqlHistoryMapper;
import com.datalink.model.DlSqlFavorite;
import com.datalink.model.DlSqlHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL查询历史与收藏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlHistoryService {

    private final DlSqlHistoryMapper historyMapper;
    private final DlSqlFavoriteMapper favoriteMapper;

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?:FROM|JOIN|INTO|UPDATE|TABLE)\\s+([`\\w.]+)", Pattern.CASE_INSENSITIVE);

    /**
     * 记录SQL执行历史
     */
    public void recordHistory(Long userId, String username, Long datasourceId, String datasourceName,
                               String databaseName, String sql, String status, Long durationMs,
                               Long affectedRows, String errorMsg, Integer rowLimit, String clientIp) {
        try {
            DlSqlHistory history = new DlSqlHistory();
            history.setUserId(userId);
            history.setUsername(username);
            history.setDatasourceId(datasourceId);
            history.setDatasourceName(datasourceName);
            history.setDatabaseName(databaseName);
            history.setSqlText(sql != null && sql.length() > 5000 ? sql.substring(0, 5000) : sql);
            history.setSqlType(detectSqlType(sql));
            history.setTableName(parseTableName(sql));
            history.setExecutionStatus(status);
            history.setDurationMs(durationMs);
            history.setAffectedRows(affectedRows);
            history.setErrorMsg(errorMsg != null && errorMsg.length() > 1000 ? errorMsg.substring(0, 1000) : errorMsg);
            history.setRowLimit(rowLimit);
            history.setClientIp(clientIp);
            history.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录SQL历史失败: {}", e.getMessage());
        }
    }

    /**
     * 检测SQL类型
     */
    public String detectSqlType(String sql) {
        if (sql == null) return "UNKNOWN";
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("CREATE")) return "CREATE";
        if (trimmed.startsWith("ALTER")) return "ALTER";
        if (trimmed.startsWith("DROP")) return "DROP";
        if (trimmed.startsWith("TRUNCATE")) return "TRUNCATE";
        if (trimmed.startsWith("WITH")) return "WITH";
        return "OTHER";
    }

    /**
     * 解析SQL中的表名
     */
    public String parseTableName(String sql) {
        if (sql == null) return null;
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).replace("`", "");
        }
        return null;
    }

    /**
     * 分页查询历史
     */
    public Page<DlSqlHistory> listHistory(Long userId, String keyword, String sqlType,
                                           String status, LocalDateTime startTime, LocalDateTime endTime,
                                           int page, int size) {
        QueryWrapper<DlSqlHistory> qw = new QueryWrapper<>();
        if (userId != null) qw.eq("user_id", userId);
        if (keyword != null && !keyword.isEmpty()) qw.like("sql_text", keyword);
        if (sqlType != null && !sqlType.isEmpty()) qw.eq("sql_type", sqlType);
        if (status != null && !status.isEmpty()) qw.eq("execution_status", status);
        if (startTime != null) qw.ge("created_at", startTime);
        if (endTime != null) qw.le("created_at", endTime);
        qw.orderByDesc("created_at");
        return historyMapper.selectPage(new Page<>(page, size), qw);
    }

    /**
     * 获取历史详情
     */
    public DlSqlHistory getHistory(Long id) {
        return historyMapper.selectById(id);
    }

    /**
     * 删除历史
     */
    public void deleteHistory(Long id) {
        historyMapper.deleteById(id);
    }

    /**
     * 清空用户历史
     */
    public void clearHistory(Long userId) {
        historyMapper.delete(new QueryWrapper<DlSqlHistory>().eq("user_id", userId));
    }

    // ==================== 收藏功能 ====================

    /**
     * 添加收藏
     */
    public DlSqlFavorite addFavorite(DlSqlFavorite favorite) {
        favorite.setCreatedAt(LocalDateTime.now());
        favorite.setUpdatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return favorite;
    }

    /**
     * 更新收藏
     */
    public DlSqlFavorite updateFavorite(DlSqlFavorite favorite) {
        favorite.setUpdatedAt(LocalDateTime.now());
        favoriteMapper.updateById(favorite);
        return favorite;
    }

    /**
     * 删除收藏
     */
    public void deleteFavorite(Long id) {
        favoriteMapper.deleteById(id);
    }

    /**
     * 查询用户收藏列表
     */
    public List<DlSqlFavorite> listFavorites(Long userId, String keyword, String tag) {
        QueryWrapper<DlSqlFavorite> qw = new QueryWrapper<>();
        if (userId != null) qw.eq("user_id", userId);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("title", keyword).or().like("sql_text", keyword));
        }
        if (tag != null && !tag.isEmpty()) qw.like("tags", tag);
        qw.orderByDesc("created_at");
        return favoriteMapper.selectList(qw);
    }

    /**
     * 收藏详情
     */
    public DlSqlFavorite getFavorite(Long id) {
        return favoriteMapper.selectById(id);
    }

    /**
     * 统计用户SQL执行情况
     */
    public Map<String, Object> getUserStats(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        QueryWrapper<DlSqlHistory> qw = new QueryWrapper<>();
        if (userId != null) qw.eq("user_id", userId);
        qw.ge("created_at", since);
        List<DlSqlHistory> histories = historyMapper.selectList(qw);

        long total = histories.size();
        long success = histories.stream().filter(h -> "SUCCESS".equals(h.getExecutionStatus())).count();
        long failed = histories.stream().filter(h -> "FAILED".equals(h.getExecutionStatus())).count();
        long totalDuration = histories.stream()
                .filter(h -> h.getDurationMs() != null)
                .mapToLong(DlSqlHistory::getDurationMs).sum();

        Map<String, Long> typeCount = new LinkedHashMap<>();
        for (DlSqlHistory h : histories) {
            typeCount.merge(h.getSqlType(), 1L, Long::sum);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalExecutions", total);
        stats.put("successCount", success);
        stats.put("failedCount", failed);
        stats.put("successRate", total > 0 ? (double) success / total : 0);
        stats.put("avgDurationMs", total > 0 ? totalDuration / total : 0);
        stats.put("typeDistribution", typeCount);
        return stats;
    }
}