package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlDataApi;
import com.datalink.model.DlDataApiLog;
import com.datalink.model.R;
import com.datalink.service.DataApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据服务 API 管理 Controller — API 的创建、发布、下线、日志
 */
@Tag(name = "数据服务API管理")
@RestController
@RequestMapping("/api/data-api")
@RequiredArgsConstructor
public class DataApiController {

    private final DataApiService dataApiService;

    @Operation(summary = "API列表")
    @GetMapping("/list")
    public R<List<DlDataApi>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status) {
        return R.ok(dataApiService.list(keyword, status));
    }

    @Operation(summary = "API详情")
    @GetMapping("/{id}")
    public R<DlDataApi> getById(@PathVariable Long id) {
        return R.ok(dataApiService.getById(id));
    }

    @Operation(summary = "保存API")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据服务", operation = "保存数据API")
    public R<DlDataApi> save(@RequestBody DlDataApi api,
                             @AuthenticationPrincipal UserDetails user) {
        return R.ok(dataApiService.save(api, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "发布API")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据服务", operation = "发布数据API")
    public R<DlDataApi> publish(@PathVariable Long id) {
        return R.ok(dataApiService.publish(id));
    }

    @Operation(summary = "下线API")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据服务", operation = "下线数据API")
    public R<DlDataApi> offline(@PathVariable Long id) {
        return R.ok(dataApiService.offline(id));
    }

    @Operation(summary = "删除API")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据服务", operation = "删除数据API")
    public R<String> delete(@PathVariable Long id) {
        dataApiService.delete(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "生成API Key")
    @PostMapping("/generate-key")
    public R<Map<String, String>> generateKey() {
        return R.ok(Map.of("apiKey", dataApiService.generateApiKey()));
    }

    @Operation(summary = "API调用日志")
    @GetMapping("/logs")
    public R<List<DlDataApiLog>> logs(@RequestParam(required = false) Long apiId,
                                      @RequestParam(required = false) String apiCode,
                                      @RequestParam(defaultValue = "100") int limit) {
        return R.ok(dataApiService.listLogs(apiId, apiCode, limit));
    }

    @Operation(summary = "API调用统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) Long apiId,
                                        @RequestParam(required = false) String apiCode,
                                        @RequestParam(defaultValue = "7") int days) {
        return R.ok(dataApiService.getStats(apiId, apiCode, days));
    }
}