package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.*;
import com.datalink.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final DlAssetMapper assetMapper;
    private final DlTableMetaMapper tableMetaMapper;
    private final DlColumnMetaMapper columnMetaMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DlScriptMapper scriptMapper;

    public Map<String, Object> search(String keyword, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("searchedAt", java.time.LocalDateTime.now().toString());

        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("assets", Collections.emptyList());
            result.put("tables", Collections.emptyList());
            result.put("columns", Collections.emptyList());
            result.put("datasources", Collections.emptyList());
            result.put("scripts", Collections.emptyList());
            result.put("total", 0);
            return result;
        }

        String kw = keyword.trim();
        int total = 0;

        // 1. 数据资产目录
        QueryWrapper<DlAsset> assetQw = new QueryWrapper<>();
        assetQw.and(w -> w.like("asset_name", kw)
                .or().like("table_name", kw)
                .or().like("description", kw)
                .or().like("business_domain", kw)
                .or().like("tags", kw));
        assetQw.orderByDesc("access_count").last("LIMIT " + limit);
        List<DlAsset> assets = assetMapper.selectList(assetQw);
        result.put("assets", assets);
        total += assets.size();

        // 2. 表元数据
        QueryWrapper<DlTableMeta> tableQw = new QueryWrapper<>();
        tableQw.and(w -> w.like("table_name", kw)
                .or().like("table_comment", kw)
                .or().like("database_name", kw));
        tableQw.orderByDesc("updated_at").last("LIMIT " + limit);
        List<DlTableMeta> tables = tableMetaMapper.selectList(tableQw);
        result.put("tables", tables);
        total += tables.size();

        // 3. 字段元数据
        QueryWrapper<DlColumnMeta> colQw = new QueryWrapper<>();
        colQw.and(w -> w.like("column_name", kw)
                .or().like("business_name", kw)
                .or().like("business_desc", kw));
        colQw.last("LIMIT " + limit);
        List<DlColumnMeta> columns = columnMetaMapper.selectList(colQw);
        result.put("columns", columns);
        total += columns.size();

        // 4. 数据源
        QueryWrapper<DlDatasource> dsQw = new QueryWrapper<>();
        dsQw.and(w -> w.like("name", kw)
                .or().like("type", kw)
                .or().like("host", kw));
        dsQw.last("LIMIT " + limit);
        List<DlDatasource> datasources = datasourceMapper.selectList(dsQw);
        result.put("datasources", datasources);
        total += datasources.size();

        // 5. SQL脚本
        QueryWrapper<DlScript> scriptQw = new QueryWrapper<>();
        scriptQw.and(w -> w.like("script_name", kw)
                .or().like("content", kw)
                .or().like("description", kw));
        scriptQw.orderByDesc("updated_at").last("LIMIT " + limit);
        List<DlScript> scripts = scriptMapper.selectList(scriptQw);
        result.put("scripts", scripts);
        total += scripts.size();

        result.put("total", total);
        return result;
    }

    public List<Map<String, Object>> searchSuggestions(String keyword, int limit) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return suggestions;

        String kw = keyword.trim();

        // 资产名称建议
        QueryWrapper<DlAsset> assetQw = new QueryWrapper<>();
        assetQw.select("asset_name", "table_name", "description");
        assetQw.like("asset_name", kw).orderByDesc("access_count").last("LIMIT " + limit);
        for (DlAsset a : assetMapper.selectList(assetQw)) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type", "asset");
            s.put("title", a.getAssetName());
            s.put("subtitle", a.getTableName());
            suggestions.add(s);
        }

        // 表名建议
        QueryWrapper<DlTableMeta> tableQw = new QueryWrapper<>();
        tableQw.select("table_name", "table_comment", "database_name");
        tableQw.like("table_name", kw).last("LIMIT " + limit);
        for (DlTableMeta t : tableMetaMapper.selectList(tableQw)) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type", "table");
            s.put("title", t.getTableName());
            s.put("subtitle", t.getDatabaseName() + " - " + t.getTableComment());
            suggestions.add(s);
        }

        // 字段名建议
        QueryWrapper<DlColumnMeta> colQw = new QueryWrapper<>();
        colQw.select("column_name", "business_name", "business_desc");
        colQw.like("column_name", kw).last("LIMIT " + limit);
        for (DlColumnMeta c : columnMetaMapper.selectList(colQw)) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type", "column");
            s.put("title", c.getColumnName());
            s.put("subtitle", c.getBusinessDesc());
            suggestions.add(s);
        }

        if (suggestions.size() > limit * 2) {
            suggestions = suggestions.subList(0, limit * 2);
        }
        return suggestions;
    }
}