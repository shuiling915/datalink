package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlSubjectMapper;
import com.datalink.model.DlSubject;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 主题域管理 Controller
 */
@RestController
@RequestMapping("/api/subject")
@RequiredArgsConstructor
@Tag(name = "主题域管理", description = "主题域的增删改查与树形结构")
public class SubjectController {

    private final DlSubjectMapper subjectMapper;

    /**
     * 获取所有主题域（平铺列表）
     */
    @Operation(summary = "获取主题域列表")
    @GetMapping("/list")
    public R<List<DlSubject>> list() {
        QueryWrapper<DlSubject> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order", "id");
        return R.ok(subjectMapper.selectList(qw));
    }

    /**
     * 获取主题域树形结构
     */
    @Operation(summary = "获取主题域树")
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        QueryWrapper<DlSubject> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order", "id");
        List<DlSubject> subjects = subjectMapper.selectList(qw);
        return R.ok(buildTree(subjects, null));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<DlSubject> subjects, Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (DlSubject s : subjects) {
            boolean match = (parentId == null && s.getParentId() == null)
                    || (parentId != null && parentId.equals(s.getParentId()));
            if (match) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", s.getId());
                node.put("name", s.getName());
                node.put("layer", s.getLayer());
                node.put("sortOrder", s.getSortOrder());
                node.put("children", buildTree(subjects, s.getId()));
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * 创建主题域
     */
    @Operation(summary = "创建主题域")
    @PostMapping
    public R<DlSubject> create(@RequestBody DlSubject subject) {
        subject.setCreatedAt(LocalDateTime.now());
        subjectMapper.insert(subject);
        return R.ok(subject);
    }

    /**
     * 更新主题域
     */
    @Operation(summary = "更新主题域")
    @PutMapping("/{id}")
    public R<DlSubject> update(@PathVariable Long id, @RequestBody DlSubject subject) {
        subject.setId(id);
        subjectMapper.updateById(subject);
        return R.ok(subject);
    }

    /**
     * 删除主题域（级联删除其下二级主题）
     */
    @Operation(summary = "删除主题域")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        // 先删子节点，避免产生孤儿二级主题
        QueryWrapper<DlSubject> childQuery = new QueryWrapper<>();
        childQuery.eq("parent_id", id);
        subjectMapper.delete(childQuery);
        subjectMapper.deleteById(id);
        return R.ok("删除成功");
    }
}
