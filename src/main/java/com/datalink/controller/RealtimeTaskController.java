package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlRealtimeTaskMapper;
import com.datalink.model.DlRealtimeTask;
import com.datalink.model.R;
import com.datalink.service.FlinkService;
import com.datalink.service.HiveService;
import com.datalink.service.LogBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@Tag(name = "实时开发", description = "Flink SQL 实时任务管理")
public class RealtimeTaskController {

    private final DlRealtimeTaskMapper realtimeTaskMapper;
    private final HiveService hiveService;
    private final FlinkService flinkService;
    private final LogBroadcastService logBroadcastService;

    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public R<List<Map<String, Object>>> list() {
        QueryWrapper<DlRealtimeTask> qw = new QueryWrapper<>();
        qw.orderByDesc("updated_at");
        List<DlRealtimeTask> tasks = realtimeTaskMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlRealtimeTask t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("taskName", t.getTaskName());
            m.put("taskType", t.getTaskType());
            m.put("status", t.getStatus() != null ? t.getStatus() : "draft");
            m.put("updatedAt", t.getUpdatedAt());
            m.put("folderId", t.getFolderId());
            result.add(m);
        }
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public R<DlRealtimeTask> detail(@PathVariable Long id) {
        return R.ok(realtimeTaskMapper.selectById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "保存任务")
    public R<Map<String, Object>> save(@RequestBody DlRealtimeTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        task.setStatus(task.getStatus() != null ? task.getStatus() : "draft");
        if (task.getId() != null) {
            realtimeTaskMapper.updateById(task);
        } else {
            task.setCreatedAt(LocalDateTime.now());
            realtimeTaskMapper.insert(task);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        return R.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public R<Void> delete(@PathVariable Long id) {
        realtimeTaskMapper.deleteById(id);
        return R.ok();
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "执行任务（Flink SQL）")
    public R<Map<String, Object>> run(@PathVariable Long id) {
        DlRealtimeTask task = realtimeTaskMapper.selectById(id);
        if (task == null) return R.fail("任务不存在");
        if (task.getContent() == null || task.getContent().trim().isEmpty()) {
            return R.fail("任务内容为空");
        }

        try {
            Map<String, Object> execResult = flinkService.executeSQL(task.getContent());
            DlRealtimeTask update = new DlRealtimeTask();
            update.setId(id);
            update.setStatus(Boolean.TRUE.equals(execResult.get("success")) ? "success" : "failed");
            if (execResult.get("error") != null) {
                update.setLastError(String.valueOf(execResult.get("error")));
            }
            update.setUpdatedAt(LocalDateTime.now());
            realtimeTaskMapper.updateById(update);
            return R.ok(execResult);
        } catch (Exception e) {
            log.error("Flink SQL 执行失败: {}", e.getMessage());
            DlRealtimeTask update = new DlRealtimeTask();
            update.setId(id);
            update.setStatus("failed");
            update.setLastError(e.getMessage());
            update.setUpdatedAt(LocalDateTime.now());
            realtimeTaskMapper.updateById(update);
            return R.fail("执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/run-stream")
    @Operation(summary = "流式执行任务（Flink + WebSocket实时推送日志）")
    public R<Map<String, Object>> runStream(@PathVariable Long id) {
        DlRealtimeTask task = realtimeTaskMapper.selectById(id);
        if (task == null) return R.fail("任务不存在");
        if (task.getContent() == null || task.getContent().trim().isEmpty()) {
            return R.fail("任务内容为空");
        }

        String taskName = task.getTaskName();

        flinkService.executeSQLAsync(task.getContent(), String.valueOf(id), new FlinkService.LogCallback() {
            @Override
            public void onLog(String level, String message) {
                logBroadcastService.broadcast("realtime-log", level, id, taskName, message);
            }

            @Override
            public void onResult(Map<String, Object> result) {
                logBroadcastService.broadcast("realtime-result", "OK", id, taskName,
                        "执行完成，耗时 " + (result.get("duration") != null ? result.get("duration") + "ms" : "N/A"));
                DlRealtimeTask update = new DlRealtimeTask();
                update.setId(id);
                update.setStatus("stopped");
                update.setLastStopTime(LocalDateTime.now());
                update.setUpdatedAt(LocalDateTime.now());
                realtimeTaskMapper.updateById(update);
            }

            @Override
            public void onError(String error) {
                logBroadcastService.broadcast("realtime-log", "ERROR", id, taskName, error);
                DlRealtimeTask update = new DlRealtimeTask();
                update.setId(id);
                update.setStatus("failed");
                update.setLastError(error);
                update.setUpdatedAt(LocalDateTime.now());
                realtimeTaskMapper.updateById(update);
            }
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "任务已提交，实时日志将通过WebSocket推送");
        result.put("taskId", id);
        return R.ok(result);
    }

    @PostMapping("/{id}/explain")
    @Operation(summary = "语法校验 & 执行计划（Flink / Hive）")
    public R<Map<String, Object>> explain(@PathVariable Long id) {
        DlRealtimeTask task = realtimeTaskMapper.selectById(id);
        if (task == null) return R.fail("任务不存在");
        if (task.getContent() == null || task.getContent().trim().isEmpty()) {
            return R.fail("任务内容为空");
        }

        String sql = task.getContent().trim();
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String plan = flinkService.explainSQL(sql);
            result.put("valid", true);
            result.put("message", "Flink 语法校验通过");
            result.put("plan", plan);
            result.put("engine", "flink");
            return R.ok(result);
        } catch (Exception e) {
            log.warn("Flink 校验失败，尝试 Hive: {}", e.getMessage());
            try {
                String explainSql = "EXPLAIN " + sql;
                Map<String, Object> hiveResult = hiveService.executeSQL(explainSql);
                result.put("valid", true);
                result.put("message", "Hive 语法校验通过（Flink 不支持该语句）");
                result.put("plan", hiveResult);
                result.put("engine", "hive");
                return R.ok(result);
            } catch (Exception e2) {
                result.put("valid", false);
                result.put("message", "语法校验失败: " + e2.getMessage());
                result.put("error", e2.getMessage());
                return R.ok(result);
            }
        }
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动实时任务")
    public R<Void> start(@PathVariable Long id) {
        DlRealtimeTask task = new DlRealtimeTask();
        task.setId(id);
        task.setStatus("running");
        task.setLastStartTime(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        realtimeTaskMapper.updateById(task);
        return R.ok();
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "停止实时任务")
    public R<Void> stop(@PathVariable Long id) {
        DlRealtimeTask task = new DlRealtimeTask();
        task.setId(id);
        task.setStatus("stopped");
        task.setLastStopTime(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        realtimeTaskMapper.updateById(task);
        return R.ok();
    }
}