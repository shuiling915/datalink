package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.annotation.AuditLog;
import com.datalink.mapper.DlAlertChannelMapper;
import com.datalink.model.DlAlertChannel;
import com.datalink.model.R;
import com.datalink.service.AlertNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警通知渠道管理 Controller
 */
@Tag(name = "告警通知渠道")
@RestController
@RequestMapping("/api/alert-channel")
@RequiredArgsConstructor
public class AlertChannelController {

    private final DlAlertChannelMapper channelMapper;
    private final AlertNotifyService alertNotifyService;

    @Operation(summary = "渠道列表")
    @GetMapping("/list")
    public R<List<DlAlertChannel>> list() {
        return R.ok(channelMapper.selectList(
                new QueryWrapper<DlAlertChannel>().orderByDesc("created_at")));
    }

    @Operation(summary = "保存渠道")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "保存告警渠道")
    public R<DlAlertChannel> save(@RequestBody DlAlertChannel channel) {
        if (channel.getId() != null) {
            channel.setUpdatedAt(LocalDateTime.now());
            channelMapper.updateById(channel);
        } else {
            channel.setCreatedAt(LocalDateTime.now());
            channel.setUpdatedAt(LocalDateTime.now());
            if (channel.getEnabled() == null) channel.setEnabled(1);
            channelMapper.insert(channel);
        }
        return R.ok(channel);
    }

    @Operation(summary = "删除渠道")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "删除告警渠道")
    public R<Void> delete(@PathVariable Long id) {
        channelMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "测试渠道")
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('system:config')")
    public R<Map<String, Object>> test(@RequestBody DlAlertChannel channel) {
        Map<String, Object> result = new HashMap<>();
        try {
            alertNotifyService.sendAlert("测试告警", "这是一条测试告警消息，收到请忽略。");
            result.put("success", true);
            result.put("message", "测试消息已发送，请检查对应渠道");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return R.ok(result);
    }
}