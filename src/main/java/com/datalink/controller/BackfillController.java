package com.datalink.controller;

import com.datalink.model.DlBackfillInstance;
import com.datalink.model.DlBackfillTask;
import com.datalink.model.R;
import com.datalink.service.BackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/backfill")
@RequiredArgsConstructor
public class BackfillController {

    private final BackfillService backfillService;

    /**
     * 预览补数据日期
     */
    @PostMapping("/preview")
    public R<Map<String, Object>> preview(@RequestBody Map<String, String> body) {
        LocalDate start = LocalDate.parse(body.get("startDate"));
        LocalDate end = LocalDate.parse(body.get("endDate"));
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(start.plusDays(i).toString());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("totalDays", days);
        return R.ok(result);
    }

    /**
     * 开始补数据
     */
    @PostMapping("/start")
    public R<DlBackfillTask> start(@RequestBody Map<String, String> body) {
        Long taskId = Long.valueOf(body.get("taskId"));
        String taskType = body.get("taskType");
        String taskName = body.get("taskName");
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        LocalDate endDate = LocalDate.parse(body.get("endDate"));

        DlBackfillTask task = backfillService.createBackfill(taskId, taskType, taskName, startDate, endDate);

        // 异步执行
        new Thread(() -> backfillService.executeBackfill(task.getId())).start();

        return R.ok(task);
    }

    /**
     * 停止
     */
    @PostMapping("/stop/{id}")
    public R<Void> stop(@PathVariable Long id) {
        backfillService.stopBackfill(id);
        return R.ok();
    }

    /**
     * 暂停
     */
    @PostMapping("/pause/{id}")
    public R<Void> pause(@PathVariable Long id) {
        backfillService.pauseBackfill(id);
        return R.ok();
    }

    /**
     * 重启
     */
    @PostMapping("/resume/{id}")
    public R<Void> resume(@PathVariable Long id) {
        backfillService.resumeBackfill(id);
        return R.ok();
    }

    /**
     * 任务列表
     */
    @GetMapping("/list")
    public R<List<DlBackfillTask>> list(@RequestParam(required = false) Long taskId,
                                         @RequestParam(required = false) String taskType) {
        return R.ok(backfillService.listBackfills(taskId, taskType));
    }

    /**
     * 实例列表
     */
    @GetMapping("/instances/{backfillId}")
    public R<List<DlBackfillInstance>> instances(@PathVariable Long backfillId) {
        return R.ok(backfillService.listInstances(backfillId));
    }
}
