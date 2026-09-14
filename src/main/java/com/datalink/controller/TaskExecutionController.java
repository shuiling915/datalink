package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlTaskExecutionMapper;
import com.datalink.model.DlTaskExecution;
import com.datalink.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/task-execution")
@RequiredArgsConstructor
public class TaskExecutionController {

    private final DlTaskExecutionMapper taskExecutionMapper;

    @GetMapping("/list")
    public R<List<DlTaskExecution>> list(@RequestParam Long taskId,
                                          @RequestParam String taskType,
                                          @RequestParam(defaultValue = "20") int limit) {
        QueryWrapper<DlTaskExecution> qw = new QueryWrapper<>();
        if ("script".equals(taskType)) {
            qw.eq("script_id", taskId);
        } else {
            qw.eq("sync_task_id", taskId);
        }
        qw.orderByDesc("start_time").last("LIMIT " + limit);
        return R.ok(taskExecutionMapper.selectList(qw));
    }

    @GetMapping("/{id}")
    public R<DlTaskExecution> detail(@PathVariable Long id) {
        return R.ok(taskExecutionMapper.selectById(id));
    }

    /**
     * 手动任务统计：按任务+日期范围+触发类型查询
     */
    @GetMapping("/manual")
    public R<List<DlTaskExecution>> manual(@RequestParam(required = false) Long taskId,
                                            @RequestParam(required = false) String taskType,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        QueryWrapper<DlTaskExecution> qw = new QueryWrapper<>();
        qw.eq("trigger_type", "manual");
        if (taskId != null) {
            if ("script".equals(taskType)) qw.eq("script_id", taskId);
            else qw.eq("sync_task_id", taskId);
        }
        if (startDate != null) qw.ge("start_time", LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIN));
        if (endDate != null) qw.le("start_time", LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX));
        qw.orderByDesc("start_time").last("LIMIT 100");
        return R.ok(taskExecutionMapper.selectList(qw));
    }

    /**
     * 数据集成统计：查同步任务某天的执行情况
     */
    @GetMapping("/integration")
    public R<List<DlTaskExecution>> integration(@RequestParam(required = false) String date) {
        QueryWrapper<DlTaskExecution> qw = new QueryWrapper<>();
        qw.eq("task_type", "syncTask");
        if (date != null) {
            LocalDate d = LocalDate.parse(date);
            qw.ge("start_time", LocalDateTime.of(d, LocalTime.MIN));
            qw.le("start_time", LocalDateTime.of(d, LocalTime.MAX));
        }
        qw.orderByDesc("start_time").last("LIMIT 200");
        return R.ok(taskExecutionMapper.selectList(qw));
    }
}
