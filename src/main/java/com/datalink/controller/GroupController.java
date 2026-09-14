package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlGroupMapper;
import com.datalink.mapper.DlGroupMemberMapper;
import com.datalink.model.DlGroup;
import com.datalink.model.DlGroupMember;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分组管理 Controller
 */
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
@Tag(name = "分组管理", description = "分组与成员的增删改查")
public class GroupController {

    private final DlGroupMapper groupMapper;
    private final DlGroupMemberMapper memberMapper;

    /**
     * 获取所有分组
     */
    @Operation(summary = "获取分组列表")
    @GetMapping("/list")
    public R<List<DlGroup>> list() {
        return R.ok(groupMapper.selectList(null));
    }

    /**
     * 创建分组
     */
    @Operation(summary = "创建分组")
    @PostMapping
    public R<DlGroup> create(@RequestBody DlGroup group) {
        group.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(group);
        return R.ok(group);
    }

    /**
     * 更新分组
     */
    @Operation(summary = "更新分组")
    @PutMapping("/{id}")
    public R<DlGroup> update(@PathVariable Long id, @RequestBody DlGroup group) {
        group.setId(id);
        groupMapper.updateById(group);
        return R.ok(group);
    }

    /**
     * 删除分组（级联删除成员）
     */
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "删除分组")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        QueryWrapper<DlGroupMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("group_id", id);
        memberMapper.delete(memberQuery);
        groupMapper.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取分组成员列表
     */
    @Operation(summary = "获取分组成员列表")
    @GetMapping("/{id}/members")
    public R<List<DlGroupMember>> listMembers(@PathVariable Long id) {
        QueryWrapper<DlGroupMember> qw = new QueryWrapper<>();
        qw.eq("group_id", id);
        return R.ok(memberMapper.selectList(qw));
    }

    /**
     * 添加分组成员
     */
    @Operation(summary = "添加分组成员")
    @PostMapping("/{id}/members")
    public R<DlGroupMember> addMember(@PathVariable Long id, @RequestBody DlGroupMember member) {
        member.setGroupId(id);
        member.setCreatedAt(LocalDateTime.now());
        memberMapper.insert(member);
        return R.ok(member);
    }

    /**
     * 移除分组成员
     */
    @Operation(summary = "移除分组成员")
    @DeleteMapping("/{groupId}/members/{memberId}")
    public R<String> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        memberMapper.deleteById(memberId);
        return R.ok("删除成功");
    }
}
