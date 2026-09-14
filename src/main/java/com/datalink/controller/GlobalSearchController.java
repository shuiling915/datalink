package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "全局搜索", description = "跨资产、表、字段、数据源、脚本的统一搜索")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @Operation(summary = "全局搜索")
    @GetMapping
    public R<Map<String, Object>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "20") int limit) {
        return R.ok(globalSearchService.search(keyword, limit));
    }

    @Operation(summary = "搜索建议")
    @GetMapping("/suggestions")
    public R<List<Map<String, Object>>> suggestions(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "10") int limit) {
        return R.ok(globalSearchService.searchSuggestions(keyword, limit));
    }
}