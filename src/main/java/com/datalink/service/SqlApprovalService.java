package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlSqlApprovalMapper;
import com.datalink.model.DlSqlApproval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SQL 审批服务
 */
@Service
@RequiredArgsConstructor
public class SqlApprovalService {

    private final DlSqlApprovalMapper approvalMapper;
    private final MessageService messageService;

    private static final Set<String> WRITE_SQL_TYPES = new HashSet<>(Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "LOAD", "IMPORT"
    ));

    /** 判断 SQL 是否需要审批（写操作） */
    public boolean needsApproval(String sql) {
        if (sql == null || sql.trim().isEmpty()) return false;
        String type = detectSqlType(sql);
        return WRITE_SQL_TYPES.contains(type);
    }

    /** 识别 SQL 类型 */
    public String detectSqlType(String sql) {
        String trimmed = sql.trim().toUpperCase();
        // 去掉注释
        trimmed = trimmed.replaceAll("/\\*.*?\\*/", "").replaceAll("--.*", "").trim();
        if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("DROP")) return "DROP";
        if (trimmed.startsWith("ALTER")) return "ALTER";
        if (trimmed.startsWith("TRUNCATE")) return "TRUNCATE";
        if (trimmed.startsWith("CREATE")) return "CREATE";
        if (trimmed.startsWith("LOAD")) return "LOAD";
        return "OTHER";
    }

    /** 提交审批申请 */
    public DlSqlApproval submit(String title, String sql, Long datasourceId,
                                String databaseName, String applicant, String remark) {
        DlSqlApproval approval = new DlSqlApproval();
        approval.setTitle(title);
        approval.setSqlText(sql);
        approval.setSqlType(detectSqlType(sql));
        approval.setDatasourceId(datasourceId);
        approval.setDatabaseName(databaseName);
        approval.setApplicant(applicant);
        approval.setApplicantRemark(remark);
        approval.setStatus(DlSqlApproval.STATUS_PENDING);
        approval.setCreatedAt(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);

        messageService.sendAsync("admin", "SQL审批待处理",
                applicant + " 提交了SQL审批: " + title, "approval", approval.getId());
        return approval;
    }

    /** 审批通过 */
    public DlSqlApproval approve(Long id, String approver, String remark) {
        DlSqlApproval approval = approvalMapper.selectById(id);
        if (approval == null) throw new IllegalArgumentException("审批单不存在");
        if (!DlSqlApproval.STATUS_PENDING.equals(approval.getStatus())) {
            throw new IllegalStateException("该审批单已处理，状态: " + approval.getStatus());
        }
        approval.setStatus(DlSqlApproval.STATUS_APPROVED);
        approval.setApprover(approver);
        approval.setApproveRemark(remark);
        approval.setApproveTime(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        messageService.sendAsync(approval.getApplicant(), "SQL审批已通过",
                "您的SQL审批[" + approval.getTitle() + "]已通过，审批人: " + approver,
                "approval", approval.getId());
        return approval;
    }

    /** 审批驳回 */
    public DlSqlApproval reject(Long id, String approver, String remark) {
        DlSqlApproval approval = approvalMapper.selectById(id);
        if (approval == null) throw new IllegalArgumentException("审批单不存在");
        if (!DlSqlApproval.STATUS_PENDING.equals(approval.getStatus())) {
            throw new IllegalStateException("该审批单已处理，状态: " + approval.getStatus());
        }
        approval.setStatus(DlSqlApproval.STATUS_REJECTED);
        approval.setApprover(approver);
        approval.setApproveRemark(remark);
        approval.setApproveTime(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        messageService.sendAsync(approval.getApplicant(), "SQL审批已驳回",
                "您的SQL审批[" + approval.getTitle() + "]已被驳回，审批人: " + approver
                        + (remark != null ? "，原因: " + remark : ""),
                "approval", approval.getId());
        return approval;
    }

    /** 标记已执行 */
    public void markExecuted(Long id, String executor, String result) {
        DlSqlApproval approval = approvalMapper.selectById(id);
        if (approval == null) return;
        approval.setStatus(DlSqlApproval.STATUS_EXECUTED);
        approval.setExecutor(executor);
        approval.setExecuteTime(LocalDateTime.now());
        approval.setExecuteResult(result != null && result.length() > 2000 ? result.substring(0, 2000) : result);
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);
    }

    /** 分页查询 */
    public IPage<DlSqlApproval> page(int pageNum, int pageSize, String status, String applicant) {
        QueryWrapper<DlSqlApproval> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        if (applicant != null && !applicant.isEmpty()) qw.eq("applicant", applicant);
        qw.orderByDesc("created_at");
        return approvalMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    /** 根据ID查询 */
    public DlSqlApproval getById(Long id) {
        return approvalMapper.selectById(id);
    }
}