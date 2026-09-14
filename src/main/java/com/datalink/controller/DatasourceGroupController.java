package com.datalink.controller;

import com.datalink.model.DlDatasource;
import com.datalink.model.DlDatasourceGroup;
import com.datalink.model.R;
import com.datalink.service.DatasourceGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource-group")
@RequiredArgsConstructor
@Tag(name = "数据源分组管理", description = "数据源分组的增删改查、分组树、数据源分配")
public class DatasourceGroupController {

    private final DatasourceGroupService groupService;

    @Operation(summary = "查询所有分组")
    @GetMapping("/list")
    public R<List<DlDatasourceGroup>> listGroups() {
        return R.ok(groupService.listGroups());
    }

    @Operation(summary = "获取分组详情")
    @GetMapping("/{id}")
    public R<DlDatasourceGroup> getGroup(@PathVariable Long id) {
        DlDatasourceGroup group = groupService.getGroup(id);
        return group != null ? R.ok(group) : R.fail("分组不存在");
    }

    @Operation(summary = "创建分组")
    @PostMapping
    public R<DlDatasourceGroup> createGroup(@RequestBody DlDatasourceGroup group,
                                             @AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : "anonymous";
        return R.ok(groupService.createGroup(group, username));
    }

    @Operation(summary = "更新分组")
    @PutMapping("/{id}")
    public R<DlDatasourceGroup> updateGroup(@PathVariable Long id,
                                             @RequestBody DlDatasourceGroup group) {
        DlDatasourceGroup updated = groupService.updateGroup(id, group);
        return updated != null ? R.ok(updated) : R.fail("分组不存在");
    }

    @Operation(summary = "删除分组")
    @DeleteMapping("/{id}")
    public R<Void> deleteGroup(@PathVariable Long id,
                                @RequestParam(defaultValue = "true") boolean moveToUngrouped) {
        return groupService.deleteGroup(id, moveToUngrouped) ? R.ok() : R.fail("分组不存在");
    }

    @Operation(summary = "分配数据源到分组")
    @PutMapping("/{groupId}/datasource/{datasourceId}")
    public R<Void> assignDatasource(@PathVariable Long groupId, @PathVariable Long datasourceId) {
        return groupService.assignDatasourceToGroup(datasourceId, groupId) ? R.ok() : R.fail("操作失败");
    }

    @Operation(summary = "将数据源移出分组")
    @DeleteMapping("/datasource/{datasourceId}")
    public R<Void> removeDatasourceFromGroup(@PathVariable Long datasourceId) {
        return groupService.assignDatasourceToGroup(datasourceId, null) ? R.ok() : R.fail("操作失败");
    }

    @Operation(summary = "获取分组下的数据源")
    @GetMapping("/{id}/datasources")
    public R<List<DlDatasource>> getDatasourcesByGroup(@PathVariable Long id) {
        return R.ok(groupService.getDatasourcesByGroup(id));
    }

    @Operation(summary = "获取分组树（含未分组）")
    @GetMapping("/tree")
    public R<Map<String, Object>> getGroupTree() {
        return R.ok(groupService.getGroupTree());
    }
}