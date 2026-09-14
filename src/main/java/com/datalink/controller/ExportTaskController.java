package com.datalink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.model.DlExportTask;
import com.datalink.model.R;
import com.datalink.service.ExportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/export-task")
@RequiredArgsConstructor
@Tag(name = "数据导出任务", description = "异步导出大查询结果、任务状态查询、文件下载")
public class ExportTaskController {

    private final ExportTaskService exportTaskService;

    @Operation(summary = "创建导出任务")
    @PostMapping
    public R<DlExportTask> createTask(@RequestBody java.util.Map<String, Object> body,
                                       @AuthenticationPrincipal UserDetails user) {
        String operator = user != null ? user.getUsername() : "anonymous";
        String taskName = (String) body.get("taskName");
        Long datasourceId = Long.valueOf(body.get("datasourceId").toString());
        String databaseName = (String) body.get("databaseName");
        String sql = (String) body.get("sql");
        String fileName = (String) body.get("fileName");

        if (sql == null || sql.trim().isEmpty()) return R.fail("SQL不能为空");
        return R.ok(exportTaskService.createTask(taskName, datasourceId, databaseName, sql, fileName, operator));
    }

    @Operation(summary = "查询导出任务列表")
    @GetMapping("/list")
    public R<Page<DlExportTask>> listTasks(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String status,
                                            @AuthenticationPrincipal UserDetails user) {
        String operator = user != null ? user.getUsername() : null;
        return R.ok(exportTaskService.listTasks(page, size, status, operator));
    }

    @Operation(summary = "查询导出任务详情")
    @GetMapping("/{id}")
    public R<DlExportTask> getTask(@PathVariable Long id) {
        DlExportTask task = exportTaskService.getTask(id);
        if (task == null) return R.fail("任务不存在");
        return R.ok(task);
    }

    @Operation(summary = "下载导出文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        try {
            DlExportTask task = exportTaskService.getTask(id);
            if (task == null) return ResponseEntity.notFound().build();

            byte[] data = exportTaskService.downloadFile(id);
            String encodedName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .contentLength(data.length)
                    .body(data);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "删除导出任务")
    @DeleteMapping("/{id}")
    public R<Void> deleteTask(@PathVariable Long id) {
        return exportTaskService.deleteTask(id) ? R.ok() : R.fail("任务不存在");
    }
}