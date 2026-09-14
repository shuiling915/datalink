package com.datalink.controller;

import com.datalink.model.DlMessage;
import com.datalink.model.R;
import com.datalink.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内消息 Controller — 当前用户的消息查询、已读、统计
 */
@Tag(name = "站内消息")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "我的消息列表")
    @GetMapping("/list")
    public R<List<DlMessage>> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "50") int limit) {
        return R.ok(messageService.listByReceiver(user.getUsername(), type, isRead, limit));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public R<Map<String, Object>> unreadCount(@AuthenticationPrincipal UserDetails user) {
        long count = messageService.getUnreadCount(user.getUsername());
        return R.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "标记消息为已读")
    @PostMapping("/{id}/read")
    public R<String> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        messageService.markAsRead(id, user.getUsername());
        return R.ok("标记成功");
    }

    @Operation(summary = "全部标记为已读")
    @PostMapping("/read-all")
    public R<String> markAllAsRead(@AuthenticationPrincipal UserDetails user) {
        messageService.markAllAsRead(user.getUsername());
        return R.ok("全部标记成功");
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        messageService.delete(id, user.getUsername());
        return R.ok("删除成功");
    }
}