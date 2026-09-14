package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlDatasourceGroupMapper;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.model.DlDatasource;
import com.datalink.model.DlDatasourceGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceGroupService {

    private final DlDatasourceGroupMapper groupMapper;
    private final DlDatasourceMapper datasourceMapper;

    public List<DlDatasourceGroup> listGroups() {
        return groupMapper.selectList(new QueryWrapper<DlDatasourceGroup>().orderByAsc("sort_order"));
    }

    public DlDatasourceGroup getGroup(Long id) {
        return groupMapper.selectById(id);
    }

    public DlDatasourceGroup createGroup(DlDatasourceGroup group, String username) {
        group.setCreatedBy(username);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        if (group.getSortOrder() == null) group.setSortOrder(0);
        groupMapper.insert(group);
        return group;
    }

    public DlDatasourceGroup updateGroup(Long id, DlDatasourceGroup group) {
        DlDatasourceGroup existing = groupMapper.selectById(id);
        if (existing == null) return null;
        existing.setGroupName(group.getGroupName());
        existing.setGroupCode(group.getGroupCode());
        existing.setDescription(group.getDescription());
        existing.setSortOrder(group.getSortOrder());
        existing.setUpdatedAt(LocalDateTime.now());
        groupMapper.updateById(existing);
        return existing;
    }

    public boolean deleteGroup(Long id, boolean moveToUngrouped) {
        DlDatasourceGroup group = groupMapper.selectById(id);
        if (group == null) return false;

        if (moveToUngrouped) {
            List<DlDatasource> datasources = datasourceMapper.selectList(
                    new QueryWrapper<DlDatasource>().eq("group_id", id));
            for (DlDatasource ds : datasources) {
                ds.setGroupId(null);
                datasourceMapper.updateById(ds);
            }
        }
        groupMapper.deleteById(id);
        return true;
    }

    public boolean assignDatasourceToGroup(Long datasourceId, Long groupId) {
        DlDatasource ds = datasourceMapper.selectById(datasourceId);
        if (ds == null) return false;
        if (groupId != null) {
            DlDatasourceGroup group = groupMapper.selectById(groupId);
            if (group == null) return false;
        }
        ds.setGroupId(groupId);
        datasourceMapper.updateById(ds);
        return true;
    }

    public List<DlDatasource> getDatasourcesByGroup(Long groupId) {
        QueryWrapper<DlDatasource> qw = new QueryWrapper<>();
        if (groupId == null) {
            qw.isNull("group_id").or().eq("group_id", 0);
        } else {
            qw.eq("group_id", groupId);
        }
        qw.orderByAsc("name");
        return datasourceMapper.selectList(qw);
    }

    public Map<String, Object> getGroupTree() {
        List<DlDatasourceGroup> groups = listGroups();
        List<DlDatasource> allDatasources = datasourceMapper.selectList(null);

        Map<Long, List<DlDatasource>> grouped = new LinkedHashMap<>();
        List<DlDatasource> ungrouped = new ArrayList<>();

        for (DlDatasource ds : allDatasources) {
            if (ds.getGroupId() != null) {
                grouped.computeIfAbsent(ds.getGroupId(), k -> new ArrayList<>()).add(ds);
            } else {
                ungrouped.add(ds);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> groupNodes = new ArrayList<>();
        for (DlDatasourceGroup g : groups) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", g.getId());
            node.put("name", g.getGroupName());
            node.put("code", g.getGroupCode());
            node.put("description", g.getDescription());
            node.put("datasourceCount", grouped.getOrDefault(g.getId(), Collections.emptyList()).size());
            node.put("datasources", grouped.getOrDefault(g.getId(), Collections.emptyList()));
            groupNodes.add(node);
        }

        Map<String, Object> ungroupedNode = new LinkedHashMap<>();
        ungroupedNode.put("id", null);
        ungroupedNode.put("name", "未分组");
        ungroupedNode.put("datasourceCount", ungrouped.size());
        ungroupedNode.put("datasources", ungrouped);

        result.put("groups", groupNodes);
        result.put("ungrouped", ungroupedNode);
        result.put("totalDatasources", allDatasources.size());
        return result;
    }
}