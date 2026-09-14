package com.datalink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.model.DlSqlSnippet;
import com.datalink.model.R;
import com.datalink.service.SqlSnippetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql-snippet")
@RequiredArgsConstructor
@Tag(name = "SQL片段库", description = "SQL模板、片段的增删改查、变量渲染")
public class SqlSnippetController {

    private final SqlSnippetService sqlSnippetService;

    @Operation(summary = "查询片段列表")
    @GetMapping("/list")
    public R<Page<DlSqlSnippet>> listSnippets(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String keyword,
                                               @AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : "anonymous";
        return R.ok(sqlSnippetService.listSnippets(page, size, category, keyword, username, true));
    }

    @Operation(summary = "获取片段详情")
    @GetMapping("/{id}")
    public R<DlSqlSnippet> getSnippet(@PathVariable Long id) {
        DlSqlSnippet snippet = sqlSnippetService.getSnippet(id);
        return snippet != null ? R.ok(snippet) : R.fail("片段不存在");
    }

    @Operation(summary = "创建片段")
    @PostMapping
    public R<DlSqlSnippet> createSnippet(@RequestBody DlSqlSnippet snippet,
                                          @AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : "anonymous";
        return R.ok(sqlSnippetService.createSnippet(snippet, username));
    }

    @Operation(summary = "更新片段")
    @PutMapping("/{id}")
    public R<DlSqlSnippet> updateSnippet(@PathVariable Long id, @RequestBody DlSqlSnippet snippet,
                                          @AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : "anonymous";
        try {
            DlSqlSnippet updated = sqlSnippetService.updateSnippet(id, snippet, username);
            return updated != null ? R.ok(updated) : R.fail("片段不存在");
        } catch (SecurityException e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "删除片段")
    @DeleteMapping("/{id}")
    public R<Void> deleteSnippet(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        try {
            String username = user != null ? user.getUsername() : "anonymous";
            String role = "USER";
            boolean result = sqlSnippetService.deleteSnippet(id, username, role);
            return result ? R.ok() : R.fail("片段不存在");
        } catch (SecurityException e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "渲染片段（变量替换）")
    @PostMapping("/{id}/render")
    public R<Map<String, Object>> renderSnippet(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, String> params) {
        Map<String, Object> result = sqlSnippetService.renderSnippet(id, params);
        return result != null ? R.ok(result) : R.fail("片段不存在");
    }

    @Operation(summary = "获取片段分类列表")
    @GetMapping("/categories")
    public R<List<String>> listCategories(@AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : "anonymous";
        return R.ok(sqlSnippetService.listCategories(username));
    }
}