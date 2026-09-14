package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/preference")
@RequiredArgsConstructor
@Tag(name = "用户偏好设置", description = "主题、语言、默认数据源、分页大小等个性化配置")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @Operation(summary = "获取所有偏好设置")
    @GetMapping
    public R<Map<String, String>> getAll(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        return R.ok(preferenceService.getAll(user.getUsername()));
    }

    @Operation(summary = "获取单个偏好")
    @GetMapping("/{key}")
    public R<String> get(@PathVariable String key, @AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        return R.ok(preferenceService.get(user.getUsername(), key));
    }

    @Operation(summary = "设置单个偏好")
    @PutMapping("/{key}")
    public R<Void> set(@PathVariable String key, @RequestBody String value,
                       @AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        preferenceService.set(user.getUsername(), key, value);
        return R.ok();
    }

    @Operation(summary = "批量设置偏好")
    @PutMapping("/batch")
    public R<Void> setBatch(@RequestBody Map<String, String> prefs,
                            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        preferenceService.setBatch(user.getUsername(), prefs);
        return R.ok();
    }

    @Operation(summary = "重置所有偏好")
    @DeleteMapping
    public R<Void> reset(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        preferenceService.reset(user.getUsername());
        return R.ok();
    }

    @Operation(summary = "重置单个偏好")
    @DeleteMapping("/{key}")
    public R<Void> resetKey(@PathVariable String key, @AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        preferenceService.resetKey(user.getUsername(), key);
        return R.ok();
    }

    @Operation(summary = "获取默认偏好")
    @GetMapping("/defaults")
    public R<Map<String, String>> getDefaults() {
        return R.ok(preferenceService.getDefaults());
    }
}