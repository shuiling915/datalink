package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlLineage;
import com.datalink.model.R;
import com.datalink.service.LineageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
@Tag(name = "数据血缘")
public class LineageController {

    private final LineageService lineageService;

    @Operation(summary = "血缘关系列表")
    @GetMapping("/list")
    public R<List<DlLineage>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String transformType) {
        return R.ok(lineageService.list(keyword, transformType));
    }

    @Operation(summary = "血缘详情")
    @GetMapping("/{id}")
    public R<DlLineage> detail(@PathVariable Long id) {
        return R.ok(lineageService.getDetail(id));
    }

    @Operation(summary = "保存血缘关系")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('metadata:edit')")
    @AuditLog(module = "数据血缘", operation = "保存血缘")
    public R<DlLineage> save(@RequestBody DlLineage lineage,
                             @AuthenticationPrincipal UserDetails user) {
        return R.ok(lineageService.save(lineage, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "删除血缘关系")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('metadata:edit')")
    @AuditLog(module = "数据血缘", operation = "删除血缘")
    public R<String> delete(@PathVariable Long id) {
        lineageService.delete(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "血缘图(上下游追溯)")
    @GetMapping("/graph")
    public R<Map<String, Object>> graph(@RequestParam String db,
                                        @RequestParam String table,
                                        @RequestParam(defaultValue = "3") int depth) {
        return R.ok(lineageService.getLineageGraph(db, table, depth));
    }

    @Operation(summary = "直接上游")
    @GetMapping("/upstreams")
    public R<List<DlLineage>> upstreams(@RequestParam String db, @RequestParam String table) {
        return R.ok(lineageService.getUpstreams(db, table));
    }

    @Operation(summary = "直接下游")
    @GetMapping("/downstreams")
    public R<List<DlLineage>> downstreams(@RequestParam String db, @RequestParam String table) {
        return R.ok(lineageService.getDownstreams(db, table));
    }

    @Operation(summary = "血缘统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(lineageService.getStats());
    }
}