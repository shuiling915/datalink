package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.annotation.AuditLog;
import com.datalink.mapper.DlUserMapper;
import com.datalink.model.*;
import com.datalink.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据资产目录 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
@Tag(name = "数据资产目录")
public class AssetController {

    private final AssetService assetService;
    private final DlUserMapper userMapper;

    private Long getUserId(UserDetails user) {
        if (user == null) return null;
        DlUser dlUser = userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", user.getUsername()));
        return dlUser != null ? dlUser.getId() : null;
    }

    @Operation(summary = "资产列表")
    @GetMapping("/list")
    public R<List<DlAsset>> list(@RequestParam(required = false) Long datasourceId,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer assetLevel,
                                 @RequestParam(required = false) String owner,
                                 @RequestParam(required = false) String businessDomain,
                                 @RequestParam(required = false) String tag) {
        return R.ok(assetService.list(datasourceId, keyword, assetLevel, owner, businessDomain, tag));
    }

    @Operation(summary = "资产详情")
    @GetMapping("/{id}")
    public R<DlAsset> detail(@PathVariable Long id) {
        return R.ok(assetService.getById(id));
    }

    @Operation(summary = "保存资产")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('metadata:edit')")
    @AuditLog(module = "数据资产", operation = "保存资产")
    public R<DlAsset> save(@RequestBody DlAsset asset,
                           @AuthenticationPrincipal UserDetails user) {
        return R.ok(assetService.save(asset, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "删除资产")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('metadata:edit')")
    @AuditLog(module = "数据资产", operation = "删除资产")
    public R<String> delete(@PathVariable Long id) {
        assetService.delete(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "资产统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(assetService.getStats());
    }

    // ==================== 标签 ====================

    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public R<List<DlAssetTag>> listTags(@RequestParam(required = false) String category) {
        return R.ok(assetService.listTags(category));
    }

    @Operation(summary = "保存标签")
    @PostMapping("/tag/save")
    @PreAuthorize("hasAuthority('metadata:edit')")
    public R<DlAssetTag> saveTag(@RequestBody DlAssetTag tag,
                                 @AuthenticationPrincipal UserDetails user) {
        return R.ok(assetService.saveTag(tag, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/tag/{id}")
    @PreAuthorize("hasAuthority('metadata:edit')")
    public R<String> deleteTag(@PathVariable Long id) {
        assetService.deleteTag(id);
        return R.ok("删除成功");
    }

    // ==================== 收藏 ====================

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{id}/favorite")
    public R<Boolean> toggleFavorite(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        if (userId == null) return R.fail("请先登录");
        return R.ok(assetService.toggleFavorite(id, userId));
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/favorites")
    public R<List<DlAsset>> favorites(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        if (userId == null) return R.fail("请先登录");
        return R.ok(assetService.listFavorites(userId));
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/{id}/favorite/check")
    public R<Boolean> checkFavorite(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        if (userId == null) return R.ok(false);
        return R.ok(assetService.isFavorite(id, userId));
    }
}