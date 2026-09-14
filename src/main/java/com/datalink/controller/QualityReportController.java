package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.QualityReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据质量报告 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/quality-report")
@RequiredArgsConstructor
@Tag(name = "数据质量报告", description = "质量评分、规则统计、问题分布")
public class QualityReportController {

    private final QualityReportService qualityReportService;

    @Operation(summary = "数据质量评分")
    @GetMapping("/score")
    public R<Map<String, Object>> score(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        double score = qualityReportService.calculateScore(days);
        result.put("score", Math.round(score * 100) / 100.0);
        result.put("periodDays", days);
        return R.ok(result);
    }

    @Operation(summary = "规则质量统计")
    @GetMapping("/rule-stats")
    public R<List<Map<String, Object>>> ruleStats(@RequestParam(defaultValue = "7") int days) {
        return R.ok(qualityReportService.getRuleQualityStats(days));
    }

    @Operation(summary = "表质量统计")
    @GetMapping("/table-stats")
    public R<List<Map<String, Object>>> tableStats(@RequestParam(defaultValue = "7") int days) {
        return R.ok(qualityReportService.getTableQualityStats(days));
    }

    @Operation(summary = "问题分布")
    @GetMapping("/issue-distribution")
    public R<Map<String, Long>> issueDistribution(@RequestParam(defaultValue = "7") int days) {
        return R.ok(qualityReportService.getIssueDistribution(days));
    }

    @Operation(summary = "完整质量报告")
    @GetMapping("/full")
    public R<Map<String, Object>> fullReport(@RequestParam(defaultValue = "7") int days) {
        return R.ok(qualityReportService.generateReport(days));
    }
}