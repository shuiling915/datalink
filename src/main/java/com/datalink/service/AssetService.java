package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlAssetFavoriteMapper;
import com.datalink.mapper.DlAssetMapper;
import com.datalink.mapper.DlAssetTagMapper;
import com.datalink.model.DlAsset;
import com.datalink.model.DlAssetFavorite;
import com.datalink.model.DlAssetTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据资产目录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final DlAssetMapper assetMapper;
    private final DlAssetTagMapper tagMapper;
    private final DlAssetFavoriteMapper favoriteMapper;

    // ==================== 资产 CRUD ====================

    public DlAsset save(DlAsset asset, String operator) {
        if (asset.getId() != null) {
            asset.setUpdatedAt(LocalDateTime.now());
            assetMapper.updateById(asset);
        } else {
            asset.setCreatedBy(operator);
            asset.setStatus(asset.getStatus() != null ? asset.getStatus() : 1);
            asset.setAccessCount(0L);
            asset.setCreatedAt(LocalDateTime.now());
            asset.setUpdatedAt(LocalDateTime.now());
            assetMapper.insert(asset);
        }
        return asset;
    }

    public DlAsset getById(Long id) {
        DlAsset asset = assetMapper.selectById(id);
        if (asset != null) {
            asset.setAccessCount((asset.getAccessCount() == null ? 0 : asset.getAccessCount()) + 1);
            asset.setLastAccessAt(LocalDateTime.now());
            assetMapper.updateById(asset);
        }
        return asset;
    }

    public DlAsset findByDatasourceAndTable(Long datasourceId, String tableName) {
        return assetMapper.selectOne(new QueryWrapper<DlAsset>()
                .eq("datasource_id", datasourceId)
                .eq("table_name", tableName)
                .last("LIMIT 1"));
    }

    public void delete(Long id) {
        assetMapper.deleteById(id);
        favoriteMapper.delete(new QueryWrapper<DlAssetFavorite>().eq("asset_id", id));
    }

    public List<DlAsset> list(Long datasourceId, String keyword, Integer assetLevel,
                              String owner, String businessDomain, String tag) {
        QueryWrapper<DlAsset> qw = new QueryWrapper<>();
        if (datasourceId != null) qw.eq("datasource_id", datasourceId);
        if (assetLevel != null) qw.eq("asset_level", assetLevel);
        if (owner != null && !owner.isEmpty()) qw.eq("owner", owner);
        if (businessDomain != null && !businessDomain.isEmpty()) qw.eq("business_domain", businessDomain);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("table_name", keyword).or().like("asset_name", keyword).or().like("description", keyword));
        }
        if (tag != null && !tag.isEmpty()) {
            qw.like("tags", tag);
        }
        qw.eq("status", 1);
        qw.orderByDesc("access_count");
        return assetMapper.selectList(qw);
    }

    // ==================== 标签管理 ====================

    public List<DlAssetTag> listTags(String category) {
        QueryWrapper<DlAssetTag> qw = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        qw.orderByAsc("category").orderByAsc("tag_name");
        return tagMapper.selectList(qw);
    }

    public DlAssetTag saveTag(DlAssetTag tag, String operator) {
        if (tag.getId() != null) {
            tagMapper.updateById(tag);
        } else {
            tag.setCreatedBy(operator);
            tag.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        }
        return tag;
    }

    public void deleteTag(Long id) {
        tagMapper.deleteById(id);
    }

    // ==================== 收藏 ====================

    public boolean toggleFavorite(Long assetId, Long userId) {
        QueryWrapper<DlAssetFavorite> qw = new QueryWrapper<>();
        qw.eq("asset_id", assetId).eq("user_id", userId);
        DlAssetFavorite fav = favoriteMapper.selectOne(qw);
        if (fav != null) {
            favoriteMapper.deleteById(fav.getId());
            return false;
        } else {
            fav = new DlAssetFavorite();
            fav.setAssetId(assetId);
            fav.setUserId(userId);
            fav.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(fav);
            return true;
        }
    }

    public List<DlAsset> listFavorites(Long userId) {
        List<DlAssetFavorite> favs = favoriteMapper.selectList(
                new QueryWrapper<DlAssetFavorite>().eq("user_id", userId));
        if (favs.isEmpty()) return Collections.emptyList();
        List<Long> assetIds = favs.stream().map(DlAssetFavorite::getAssetId).collect(Collectors.toList());
        return assetMapper.selectBatchIds(assetIds);
    }

    public boolean isFavorite(Long assetId, Long userId) {
        return favoriteMapper.selectCount(
                new QueryWrapper<DlAssetFavorite>().eq("asset_id", assetId).eq("user_id", userId)) > 0;
    }

    // ==================== 统计 ====================

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssets", assetMapper.selectCount(null));
        stats.put("onlineAssets", assetMapper.selectCount(new QueryWrapper<DlAsset>().eq("status", 1)));

        // 按分级统计
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (int i = 1; i <= 4; i++) {
            long cnt = assetMapper.selectCount(new QueryWrapper<DlAsset>().eq("asset_level", i));
            byLevel.put("L" + i, cnt);
        }
        stats.put("byLevel", byLevel);

        // 按业务域统计
        List<Map<String, Object>> byDomain = new ArrayList<>();
        List<DlAsset> all = assetMapper.selectList(new QueryWrapper<DlAsset>().select("business_domain"));
        Map<String, Long> domainMap = all.stream()
                .filter(a -> a.getBusinessDomain() != null && !a.getBusinessDomain().isEmpty())
                .collect(Collectors.groupingBy(DlAsset::getBusinessDomain, Collectors.counting()));
        domainMap.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("domain", k);
            m.put("count", v);
            byDomain.add(m);
        });
        stats.put("byDomain", byDomain);

        // 热门资产 Top10
        List<DlAsset> hot = assetMapper.selectList(
                new QueryWrapper<DlAsset>().eq("status", 1).orderByDesc("access_count").last("LIMIT 10"));
        stats.put("hotAssets", hot);

        stats.put("totalTags", tagMapper.selectCount(null));
        return stats;
    }
}