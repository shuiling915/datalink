package com.datalink.controller;

import com.datalink.dto.ScheduleConfigRequest;
import com.datalink.exception.ResourceNotFoundException;
import com.datalink.mapper.DlScriptMapper;
import com.datalink.mapper.DlSyncTaskMapper;
import com.datalink.model.DlScript;
import com.datalink.model.DlSyncTask;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调度配置 Controller — 脚本和同步任务的调度参数管理
 */
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@Tag(name = "调度配置", description = "脚本和同步任务的调度配置管理")
public class SchedulerConfigController {

    private final DlScriptMapper scriptMapper;
    private final DlSyncTaskMapper syncTaskMapper;

    /**
     * 获取脚本的调度配置
     */
    @GetMapping("/config/{scriptId}")
    @Operation(summary = "获取脚本调度配置")
    public R<Map<String, Object>> getConfig(@PathVariable Long scriptId) {
        DlScript script = requireScript(scriptId);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("scheduleCron", script.getScheduleCron());
        config.put("scheduleStatus", script.getScheduleStatus());
        config.put("timeoutSeconds", script.getTimeoutSeconds());
        config.put("retryTimes", script.getRetryTimes());
        config.put("retryInterval", script.getRetryInterval());
        config.put("warningType", script.getWarningType());
        config.put("dsWorkflowCode", script.getDsWorkflowCode());
        config.put("dsScheduleId", script.getDsScheduleId());
        return R.ok(config);
    }

    /**
     * 保存调度配置（仅保存到 DataLink，不推送 DS）
     */
    @PostMapping("/config/{scriptId}")
    @Operation(summary = "保存脚本调度配置")
    public R<Void> saveConfig(@PathVariable Long scriptId, @RequestBody ScheduleConfigRequest req) {
        requireScript(scriptId);
        DlScript update = new DlScript();
        update.setId(scriptId);
        update.setScheduleCron(req.getScheduleCron());
        update.setTimeoutSeconds(req.getTimeoutSeconds());
        update.setRetryTimes(req.getRetryTimes());
        update.setRetryInterval(req.getRetryInterval());
        update.setWarningType(req.getWarningType());
        scriptMapper.updateById(update);
        return R.ok();
    }

    /**
     * 获取同步任务的调度配置
     */
    @GetMapping("/sync-config/{taskId}")
    @Operation(summary = "获取同步任务调度配置")
    public R<Map<String, Object>> getSyncConfig(@PathVariable Long taskId) {
        DlSyncTask task = requireSyncTask(taskId);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("scheduleCron", task.getScheduleCron());
        config.put("scheduleStatus", task.getScheduleStatus());
        config.put("timeoutSeconds", task.getTimeoutSeconds());
        config.put("retryTimes", task.getRetryTimes());
        config.put("retryInterval", task.getRetryInterval());
        config.put("warningType", task.getWarningType());
        config.put("dsWorkflowCode", task.getDsWorkflowCode());
        config.put("dsScheduleId", task.getDsScheduleId());
        return R.ok(config);
    }

    /**
     * 保存同步任务调度配置
     */
    @PostMapping("/sync-config/{taskId}")
    @Operation(summary = "保存同步任务调度配置")
    public R<Void> saveSyncConfig(@PathVariable Long taskId, @RequestBody ScheduleConfigRequest req) {
        requireSyncTask(taskId);
        DlSyncTask update = new DlSyncTask();
        update.setId(taskId);
        update.setScheduleCron(req.getScheduleCron());
        update.setTimeoutSeconds(req.getTimeoutSeconds());
        update.setRetryTimes(req.getRetryTimes());
        update.setRetryInterval(req.getRetryInterval());
        update.setWarningType(req.getWarningType());
        syncTaskMapper.updateById(update);
        return R.ok();
    }

    // ========== 内部辅助方法 ==========

    private DlScript requireScript(Long scriptId) {
        DlScript script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new ResourceNotFoundException("脚本");
        }
        return script;
    }

    private DlSyncTask requireSyncTask(Long taskId) {
        DlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("同步任务");
        }
        return task;
    }
}
