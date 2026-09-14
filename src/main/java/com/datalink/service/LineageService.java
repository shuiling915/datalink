package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlLineageColumnMapper;
import com.datalink.mapper.DlLineageMapper;
import com.datalink.model.DlLineage;
import com.datalink.model.DlLineageColumn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据血缘服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private final DlLineageMapper lineageMapper;
    private final DlLineageColumnMapper columnMapper;

    // ==================== CRUD ====================

    @Transactional
    public DlLineage save(DlLineage lineage, String operator) {
        lineage.setCreatedBy(operator);
        if (lineage.getId() != null) {
            lineage.setUpdatedAt(LocalDateTime.now());
            lineageMapper.updateById(lineage);
            columnMapper.delete(new QueryWrapper<DlLineageColumn>().eq("lineage_id", lineage.getId()));
        } else {
            lineage.setCreatedAt(LocalDateTime.now());
            lineage.setUpdatedAt(LocalDateTime.now());
            lineageMapper.insert(lineage);
        }

        if (lineage.getColumns() != null) {
            for (DlLineageColumn col : lineage.getColumns()) {
                col.setLineageId(lineage.getId());
                columnMapper.insert(col);
            }
        }
        return getDetail(lineage.getId());
    }

    @Transactional
    public void delete(Long id) {
        columnMapper.delete(new QueryWrapper<DlLineageColumn>().eq("lineage_id", id));
        lineageMapper.deleteById(id);
    }

    public DlLineage getDetail(Long id) {
        DlLineage lineage = lineageMapper.selectById(id);
        if (lineage != null) {
            List<DlLineageColumn> cols = columnMapper.selectList(
                    new QueryWrapper<DlLineageColumn>().eq("lineage_id", id));
            lineage.setColumns(cols);
        }
        return lineage;
    }

    public List<DlLineage> list(String keyword, String transformType) {
        QueryWrapper<DlLineage> qw = new QueryWrapper<>();
        if (transformType != null && !transformType.isEmpty()) {
            qw.eq("transform_type", transformType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("source_table", keyword)
                    .or().like("target_table", keyword)
                    .or().like("job_name", keyword));
        }
        qw.orderByDesc("updated_at");
        return lineageMapper.selectList(qw);
    }

    // ==================== 血缘追溯 ====================

    /**
     * 获取指定表的完整血缘图（上游 + 下游）
     * 返回 nodes 和 edges，用于前端可视化
     */
    public Map<String, Object> getLineageGraph(String dbName, String tableName, int depth) {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> visitedLineage = new HashSet<>();

        String rootKey = dbName + "." + tableName;
        addNode(nodes, dbName, tableName, "center");

        // 向上游追溯
        traceUpstream(dbName, tableName, depth, nodes, edges, visitedLineage);
        // 向下游追溯
        traceDownstream(dbName, tableName, depth, nodes, edges, visitedLineage);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", new ArrayList<>(nodes.values()));
        result.put("edges", edges);
        result.put("totalNodes", nodes.size());
        result.put("totalEdges", edges.size());
        return result;
    }

    private void traceUpstream(String db, String table, int depth,
                               Map<String, Map<String, Object>> nodes,
                               List<Map<String, Object>> edges,
                               Set<String> visitedLineage) {
        if (depth <= 0) return;

        List<DlLineage> upstreams = lineageMapper.selectList(
                new QueryWrapper<DlLineage>()
                        .eq("target_db", db)
                        .eq("target_table", table));

        for (DlLineage line : upstreams) {
            String lineKey = "L" + line.getId();
            if (visitedLineage.contains(lineKey)) continue;
            visitedLineage.add(lineKey);

            addNode(nodes, line.getSourceDb(), line.getSourceTable(), "upstream");
            addEdge(edges, line.getSourceDb(), line.getSourceTable(),
                    db, table, line.getTransformType(), line.getJobName());

            traceUpstream(line.getSourceDb(), line.getSourceTable(),
                    depth - 1, nodes, edges, visitedLineage);
        }
    }

    private void traceDownstream(String db, String table, int depth,
                                 Map<String, Map<String, Object>> nodes,
                                 List<Map<String, Object>> edges,
                                 Set<String> visitedLineage) {
        if (depth <= 0) return;

        List<DlLineage> downstreams = lineageMapper.selectList(
                new QueryWrapper<DlLineage>()
                        .eq("source_db", db)
                        .eq("source_table", table));

        for (DlLineage line : downstreams) {
            String lineKey = "L" + line.getId();
            if (visitedLineage.contains(lineKey)) continue;
            visitedLineage.add(lineKey);

            addNode(nodes, line.getTargetDb(), line.getTargetTable(), "downstream");
            addEdge(edges, db, table,
                    line.getTargetDb(), line.getTargetTable(),
                    line.getTransformType(), line.getJobName());

            traceDownstream(line.getTargetDb(), line.getTargetTable(),
                    depth - 1, nodes, edges, visitedLineage);
        }
    }

    private void addNode(Map<String, Map<String, Object>> nodes,
                         String db, String table, String category) {
        String key = db + "." + table;
        if (!nodes.containsKey(key)) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", key);
            node.put("db", db);
            node.put("table", table);
            node.put("category", category);
            nodes.put(key, node);
        } else {
            // center 优先级最高
            if ("center".equals(category)) {
                nodes.get(key).put("category", "center");
            }
        }
    }

    private void addEdge(List<Map<String, Object>> edges,
                         String sDb, String sTable, String tDb, String tTable,
                         String type, String jobName) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("source", sDb + "." + sTable);
        edge.put("target", tDb + "." + tTable);
        edge.put("type", type);
        edge.put("jobName", jobName);
        edges.add(edge);
    }

    /**
     * 获取表的直接上游表列表
     */
    public List<DlLineage> getUpstreams(String db, String table) {
        return lineageMapper.selectList(
                new QueryWrapper<DlLineage>().eq("target_db", db).eq("target_table", table));
    }

    /**
     * 获取表的直接下游表列表
     */
    public List<DlLineage> getDownstreams(String db, String table) {
        return lineageMapper.selectList(
                new QueryWrapper<DlLineage>().eq("source_db", db).eq("source_table", table));
    }

    /**
     * 统计信息
     */
    public Map<String, Object> getStats() {
        Long total = lineageMapper.selectCount(null);
        List<DlLineage> all = lineageMapper.selectList(null);

        Set<String> tables = new HashSet<>();
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (DlLineage l : all) {
            tables.add(l.getSourceDb() + "." + l.getSourceTable());
            tables.add(l.getTargetDb() + "." + l.getTargetTable());
            byType.merge(l.getTransformType() != null ? l.getTransformType() : "unknown", 1, Integer::sum);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLineage", total);
        stats.put("totalTables", tables.size());
        stats.put("byType", byType);
        return stats;
    }
}