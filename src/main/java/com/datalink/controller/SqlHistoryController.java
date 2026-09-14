package com.datalink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlUserMapper;
import com.datalink.model.DlSqlFavorite;
import com.datalink.model.DlSqlHistory;
import com.datalink.model.DlUser;
import com.datalink.model.R;
import com.datalink.service.SqlHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SQL查询历史与收藏 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/sql-history")
@RequiredArgsConstructor
@Tag(name = "SQL历史与收藏", description = "SQL执行历史记录、查询、收藏管理")
public class SqlHistoryController {

    private final SqlHistoryService sqlHistoryService;
    private final DlUserMapper userMapper;

    @Operation(summary = "分页查询SQL执行历史")
    @GetMapping("/list")
    public R<Page<DlSqlHistory>> listHistory(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String sqlType,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : null;
        return R.ok(sqlHistoryService.listHistory(userId, keyword, sqlType, status, start, end, page, size));
    }

    @Operation(summary = "获取历史详情")
    @GetMapping("/{id}")
    public R<DlSqlHistory> getHistory(@PathVariable Long id) {
        DlSqlHistory history = sqlHistoryService.getHistory(id);
        if (history == null) return R.fail("记录不存在");
        return R.ok(history);
    }

    @Operation(summary = "删除历史记录")
    @DeleteMapping("/{id}")
    public R<String> deleteHistory(@PathVariable Long id) {
        sqlHistoryService.deleteHistory(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "清空当前用户历史")
    @DeleteMapping("/clear")
    public R<String> clearHistory(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return R.fail("未登录");
        Long userId = getUserId(user);
        sqlHistoryService.clearHistory(userId);
        return R.ok("已清空");
    }

    @Operation(summary = "用户SQL执行统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) Long userId,
                                         @RequestParam(defaultValue = "7") int days) {
        return R.ok(sqlHistoryService.getUserStats(userId, days));
    }

    // ==================== 收藏 ====================

    @Operation(summary = "添加SQL收藏")
    @PostMapping("/favorite")
    public R<DlSqlFavorite> addFavorite(@RequestBody DlSqlFavorite favorite,
                                         @AuthenticationPrincipal UserDetails user) {
        if (user != null) {
            favorite.setUsername(user.getUsername());
            if (favorite.getUserId() == null) {
                favorite.setUserId(getUserId(user));
            }
        }
        return R.ok(sqlHistoryService.addFavorite(favorite));
    }

    @Operation(summary = "更新SQL收藏")
    @PutMapping("/favorite")
    public R<DlSqlFavorite> updateFavorite(@RequestBody DlSqlFavorite favorite) {
        return R.ok(sqlHistoryService.updateFavorite(favorite));
    }

    @Operation(summary = "删除SQL收藏")
    @DeleteMapping("/favorite/{id}")
    public R<String> deleteFavorite(@PathVariable Long id) {
        sqlHistoryService.deleteFavorite(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "查询收藏列表")
    @GetMapping("/favorites")
    public R<List<DlSqlFavorite>> listFavorites(@RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String tag) {
        return R.ok(sqlHistoryService.listFavorites(userId, keyword, tag));
    }

    @Operation(summary = "收藏详情")
    @GetMapping("/favorite/{id}")
    public R<DlSqlFavorite> getFavorite(@PathVariable Long id) {
        DlSqlFavorite fav = sqlHistoryService.getFavorite(id);
        if (fav == null) return R.fail("收藏不存在");
        return R.ok(fav);
    }

    private Long getUserId(UserDetails user) {
        if (user == null) return null;
        DlUser dlUser = userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", user.getUsername()));
        return dlUser != null ? dlUser.getId() : null;
    }
}