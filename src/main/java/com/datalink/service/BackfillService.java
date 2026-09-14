package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlBackfillInstanceMapper;
import com.datalink.mapper.DlBackfillTaskMapper;
import com.datalink.mapper.DlScriptMapper;
import com.datalink.model.DlBackfillInstance;
import com.datalink.model.DlBackfillTask;
import com.datalink.model.DlScript;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillService {

    private final DlBackfillTaskMapper backfillTaskMapper;
    private final DlBackfillInstanceMapper backfillInstanceMapper;
    private final DlScriptMapper scriptMapper;
    private final HiveService hiveService;

    // 运行中的补数据任务，用于停止/暂停控制
    private final ConcurrentHashMap<Long, String> taskControl = new ConcurrentHashMap<>();

    /**
     * 创建补数据任务
     */
    public DlBackfillTask createBackfill(Long taskId, String taskType, String taskName,
                                          LocalDate startDate, LocalDate endDate) {
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        DlBackfillTask task = new DlBackfillTask();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setTotalDays(totalDays);
        task.setCompleted(0);
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        backfillTaskMapper.insert(task);

        // 创建每天的实例
        for (int i = 0; i < totalDays; i++) {
            DlBackfillInstance inst = new DlBackfillInstance();
            inst.setBackfillId(task.getId());
            inst.setRunDate(startDate.plusDays(i));
            inst.setStatus("WAITING");
            backfillInstanceMapper.insert(inst);
        }

        taskControl.put(task.getId(), "RUNNING");
        return task;
    }

    /**
     * 异步执行补数据（在新线程中调用）
     */
    public void executeBackfill(Long backfillId) {
        DlBackfillTask task = backfillTaskMapper.selectById(backfillId);
        if (task == null) return;

        // 查脚本内容
        String sql = null;
        if ("script".equals(task.getTaskType())) {
            DlScript script = scriptMapper.selectById(task.getTaskId());
            if (script != null) sql = script.getContent();
        }

        QueryWrapper<DlBackfillInstance> qw = new QueryWrapper<>();
        qw.eq("backfill_id", backfillId).orderByAsc("run_date");
        List<DlBackfillInstance> instances = backfillInstanceMapper.selectList(qw);

        int completed = 0;
        for (DlBackfillInstance inst : instances) {
            // 检查控制信号
            String control = taskControl.getOrDefault(backfillId, "RUNNING");
            if ("STOPPED".equals(control)) {
                inst.setStatus("SKIPPED");
                backfillInstanceMapper.updateById(inst);
                continue;
            }
            if ("PAUSED".equals(control)) {
                // 暂停：后续实例保持 WAITING
                break;
            }
            if ("WAITING".equals(inst.getStatus()) || "FAILED".equals(inst.getStatus())) {
                inst.setStatus("RUNNING");
                inst.setStartTime(LocalDateTime.now());
                backfillInstanceMapper.updateById(inst);

                try {
                    if (sql != null) {
                        // 替换 bizdate
                        String replaced = sql.replaceAll("\\$\\{bizdate}", inst.getRunDate().toString());
                        hiveService.executeSQLWithStream(replaced, new HiveService.LogCallback() {
                            public void onLog(String level, String message) {}
                            public void onResult(java.util.Map<String, Object> result) {}
                            public void onError(String error) { throw new RuntimeException(error); }
                        });
                    }
                    inst.setStatus("SUCCESS");
                } catch (Exception e) {
                    inst.setStatus("FAILED");
                    inst.setLog(e.getMessage());
                    log.error("补数据失败 backfillId={} date={}", backfillId, inst.getRunDate(), e);
                }
                inst.setEndTime(LocalDateTime.now());
                inst.setDuration((int) ChronoUnit.SECONDS.between(inst.getStartTime(), inst.getEndTime()));
                backfillInstanceMapper.updateById(inst);

                completed++;
                task.setCompleted(completed);
                backfillTaskMapper.updateById(task);
            }
        }

        // 更新主任务状态
        String finalStatus = taskControl.getOrDefault(backfillId, "RUNNING");
        if ("STOPPED".equals(finalStatus)) {
            task.setStatus("STOPPED");
        } else if ("PAUSED".equals(finalStatus)) {
            task.setStatus("PAUSED");
        } else {
            task.setStatus("COMPLETED");
        }
        task.setFinishedAt(LocalDateTime.now());
        backfillTaskMapper.updateById(task);
        taskControl.remove(backfillId);
    }

    /**
     * 停止补数据任务
     */
    public void stopBackfill(Long backfillId) {
        taskControl.put(backfillId, "STOPPED");
        DlBackfillTask task = backfillTaskMapper.selectById(backfillId);
        if (task != null) {
            task.setStatus("STOPPED");
            backfillTaskMapper.updateById(task);
        }
    }

    /**
     * 暂停补数据任务
     */
    public void pauseBackfill(Long backfillId) {
        taskControl.put(backfillId, "PAUSED");
        DlBackfillTask task = backfillTaskMapper.selectById(backfillId);
        if (task != null) {
            task.setStatus("PAUSED");
            backfillTaskMapper.updateById(task);
        }
    }

    /**
     * 重启补数据任务（从暂停位置继续）
     */
    public void resumeBackfill(Long backfillId) {
        taskControl.put(backfillId, "RUNNING");
        DlBackfillTask task = backfillTaskMapper.selectById(backfillId);
        if (task != null) {
            task.setStatus("RUNNING");
            backfillTaskMapper.updateById(task);
            // 异步继续执行
            new Thread(() -> executeBackfill(backfillId)).start();
        }
    }

    /**
     * 查询补数据任务列表
     */
    public List<DlBackfillTask> listBackfills(Long taskId, String taskType) {
        QueryWrapper<DlBackfillTask> qw = new QueryWrapper<>();
        if (taskId != null) qw.eq("task_id", taskId);
        if (taskType != null) qw.eq("task_type", taskType);
        qw.orderByDesc("started_at");
        return backfillTaskMapper.selectList(qw);
    }

    /**
     * 查询补数据实例
     */
    public List<DlBackfillInstance> listInstances(Long backfillId) {
        QueryWrapper<DlBackfillInstance> qw = new QueryWrapper<>();
        qw.eq("backfill_id", backfillId).orderByAsc("run_date");
        return backfillInstanceMapper.selectList(qw);
    }
}
