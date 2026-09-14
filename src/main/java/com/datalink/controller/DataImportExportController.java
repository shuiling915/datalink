package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlDataImport;
import com.datalink.model.R;
import com.datalink.service.DataImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据导入导出 Controller — CSV 文件导入、SQL 结果导出
 */
@Slf4j
@Tag(name = "数据导入导出")
@RestController
@RequestMapping("/api/data-io")
@RequiredArgsConstructor
public class DataImportExportController {

    private final DataImportExportService dataImportExportService;

    @Operation(summary = "导入CSV文件(自动建表+导入)")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('data:write')")
    @AuditLog(module = "数据导入", operation = "CSV文件导入")
    public R<DlDataImport> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("datasourceId") Long datasourceId,
            @RequestParam("tableName") String tableName,
            @RequestParam(defaultValue = "true") boolean createTable,
            @RequestParam(defaultValue = "false") boolean truncate,
            @AuthenticationPrincipal UserDetails user) {
        try {
            DlDataImport result = dataImportExportService.importCsv(
                    file, datasourceId, tableName, createTable, truncate,
                    user != null ? user.getUsername() : "anonymous");
            return R.ok(result);
        } catch (Exception e) {
            log.error("CSV导入失败", e);
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "导出SQL查询结果为CSV")
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam Long datasourceId,
            @RequestParam String sql,
            @RequestParam(defaultValue = "export") String fileName) {
        try {
            String csv = dataImportExportService.exportToCsv(datasourceId, sql);
            byte[] bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);

            String encodedName = URLEncoder.encode(fileName + ".csv", StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", encodedName);
            headers.setContentLength(bytes.length);

            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            log.error("CSV导出失败", e);
            return ResponseEntity.badRequest().body(("导出失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Operation(summary = "导入历史记录")
    @GetMapping("/history")
    public R<List<DlDataImport>> history(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "50") int limit) {
        return R.ok(dataImportExportService.listHistory(
                user != null ? user.getUsername() : null, limit));
    }
}