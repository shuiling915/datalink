package com.datalink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlAlertEventMapper;
import com.datalink.model.DlAlertEvent;
import com.datalink.model.R;
import com.datalink.service.AlertCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alert-center")
@RequiredArgsConstructor
@Tag(name = "告警中心", description = "告警事件查询、确认、解决、统计")
public class AlertCenterController {

    private final AlertCenterService alertCenterService;
    private final DlAlertEventMapper alertEventMapper;

    @Operation(summary = "分页查询告警事件")
    @GetMapping("/events")
    public R<Page<DlAlertEvent>> listEvents(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String severity,
                                             @RequestParam(required = false) String sourceType,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(alertCenterService.queryEvents(page, size, status, severity, sourceType, keyword));
    }

    @Operation(summary = "告警事件详情")
    @GetMapping("/events/{id}")
    public R<DlAlertEvent> getEvent(@PathVariable Long id) {
        return R.ok(alertEventMapper.selectById(id));
    }

    @Operation(summary = "确认告警")
    @PostMapping("/events/{id}/acknowledge")
    @PreAuthorize("hasAnyAuthority('alert:edit','admin:edit')")
    public R<DlAlertEvent> acknowledge(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails user) {
        String operator = user != null ? user.getUsername() : "anonymous";
        DlAlertEvent event = alertCenterService.acknowledge(id, operator);
        if (event == null) return R.fail("告警事件不存在");
        return R.ok(event);
    }

    @Operation(summary = "解决告警")
    @PostMapping("/events/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('alert:edit','admin:edit')")
    public R<DlAlertEvent> resolve(@PathVariable Long id,
                                    @RequestParam(required = false) String note,
                                    @AuthenticationPrincipal UserDetails user) {
        String operator = user != null ? user.getUsername() : "anonymous";
        DlAlertEvent event = alertCenterService.resolve(id, operator, note);
        if (event == null) return R.fail("告警事件不存在");
        return R.ok(event);
    }

    @Operation(summary = "指派告警")
    @PostMapping("/events/{id}/assign")
    @PreAuthorize("hasAnyAuthority('alert:edit','admin:edit')")
    public R<Void> assign(@PathVariable Long id, @RequestParam String assignee) {
        boolean ok = alertCenterService.assign(id, assignee);
        return ok ? R.ok() : R.fail("指派失败");
    }

    @Operation(summary = "告警统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(defaultValue = "7") int days) {
        return R.ok(alertCenterService.getStatistics(days));
    }
}