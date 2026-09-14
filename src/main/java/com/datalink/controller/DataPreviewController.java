package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.DataPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-preview")
@RequiredArgsConstructor
@Tag(name = "数据预览", description = "表数据采样预览、表结构统计")
public class DataPreviewController {

    private final DataPreviewService dataPreviewService;

    @Operation(summary = "预览表数据")
    @GetMapping("/table")
    @PreAuthorize("hasAnyAuthority('metadata:view','data:query','admin:view')")
    public R<Map<String, Object>> previewTable(
            @RequestParam Long datasourceId,
            @RequestParam String database,
            @RequestParam String table,
            @RequestParam(required = false) List<String> columns,
            @RequestParam(required = false) String where,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            int safeLimit = Math.min(limit, 500);
            return R.ok(dataPreviewService.previewTable(datasourceId, database, table,
                    columns, where, safeLimit, offset));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("预览失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取表结构统计")
    @GetMapping("/table/stats")
    @PreAuthorize("hasAnyAuthority('metadata:view','data:query','admin:view')")
    public R<Map<String, Object>> getTableStats(
            @RequestParam Long datasourceId,
            @RequestParam String database,
            @RequestParam String table) {
        try {
            return R.ok(dataPreviewService.getTableStats(datasourceId, database, table));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("获取统计失败: " + e.getMessage());
        }
    }
}