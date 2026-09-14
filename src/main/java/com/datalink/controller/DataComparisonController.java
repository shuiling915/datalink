package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.R;
import com.datalink.service.DataComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据一致性校验 Controller — 源端 vs 目标端对账
 */
@Tag(name = "数据一致性校验")
@RestController
@RequestMapping("/api/data-compare")
@RequiredArgsConstructor
public class DataComparisonController {

    private final DataComparisonService dataComparisonService;

    @Operation(summary = "执行数据一致性校验")
    @PostMapping("/run")
    @PreAuthorize("hasAuthority('quality:execute')")
    @AuditLog(module = "数据质量", operation = "数据一致性校验")
    public R<Map<String, Object>> compare(@RequestBody Map<String, Object> body) {
        Long sourceDsId = Long.valueOf(body.get("sourceDsId").toString());
        String sourceTable = (String) body.get("sourceTable");
        Long targetDsId = Long.valueOf(body.get("targetDsId").toString());
        String targetTable = (String) body.get("targetTable");
        String primaryKey = (String) body.get("primaryKey");
        String compareMode = (String) body.get("compareMode");

        return R.ok(dataComparisonService.compare(
                sourceDsId, sourceTable, targetDsId, targetTable, primaryKey, compareMode));
    }

    @Operation(summary = "对比模式列表")
    @GetMapping("/modes")
    public R<Map<String, String>> modes() {
        return R.ok(Map.of(
                "row_count", "行数对比",
                "primary_key", "主键集合对比",
                "sample", "抽样数据对比",
                "full", "完整对比(行数+主键)"
        ));
    }
}