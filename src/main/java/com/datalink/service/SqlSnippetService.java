package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlSqlSnippetMapper;
import com.datalink.model.DlSqlSnippet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlSnippetService {

    private final DlSqlSnippetMapper snippetMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<DlSqlSnippet> listSnippets(int page, int size, String category, String keyword,
                                            String username, boolean includePublic) {
        QueryWrapper<DlSqlSnippet> qw = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            qw.and(w -> w.like("title", kw).or().like("sql_text", kw)
                    .or().like("description", kw).or().like("tags", kw));
        }
        if (includePublic) {
            qw.and(w -> w.eq("created_by", username).or().eq("is_public", 1));
        } else {
            qw.eq("created_by", username);
        }
        qw.orderByDesc("usage_count").orderByDesc("created_at");
        return snippetMapper.selectPage(new Page<>(page, size), qw);
    }

    public DlSqlSnippet getSnippet(Long id) {
        return snippetMapper.selectById(id);
    }

    public DlSqlSnippet createSnippet(DlSqlSnippet snippet, String username) {
        snippet.setCreatedBy(username);
        snippet.setSnippetType(snippet.getSnippetType() != null ? snippet.getSnippetType() : "custom");
        snippet.setIsPublic(snippet.getIsPublic() != null ? snippet.getIsPublic() : 0);
        snippet.setUsageCount(0);
        snippet.setCreatedAt(java.time.LocalDateTime.now());
        snippet.setUpdatedAt(java.time.LocalDateTime.now());
        snippetMapper.insert(snippet);
        return snippet;
    }

    public DlSqlSnippet updateSnippet(Long id, DlSqlSnippet snippet, String username) {
        DlSqlSnippet existing = snippetMapper.selectById(id);
        if (existing == null) return null;
        if (!username.equals(existing.getCreatedBy()) && !"ADMIN".equals(username)) {
            throw new SecurityException("无权修改他人片段");
        }
        existing.setTitle(snippet.getTitle());
        existing.setSqlText(snippet.getSqlText());
        existing.setDescription(snippet.getDescription());
        existing.setCategory(snippet.getCategory());
        existing.setVariables(snippet.getVariables());
        existing.setTags(snippet.getTags());
        existing.setIsPublic(snippet.getIsPublic());
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        snippetMapper.updateById(existing);
        return existing;
    }

    public boolean deleteSnippet(Long id, String username, String role) {
        DlSqlSnippet existing = snippetMapper.selectById(id);
        if (existing == null) return false;
        if (!username.equals(existing.getCreatedBy()) && !"ADMIN".equals(role)) {
            throw new SecurityException("无权删除他人片段");
        }
        snippetMapper.deleteById(id);
        return true;
    }

    public void incrementUsage(Long id) {
        DlSqlSnippet snippet = snippetMapper.selectById(id);
        if (snippet != null) {
            snippet.setUsageCount(snippet.getUsageCount() == null ? 1 : snippet.getUsageCount() + 1);
            snippetMapper.updateById(snippet);
        }
    }

    public Map<String, Object> renderSnippet(Long id, Map<String, String> params) {
        DlSqlSnippet snippet = snippetMapper.selectById(id);
        if (snippet == null) return null;

        String sql = snippet.getSqlText();
        Map<String, String> appliedParams = new LinkedHashMap<>();

        List<Map<String, Object>> varDefs = parseVariables(snippet.getVariables());
        for (Map<String, Object> varDef : varDefs) {
            String name = (String) varDef.get("name");
            String defaultValue = varDef.get("default") != null ? varDef.get("default").toString() : "";
            String value = (params != null && params.containsKey(name)) ? params.get(name) : defaultValue;
            appliedParams.put(name, value);
            if (value != null) {
                sql = sql.replace("${" + name + "}", value).replace("#{" + name + "}", value);
            }
        }

        incrementUsage(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", snippet.getId());
        result.put("title", snippet.getTitle());
        result.put("renderedSql", sql);
        result.put("variables", varDefs);
        result.put("appliedParams", appliedParams);
        return result;
    }

    public List<String> listCategories(String username) {
        QueryWrapper<DlSqlSnippet> qw = new QueryWrapper<>();
        qw.select("DISTINCT category");
        qw.and(w -> w.eq("created_by", username).or().eq("is_public", 1));
        qw.isNotNull("category").ne("category", "");
        List<DlSqlSnippet> snippets = snippetMapper.selectList(qw);
        List<String> categories = new ArrayList<>();
        for (DlSqlSnippet s : snippets) {
            if (s.getCategory() != null && !categories.contains(s.getCategory())) {
                categories.add(s.getCategory());
            }
        }
        return categories;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("解析变量定义失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}