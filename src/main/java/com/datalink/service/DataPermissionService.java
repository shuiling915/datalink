package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlDataPermissionMapper;
import com.datalink.mapper.DlRoleMapper;
import com.datalink.mapper.DlUserRoleMapper;
import com.datalink.model.DlDataPermission;
import com.datalink.model.DlRole;
import com.datalink.model.DlUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限服务 — 行级过滤、列级隐藏/脱敏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionService {

    private final DlDataPermissionMapper dataPermissionMapper;
    private final DlUserRoleMapper userRoleMapper;
    private final DlRoleMapper roleMapper;
    private final DataMaskingService dataMaskingService;

    /**
     * 保存数据权限规则
     */
    public DlDataPermission save(DlDataPermission perm, String operator) {
        validate(perm);
        if (perm.getId() != null) {
            perm.setUpdatedAt(LocalDateTime.now());
            dataPermissionMapper.updateById(perm);
        } else {
            perm.setCreatedBy(operator);
            perm.setEnabled(perm.getEnabled() != null ? perm.getEnabled() : 1);
            perm.setCreatedAt(LocalDateTime.now());
            perm.setUpdatedAt(LocalDateTime.now());
            dataPermissionMapper.insert(perm);
        }
        return perm;
    }

    private void validate(DlDataPermission perm) {
        if (perm.getRoleId() == null) throw new IllegalArgumentException("请选择角色");
        if (perm.getTableName() == null || perm.getTableName().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (perm.getPermissionType() == null) {
            throw new IllegalArgumentException("权限类型不能为空");
        }
        if ("ROW_FILTER".equals(perm.getPermissionType())) {
            if (perm.getRuleValue() == null || perm.getRuleValue().trim().isEmpty()) {
                throw new IllegalArgumentException("行过滤条件不能为空");
            }
            perm.setColumnName(null);
        } else {
            if (perm.getColumnName() == null || perm.getColumnName().isEmpty()) {
                throw new IllegalArgumentException("列名不能为空");
            }
        }
    }

    /**
     * 删除权限规则
     */
    public void delete(Long id) {
        dataPermissionMapper.deleteById(id);
    }

    /**
     * 切换启用状态
     */
    public DlDataPermission toggle(Long id) {
        DlDataPermission perm = dataPermissionMapper.selectById(id);
        if (perm == null) throw new IllegalArgumentException("规则不存在");
        perm.setEnabled(perm.getEnabled() == 1 ? 0 : 1);
        perm.setUpdatedAt(LocalDateTime.now());
        dataPermissionMapper.updateById(perm);
        return perm;
    }

    /**
     * 列表查询
     */
    public List<DlDataPermission> list(Long roleId, Long datasourceId, String tableName) {
        QueryWrapper<DlDataPermission> qw = new QueryWrapper<>();
        if (roleId != null) qw.eq("role_id", roleId);
        if (datasourceId != null) qw.eq("datasource_id", datasourceId);
        if (tableName != null && !tableName.isEmpty()) qw.eq("table_name", tableName);
        qw.orderByDesc("created_at");
        return dataPermissionMapper.selectList(qw);
    }

    /**
     * 获取用户的所有角色ID
     */
    public Set<Long> getUserRoleIds(Long userId) {
        List<DlUserRole> userRoles = userRoleMapper.selectList(
                new QueryWrapper<DlUserRole>().eq("user_id", userId));
        return userRoles.stream().map(DlUserRole::getRoleId).collect(Collectors.toSet());
    }

    /**
     * 获取用户针对某表的列级权限（隐藏列 + 脱敏列）
     *
     * @return Map<列名, 脱敏规则(null表示隐藏)>
     */
    public Map<String, String> getColumnPermissions(Long userId, Long datasourceId, String tableName) {
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) return Collections.emptyMap();

        QueryWrapper<DlDataPermission> qw = new QueryWrapper<>();
        qw.in("role_id", roleIds)
                .eq("table_name", tableName)
                .eq("enabled", 1)
                .and(w -> w.eq("permission_type", "HIDE").or().eq("permission_type", "MASK"));
        if (datasourceId != null) {
            qw.and(w -> w.eq("datasource_id", datasourceId).or().isNull("datasource_id"));
        }

        List<DlDataPermission> perms = dataPermissionMapper.selectList(qw);
        Map<String, String> result = new HashMap<>();
        for (DlDataPermission p : perms) {
            // HIDE 优先级高于 MASK
            if ("HIDE".equals(p.getPermissionType())) {
                result.put(p.getColumnName(), null);
            } else if (!result.containsKey(p.getColumnName())) {
                result.put(p.getColumnName(), p.getRuleValue());
            }
        }
        return result;
    }

    /**
     * 获取用户针对某表的行级过滤条件（多个条件用 AND 连接）
     */
    public String getRowFilter(Long userId, Long datasourceId, String tableName) {
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) return null;

        QueryWrapper<DlDataPermission> qw = new QueryWrapper<>();
        qw.in("role_id", roleIds)
                .eq("table_name", tableName)
                .eq("permission_type", "ROW_FILTER")
                .eq("enabled", 1);
        if (datasourceId != null) {
            qw.and(w -> w.eq("datasource_id", datasourceId).or().isNull("datasource_id"));
        }

        List<DlDataPermission> perms = dataPermissionMapper.selectList(qw);
        if (perms.isEmpty()) return null;

        List<String> conditions = perms.stream()
                .map(DlDataPermission::getRuleValue)
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.toList());
        if (conditions.isEmpty()) return null;
        return "(" + String.join(") AND (", conditions) + ")";
    }

    /**
     * 对查询结果应用列级权限（隐藏 + 脱敏）
     * 行数据格式为 List<List<Object>>（与 DataDevelopmentController 保持一致）
     *
     * @param columns  列名列表
     * @param rows     数据行（List<List<Object>>）
     * @param colPerms 列权限 Map<列名, 脱敏规则>
     * @return 处理后的列和行
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> applyColumnPermissions(List<String> columns,
                                                       List<?> rows,
                                                       Map<String, String> colPerms) {
        if (colPerms == null || colPerms.isEmpty()) {
            return Map.of("columns", columns, "rows", rows);
        }

        // 需要隐藏的列索引
        Set<Integer> hideIndexes = new HashSet<>();
        // 需要脱敏的列索引和规则
        Map<Integer, String> maskIndexes = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            if (colPerms.containsKey(col)) {
                String rule = colPerms.get(col);
                if (rule == null) {
                    hideIndexes.add(i);
                } else {
                    maskIndexes.put(i, rule);
                }
            }
        }

        // 过滤列
        List<String> newColumns = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            if (!hideIndexes.contains(i)) {
                newColumns.add(columns.get(i));
            }
        }

        // 处理行数据
        List<List<Object>> newRows = new ArrayList<>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof List)) continue;
            List<Object> row = (List<Object>) rowObj;
            List<Object> newRow = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                if (hideIndexes.contains(i)) continue;
                Object val = row.get(i);
                if (maskIndexes.containsKey(i) && val != null) {
                    newRow.add(applyMask(val.toString(), maskIndexes.get(i)));
                } else {
                    newRow.add(val);
                }
            }
            newRows.add(newRow);
        }

        return Map.of("columns", newColumns, "rows", newRows);
    }

    /**
     * 应用脱敏规则
     */
    private String applyMask(String value, String rule) {
        if (value == null || value.isEmpty()) return value;
        switch (rule.toLowerCase()) {
            case "phone":
                return dataMaskingService.maskPhone(value);
            case "idcard":
                return dataMaskingService.maskIdCard(value);
            case "email":
                return dataMaskingService.maskEmail(value);
            case "bankcard":
                return dataMaskingService.maskBankCard(value);
            case "name":
                return dataMaskingService.maskName(value);
            case "auto":
                return dataMaskingService.mask(value);
            default:
                return "****";
        }
    }
}