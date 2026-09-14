package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlProjectMapper;
import com.datalink.model.DlProject;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/project")
@Tag(name = "项目管理", description = "项目空间、成员、发布管理")
@RequiredArgsConstructor
public class ProjectController {

    private final DlProjectMapper projectMapper;

    @Operation(summary = "项目列表")
    @GetMapping("/list")
    public R<List<DlProject>> list() {
        return R.ok(projectMapper.selectList(new QueryWrapper<DlProject>().orderByDesc("created_at")));
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public R<DlProject> detail(@PathVariable Long id) {
        return R.ok(projectMapper.selectById(id));
    }

    @Operation(summary = "创建项目")
    @PostMapping("/create")
    public R<DlProject> create(@RequestBody DlProject project) {
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        if (project.getStatus() == null) project.setStatus("active");
        projectMapper.insert(project);
        return R.ok(project);
    }

    @Operation(summary = "更新项目")
    @PostMapping("/update")
    public R<String> update(@RequestBody DlProject project) {
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return R.ok("更新成功");
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        projectMapper.deleteById(id);
        return R.ok("删除成功");
    }
}