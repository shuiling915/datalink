package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlAnnouncement;
import com.datalink.model.R;
import com.datalink.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告 Controller — 公告发布、查询、管理
 */
@Tag(name = "系统公告")
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "已发布公告列表")
    @GetMapping("/list")
    public R<List<DlAnnouncement>> listPublished(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(announcementService.listPublished(limit));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public R<DlAnnouncement> getById(@PathVariable Long id) {
        return R.ok(announcementService.getById(id));
    }

    @Operation(summary = "全部公告列表（管理）")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:config')")
    public R<List<DlAnnouncement>> listAll(@RequestParam(required = false) String keyword) {
        return R.ok(announcementService.listAll(keyword));
    }

    @Operation(summary = "保存公告")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "保存公告")
    public R<DlAnnouncement> save(@RequestBody DlAnnouncement announcement,
                                  @AuthenticationPrincipal UserDetails user) {
        return R.ok(announcementService.save(announcement, user.getUsername()));
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "发布公告")
    public R<DlAnnouncement> publish(@PathVariable Long id) {
        return R.ok(announcementService.publish(id));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "删除公告")
    public R<String> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok("删除成功");
    }
}