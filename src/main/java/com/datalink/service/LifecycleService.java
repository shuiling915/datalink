package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.mapper.DlLifecycleLogMapper;
import com.datalink.mapper.DlLifecyclePolicyMapper;
import com.datalink.model.*;
import com.datalink.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据生命周期管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleService {

    private final DlLifecyclePolicyMapper policyMapper;
    private final DlLifecycleLogMapper logMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DatasourceConnectionFactory connectionFactory;

    @Value("${datalink.crypto.key}")
    private String cryptoKey;

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5]{1,128}$");

    private void validateIdentifier(String name, String label) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(label + " 包含非法字符: " + name);
        }
    }

    // ==================== 策略 CRUD ====================

    public DlLifecyclePolicy save(DlLifecyclePolicy policy, String operator) {
        validate(policy);
        if (policy.getId() != null) {
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.updateById(policy);
        } else {
            policy.setCreatedBy(operator);
            policy.setEnabled(policy.getEnabled() != null ? policy.getEnabled() : 1);
            if (policy.getScheduleCron() == null) policy.setScheduleCron("0 0 2 * * ?");
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.insert(policy);
        }
        return policy;
    }

    private void validate(DlLifecyclePolicy policy) {
        if (policy.getPolicyName() == null || policy.getPolicyName().isEmpty()) {
            throw new IllegalArgumentException("策略名称不能为空");
        }
        if (policy.getDatabaseName() == null || policy.getTableName() == null) {
            throw new IllegalArgumentException("数据库和表名不能为空");
        }
        if (policy.getPolicyType() == null) {
            throw new IllegalArgumentException("策略类型不能为空");
        }
        if (policy.getRetentionDays() == null || policy.getRetentionDays() <= 0) {
            throw new IllegalArgumentException("保留天数必须大于0");
        }
        validateIdentifier(policy.getDatabaseName(), "数据库名");
        validateIdentifier(policy.getTableName(), "表名");
        if (policy.getPartitionColumn() != null && !policy.getPartitionColumn().isEmpty()) {
            validateIdentifier(policy.getPartitionColumn(), "分区列名");
        }
    }

    public void delete(Long id) {
        policyMapper.deleteById(id);
        logMapper.delete(new QueryWrapper<DlLifecycleLog>().eq("policy_id", id));
    }

    public DlLifecyclePolicy toggle(Long id) {
        DlLifecyclePolicy policy = policyMapper.selectById(id);
        if (policy == null) throw new IllegalArgumentException("策略不存在");
        policy.setEnabled(policy.getEnabled() == 1 ? 0 : 1);
        policy.setUpdatedAt(LocalDateTime.now());
        policyMapper.updateById(policy);
        return policy;
    }

    public List<DlLifecyclePolicy> list(Long datasourceId, String keyword, String policyType) {
        QueryWrapper<DlLifecyclePolicy> qw = new QueryWrapper<>();
        if (datasourceId != null) qw.eq("datasource_id", datasourceId);
        if (policyType != null && !policyType.isEmpty()) qw.eq("policy_type", policyType);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("policy_name", keyword).or().like("table_name", keyword));
        }
        qw.orderByDesc("updated_at");
        return policyMapper.selectList(qw);
    }

    public DlLifecyclePolicy getById(Long id) {
        return policyMapper.selectById(id);
    }

    // ==================== 执行策略 ====================

    /**
     * 手动执行策略
     */
    public DlLifecycleLog execute(Long policyId) {
        DlLifecyclePolicy policy = policyMapper.selectById(policyId);
        if (policy == null) throw new IllegalArgumentException("策略不存在");
        return doExecute(policy);
    }

    /**
     * 执行单个生命周期策略
     */
    public DlLifecycleLog doExecute(DlLifecyclePolicy policy) {
        DlLifecycleLog runLog = new DlLifecycleLog();
        runLog.setPolicyId(policy.getId());
        runLog.setStartedAt(LocalDateTime.now());
        long startMs = System.currentTimeMillis();

        try {
            DlDatasource ds = datasourceMapper.selectById(policy.getDatasourceId());
            if (ds == null) throw new IllegalArgumentException("数据源不存在");

            try (Connection conn = connectionFactory.getConnection(ds)) {
                switch (policy.getPolicyType()) {
                    case "DELETE":
                        executeDelete(conn, policy, runLog);
                        break;
                    case "ARCHIVE":
                        executeArchive(conn, policy, runLog);
                        break;
                    case "COLD":
                        executeColdMark(conn, policy, runLog);
                        break;
                    default:
                        throw new IllegalArgumentException("不支持的策略类型: " + policy.getPolicyType());
                }
            }

            runLog.setRunStatus("SUCCESS");
            policy.setLastRunAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.updateById(policy);

        } catch (Exception e) {
            log.error("生命周期策略执行失败: policyId={}", policy.getId(), e);
            runLog.setRunStatus("FAILED");
            runLog.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
        }

        runLog.setDurationMs((int) (System.currentTimeMillis() - startMs));
        runLog.setFinishedAt(LocalDateTime.now());
        logMapper.insert(runLog);
        return runLog;
    }

    private void executeDelete(Connection conn, DlLifecyclePolicy policy, DlLifecycleLog runLog) throws Exception {
        String table = policy.getTableName();
        String col = policy.getPartitionColumn();
        LocalDate cutoff = LocalDate.now().minusDays(policy.getRetentionDays());
        String dateStr = cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String sql = String.format("DELETE FROM `%s` WHERE `%s` < '%s'", table, col, dateStr);
        runLog.setExecSql(sql);

        try (Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(sql);
            runLog.setAffectedRows((long) affected);
        }
    }

    private void executeArchive(Connection conn, DlLifecyclePolicy policy, DlLifecycleLog runLog) throws Exception {
        String table = policy.getTableName();
        String col = policy.getPartitionColumn();
        String archiveTable = policy.getArchiveTarget() != null ? policy.getArchiveTarget() : table + "_archive";
        LocalDate cutoff = LocalDate.now().minusDays(policy.getRetentionDays());
        String dateStr = cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 确保归档表存在
        String createSql = String.format("CREATE TABLE IF NOT EXISTS `%s` LIKE `%s`", archiveTable, table);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
        }

        // 迁移数据
        String insertSql = String.format("INSERT INTO `%s` SELECT * FROM `%s` WHERE `%s` < '%s'",
                archiveTable, table, col, dateStr);
        runLog.setExecSql(insertSql);
        try (Statement stmt = conn.createStatement()) {
            int affected = stmt.executeUpdate(insertSql);
            runLog.setAffectedRows((long) affected);

            // 删除原表已归档数据
            if (affected > 0) {
                String deleteSql = String.format("DELETE FROM `%s` WHERE `%s` < '%s'", table, col, dateStr);
                stmt.executeUpdate(deleteSql);
            }
        }
    }

    private void executeColdMark(Connection conn, DlLifecyclePolicy policy, DlLifecycleLog runLog) throws Exception {
        // 冷数据标记：给过期数据设置一个 cold_flag=1 字段（如果存在）
        String table = policy.getTableName();
        String col = policy.getPartitionColumn();
        LocalDate cutoff = LocalDate.now().minusDays(policy.getRetentionDays());
        String dateStr = cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 检查是否有 cold_flag 列
        boolean hasColdFlag = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM `" + table + "` LIKE 'cold_flag'")) {
            hasColdFlag = rs.next();
        }

        if (!hasColdFlag) {
            // 尝试添加 cold_flag 列
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE `" + table + "` ADD COLUMN cold_flag TINYINT DEFAULT 0");
                hasColdFlag = true;
            } catch (Exception e) {
                log.warn("添加 cold_flag 列失败: {}", e.getMessage());
            }
        }

        if (hasColdFlag) {
            String sql = String.format("UPDATE `%s` SET cold_flag = 1 WHERE `%s` < '%s' AND cold_flag = 0",
                    table, col, dateStr);
            runLog.setExecSql(sql);
            try (Statement stmt = conn.createStatement()) {
                int affected = stmt.executeUpdate(sql);
                runLog.setAffectedRows((long) affected);
            }
        } else {
            runLog.setAffectedRows(0L);
            runLog.setExecSql("-- 跳过: 表无 cold_flag 列且无法添加");
        }
    }

    // ==================== 日志查询 ====================

    public List<DlLifecycleLog> listLogs(Long policyId, int limit) {
        QueryWrapper<DlLifecycleLog> qw = new QueryWrapper<>();
        if (policyId != null) qw.eq("policy_id", policyId);
        qw.orderByDesc("started_at").last("LIMIT " + Math.min(limit, 200));
        return logMapper.selectList(qw);
    }

    /**
     * 获取所有启用的策略
     */
    public List<DlLifecyclePolicy> listEnabledPolicies() {
        return policyMapper.selectList(new QueryWrapper<DlLifecyclePolicy>().eq("enabled", 1));
    }
}