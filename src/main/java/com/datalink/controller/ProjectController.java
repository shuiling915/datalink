package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlProjectMapper;
import com.datalink.model.DlProject;
import com.datalink.model.R;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/project")
@Tag(name = "项目管理", description = "项目空间、成员、发布管理")
@RequiredArgsConstructor
public class ProjectController {

    private final DlProjectMapper projectMapper;
    private final ObjectMapper objectMapper;

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

    // ========== 成员管理 ==========

    @Operation(summary = "获取项目成员列表")
    @GetMapping("/{id}/members")
    public R<List<Map<String, String>>> listMembers(@PathVariable Long id) {
        DlProject project = projectMapper.selectById(id);
        if (project == null) return R.fail("项目不存在");
        return R.ok(parseMembers(project.getMembers()));
    }

    @Operation(summary = "添加项目成员")
    @PostMapping("/{id}/members")
    public R<String> addMember(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String role = body.getOrDefault("role", "member");
        if (username == null || username.trim().isEmpty()) return R.fail("用户名不能为空");

        DlProject project = projectMapper.selectById(id);
        if (project == null) return R.fail("项目不存在");

        List<Map<String, String>> members = parseMembers(project.getMembers());
        for (Map<String, String> m : members) {
            if (username.equals(m.get("username"))) {
                m.put("role", role);
                saveMembers(project, members);
                return R.ok("成员角色已更新");
            }
        }
        Map<String, String> newMember = new LinkedHashMap<>();
        newMember.put("username", username);
        newMember.put("role", role);
        members.add(newMember);
        saveMembers(project, members);
        return R.ok("成员添加成功");
    }

    @Operation(summary = "移除项目成员")
    @DeleteMapping("/{id}/members/{username}")
    public R<String> removeMember(@PathVariable Long id, @PathVariable String username) {
        DlProject project = projectMapper.selectById(id);
        if (project == null) return R.fail("项目不存在");

        List<Map<String, String>> members = parseMembers(project.getMembers());
        members.removeIf(m -> username.equals(m.get("username")));
        saveMembers(project, members);
        return R.ok("成员已移除");
    }

    private List<Map<String, String>> parseMembers(String membersJson) {
        if (membersJson == null || membersJson.trim().isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(membersJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveMembers(DlProject project, List<Map<String, String>> members) {
        try {
            project.setMembers(objectMapper.writeValueAsString(members));
        } catch (Exception e) {
            project.setMembers("[]");
        }
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
    }
}