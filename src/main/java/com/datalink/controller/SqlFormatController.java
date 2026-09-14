package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.SqlFormatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sql-format")
@RequiredArgsConstructor
@Tag(name = "SQL格式化", description = "SQL格式化、压缩、语法分析")
public class SqlFormatController {

    private final SqlFormatService sqlFormatService;

    @Operation(summary = "格式化SQL")
    @PostMapping("/format")
    public R<String> format(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        return R.ok(sqlFormatService.format(sql));
    }

    @Operation(summary = "压缩SQL")
    @PostMapping("/compact")
    public R<String> compact(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        return R.ok(sqlFormatService.compact(sql));
    }

    @Operation(summary = "分析SQL")
    @PostMapping("/analyze")
    public R<Map<String, Object>> analyze(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        return R.ok(sqlFormatService.analyze(sql));
    }
}