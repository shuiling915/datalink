package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.DatasourceHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource-health")
@RequiredArgsConstructor
@Tag(name = "数据源健康检查", description = "数据源在线状态、响应时间、数据库信息")
public class DatasourceHealthController {

    private final DatasourceHealthService healthService;

    @Operation(summary = "检查单个数据源健康状态")
    @GetMapping("/{id}")
    public R<Map<String, Object>> checkOne(@PathVariable Long id) {
        Map<String, Object> health = healthService.checkDatasourceById(id);
        return health != null ? R.ok(health) : R.fail("数据源不存在");
    }

    @Operation(summary = "检查所有数据源健康状态")
    @GetMapping("/all")
    public R<List<Map<String, Object>>> checkAll() {
        return R.ok(healthService.checkAll());
    }

    @Operation(summary = "获取缓存的健康状态汇总")
    @GetMapping("/summary")
    public R<Map<String, Object>> getSummary() {
        return R.ok(healthService.getHealthSummary());
    }

    @Operation(summary = "获取缓存的所有数据源健康状态")
    @GetMapping("/cached")
    public R<List<Map<String, Object>>> getCached() {
        return R.ok(healthService.getAllCachedHealth());
    }
}