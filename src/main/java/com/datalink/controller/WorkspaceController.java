package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
@Tag(name = "个人工作台", description = "用户活动概览、未读消息、收藏、待办")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "获取个人工作台数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        return R.ok(workspaceService.getDashboard(user.getUsername()));
    }
}