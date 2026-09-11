package com.datanote.controller;

import com.datanote.mapper.DnSchedulerRunMapper;
import com.datanote.mapper.DnScriptMapper;
import com.datanote.mapper.DnSyncTaskMapper;
import com.datanote.model.DnSchedulerRun;
import com.datanote.model.DnScript;
import com.datanote.model.DnSyncTask;
import com.datanote.model.R;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "系统监控", description = "JVM/系统资源监控、任务执行统计")
public class SystemMonitorController {

    private final DnSchedulerRunMapper schedulerRunMapper;
    private final DnScriptMapper scriptMapper;
    private final DnSyncTaskMapper syncTaskMapper;

    @GetMapping("/overview")
    @Operation(summary = "监控概览")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jvm", jvmMetrics());
        data.put("system", systemMetrics());
        data.put("tasks", taskStats());
        return R.ok(data);
    }

    private Map<String, Object> jvmMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory();
        long totalMem = runtime.totalMemory();
        long freeMem = runtime.freeMemory();
        long usedMem = totalMem - freeMem;

        m.put("maxMemory", maxMem / 1024 / 1024);
        m.put("totalMemory", totalMem / 1024 / 1024);
        m.put("freeMemory", freeMem / 1024 / 1024);
        m.put("usedMemory", usedMem / 1024 / 1024);
        m.put("memoryUsagePercent", maxMem > 0 ? Math.round(usedMem * 10000.0 / maxMem) / 100.0 : 0);

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        m.put("heapUsed", heap.getUsed() / 1024 / 1024);
        m.put("heapMax", heap.getMax() / 1024 / 1024);
        m.put("heapUsagePercent", heap.getMax() > 0 ? Math.round(heap.getUsed() * 10000.0 / heap.getMax()) / 100.0 : 0);

        m.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        m.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        m.put("uptimeMinutes", ManagementFactory.getRuntimeMXBean().getUptime() / 60000);

        return m;
    }

    private Map<String, Object> systemMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        m.put("osName", osBean.getName());
        m.put("osArch", osBean.getArch());
        m.put("osVersion", osBean.getVersion());
        m.put("loadAverage", Math.round(osBean.getSystemLoadAverage() * 100.0) / 100.0);

        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            m.put("cpuUsagePercent", Math.round(sunOsBean.getCpuLoad() * 10000.0) / 100.0);
            m.put("totalPhysicalMemory", sunOsBean.getTotalPhysicalMemorySize() / 1024 / 1024);
            m.put("freePhysicalMemory", sunOsBean.getFreePhysicalMemorySize() / 1024 / 1024);
            long usedPhysical = (sunOsBean.getTotalPhysicalMemorySize() - sunOsBean.getFreePhysicalMemorySize()) / 1024 / 1024;
            m.put("usedPhysicalMemory", usedPhysical);
            m.put("memoryUsagePercent", sunOsBean.getTotalPhysicalMemorySize() > 0
                    ? Math.round((sunOsBean.getTotalPhysicalMemorySize() - sunOsBean.getFreePhysicalMemorySize()) * 10000.0 / sunOsBean.getTotalPhysicalMemorySize()) / 100.0 : 0);
        } else {
            m.put("cpuUsagePercent", -1);
            m.put("totalPhysicalMemory", 0);
            m.put("freePhysicalMemory", 0);
            m.put("usedPhysicalMemory", 0);
            m.put("memoryUsagePercent", 0);
        }

        // 磁盘
        File[] roots = File.listRoots();
        List<Map<String, Object>> disks = new ArrayList<>();
        if (roots != null) {
            for (File root : roots) {
                Map<String, Object> disk = new LinkedHashMap<>();
                disk.put("path", root.getPath());
                disk.put("total", root.getTotalSpace() / 1024 / 1024 / 1024);
                disk.put("free", root.getFreeSpace() / 1024 / 1024 / 1024);
                disk.put("used", (root.getTotalSpace() - root.getFreeSpace()) / 1024 / 1024 / 1024);
                disk.put("usagePercent", root.getTotalSpace() > 0
                        ? Math.round((root.getTotalSpace() - root.getFreeSpace()) * 10000.0 / root.getTotalSpace()) / 100.0 : 0);
                disks.add(disk);
            }
        }
        m.put("disks", disks);

        return m;
    }

    private Map<String, Object> taskStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        QueryWrapper<DnSchedulerRun> todayQw = new QueryWrapper<>();
        todayQw.eq("run_date", today).eq("run_type", "daily");
        List<DnSchedulerRun> todayRuns = schedulerRunMapper.selectList(todayQw);

        long total = todayRuns.size();
        long success = todayRuns.stream().filter(r -> r.getStatus() != null && r.getStatus() == DnSchedulerRun.STATUS_SUCCESS).count();
        long failed = todayRuns.stream().filter(r -> r.getStatus() != null && r.getStatus() == DnSchedulerRun.STATUS_FAILED).count();
        long running = todayRuns.stream().filter(r -> r.getStatus() != null && r.getStatus() == DnSchedulerRun.STATUS_RUNNING).count();
        long pending = total - success - failed - running;

        m.put("total", total);
        m.put("success", success);
        m.put("failed", failed);
        m.put("running", running);
        m.put("pending", pending);
        m.put("successRate", total > 0 ? Math.round(success * 10000.0 / total) / 100.0 : 0);

        long scriptCount = scriptMapper.selectCount(null);
        QueryWrapper<DnScript> onlineScriptQw = new QueryWrapper<>();
        onlineScriptQw.eq("schedule_status", "online");
        long onlineScriptCount = scriptMapper.selectCount(onlineScriptQw);

        long syncCount = syncTaskMapper.selectCount(null);
        QueryWrapper<DnSyncTask> onlineSyncQw = new QueryWrapper<>();
        onlineSyncQw.eq("schedule_status", "online");
        long onlineSyncCount = syncTaskMapper.selectCount(onlineSyncQw);

        m.put("totalScripts", scriptCount + syncCount);
        m.put("onlineScripts", onlineScriptCount + onlineSyncCount);

        return m;
    }
}