package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlBaselineMapper;
import com.datalink.mapper.DlBaselineTaskMapper;
import com.datalink.model.DlBaseline;
import com.datalink.model.DlBaselineTask;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 基线管理 Controller
 */
@RestController
@RequestMapping("/api/baseline")
@RequiredArgsConstructor
@Tag(name = "基线管理", description = "基线CRUD与任务关联")
public class BaselineController {

    private final DlBaselineMapper baselineMapper;
    private final DlBaselineTaskMapper baselineTaskMapper;

    @GetMapping("/list")
    @Operation(summary = "基线列表")
    public R<List<Map<String, Object>>> list() {
        List<DlBaseline> baselines = baselineMapper.selectList(
                new QueryWrapper<DlBaseline>().orderByDesc("created_at"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlBaseline b : baselines) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("baselineName", b.getBaselineName());
            m.put("description", b.getDescription());
            m.put("commitTime", b.getCommitTime() != null ? b.getCommitTime().toString() : null);
            m.put("priority", b.getPriority());
            m.put("status", b.getStatus());
            m.put("createdBy", b.getCreatedBy());
            m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);

            // 关联任务数
            QueryWrapper<DlBaselineTask> tqw = new QueryWrapper<>();
            tqw.eq("baseline_id", b.getId());
            m.put("taskCount", baselineTaskMapper.selectCount(tqw));

            result.add(m);
        }
        return R.ok(result);
    }

    @PostMapping("/create")
    @Operation(summary = "创建基线")
    public R<DlBaseline> create(@RequestBody Map<String, Object> body) {
        DlBaseline baseline = new DlBaseline();
        baseline.setBaselineName((String) body.get("baselineName"));
        baseline.setDescription((String) body.get("description"));
        if (body.get("commitTime") != null) {
            baseline.setCommitTime(java.time.LocalTime.parse((String) body.get("commitTime")));
        }
        baseline.setPriority(body.get("priority") != null ? ((Number) body.get("priority")).intValue() : 1);
        baseline.setStatus(DlBaseline.STATUS_ENABLED);
        baseline.setCreatedBy("default");
        baseline.setCreatedAt(LocalDateTime.now());
        baseline.setUpdatedAt(LocalDateTime.now());
        baselineMapper.insert(baseline);

        // 关联任务
        if (body.get("taskIds") instanceof List) {
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) body.get("taskIds");
            for (Map<String, Object> t : tasks) {
                DlBaselineTask bt = new DlBaselineTask();
                bt.setBaselineId(baseline.getId());
                bt.setTaskId(((Number) t.get("taskId")).longValue());
                bt.setTaskType((String) t.get("taskType"));
                bt.setTaskName((String) t.get("taskName"));
                bt.setCreatedAt(LocalDateTime.now());
                baselineTaskMapper.insert(bt);
            }
        }

        return R.ok(baseline);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除基线")
    public R<Void> delete(@PathVariable Long id) {
        baselineMapper.deleteById(id);
        baselineTaskMapper.delete(new QueryWrapper<DlBaselineTask>().eq("baseline_id", id));
        return R.ok();
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用基线")
    public R<Void> toggle(@PathVariable Long id) {
        DlBaseline baseline = baselineMapper.selectById(id);
        if (baseline == null) return R.fail("基线不存在");
        DlBaseline update = new DlBaseline();
        update.setId(id);
        update.setStatus(DlBaseline.STATUS_ENABLED.equals(baseline.getStatus())
                ? DlBaseline.STATUS_DISABLED : DlBaseline.STATUS_ENABLED);
        update.setUpdatedAt(LocalDateTime.now());
        baselineMapper.updateById(update);
        return R.ok();
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "获取基线关联任务")
    public R<List<DlBaselineTask>> tasks(@PathVariable Long id) {
        List<DlBaselineTask> tasks = baselineTaskMapper.selectList(
                new QueryWrapper<DlBaselineTask>().eq("baseline_id", id));
        return R.ok(tasks);
    }
}
