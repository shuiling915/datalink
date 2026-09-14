package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.*;
import com.datalink.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final DlUserMapper userMapper;
    private final DlMessageMapper messageMapper;
    private final DlAnnouncementMapper announcementMapper;
    private final DlSqlHistoryMapper sqlHistoryMapper;
    private final DlSqlFavoriteMapper sqlFavoriteMapper;
    private final DlAssetFavoriteMapper assetFavoriteMapper;
    private final DlAssetMapper assetMapper;
    private final DlSqlApprovalMapper sqlApprovalMapper;
    private final DlExportTaskMapper exportTaskMapper;
    private final DlAlertEventMapper alertEventMapper;

    public Map<String, Object> getDashboard(String username) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("username", username);
        dashboard.put("generatedAt", LocalDateTime.now().toString());

        DlUser user = userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", username));
        Long userId = user != null ? user.getId() : null;
        if (user != null) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("userId", user.getId());
            profile.put("username", user.getUsername());
            profile.put("nickname", user.getNickname());
            profile.put("role", user.getRole());
            profile.put("status", user.getStatus());
            dashboard.put("profile", profile);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("unreadMessages", getUnreadMessageCount(username));
        stats.put("sqlHistoryCount", getHistoryCount(userId));
        stats.put("favoriteSqlCount", getFavoriteSqlCount(userId));
        stats.put("favoriteAssetCount", getFavoriteAssetCount(userId));
        stats.put("pendingApprovals", getPendingApprovalCount(username));
        stats.put("runningExports", getRunningExportCount(username));
        stats.put("openAlerts", getOpenAlertCount());
        dashboard.put("stats", stats);

        dashboard.put("recentMessages", getRecentMessages(username, 5));
        dashboard.put("recentAnnouncements", getRecentAnnouncements(3));
        dashboard.put("recentSqlHistory", getRecentSqlHistory(userId, 10));
        dashboard.put("favoriteSql", getFavoriteSql(userId, 5));
        dashboard.put("favoriteAssets", getFavoriteAssets(userId, 5));
        dashboard.put("pendingApprovals", getPendingApprovals(username, 5));
        dashboard.put("recentExports", getRecentExports(username, 5));
        dashboard.put("recentAlerts", getRecentAlerts(5));

        return dashboard;
    }

    private long getUnreadMessageCount(String username) {
        return messageMapper.selectCount(new QueryWrapper<DlMessage>()
                .eq("receiver", username).eq("is_read", 0));
    }

    private long getHistoryCount(Long userId) {
        if (userId == null) return 0;
        return sqlHistoryMapper.selectCount(new QueryWrapper<DlSqlHistory>().eq("user_id", userId));
    }

    private long getFavoriteSqlCount(Long userId) {
        if (userId == null) return 0;
        return sqlFavoriteMapper.selectCount(new QueryWrapper<DlSqlFavorite>().eq("user_id", userId));
    }

    private long getFavoriteAssetCount(Long userId) {
        if (userId == null) return 0;
        return assetFavoriteMapper.selectCount(new QueryWrapper<DlAssetFavorite>().eq("user_id", userId));
    }

    private long getPendingApprovalCount(String username) {
        return sqlApprovalMapper.selectCount(new QueryWrapper<DlSqlApproval>()
                .eq("status", "PENDING")
                .and(w -> w.eq("applicant", username).or().eq("approver", username)));
    }

    private long getRunningExportCount(String username) {
        return exportTaskMapper.selectCount(new QueryWrapper<DlExportTask>()
                .eq("created_by", username).in("status", "PENDING", "RUNNING"));
    }

    private long getOpenAlertCount() {
        return alertEventMapper.selectCount(new QueryWrapper<DlAlertEvent>()
                .in("status", "OPEN", "ACKNOWLEDGED"));
    }

    private List<DlMessage> getRecentMessages(String username, int limit) {
        return messageMapper.selectList(new QueryWrapper<DlMessage>()
                .eq("receiver", username).orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<DlAnnouncement> getRecentAnnouncements(int limit) {
        return announcementMapper.selectList(new QueryWrapper<DlAnnouncement>()
                .eq("status", "published").orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<DlSqlHistory> getRecentSqlHistory(Long userId, int limit) {
        if (userId == null) return Collections.emptyList();
        return sqlHistoryMapper.selectList(new QueryWrapper<DlSqlHistory>()
                .eq("user_id", userId).orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<DlSqlFavorite> getFavoriteSql(Long userId, int limit) {
        if (userId == null) return Collections.emptyList();
        return sqlFavoriteMapper.selectList(new QueryWrapper<DlSqlFavorite>()
                .eq("user_id", userId).orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<Map<String, Object>> getFavoriteAssets(Long userId, int limit) {
        if (userId == null) return Collections.emptyList();
        List<DlAssetFavorite> favs = assetFavoriteMapper.selectList(new QueryWrapper<DlAssetFavorite>()
                .eq("user_id", userId).orderByDesc("created_at").last("LIMIT " + limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlAssetFavorite fav : favs) {
            DlAsset asset = assetMapper.selectById(fav.getAssetId());
            if (asset != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("favoriteId", fav.getId());
                item.put("assetId", asset.getId());
                item.put("assetName", asset.getAssetName());
                item.put("tableName", asset.getTableName());
                item.put("businessDomain", asset.getBusinessDomain());
                item.put("favoritedAt", fav.getCreatedAt());
                result.add(item);
            }
        }
        return result;
    }

    private List<DlSqlApproval> getPendingApprovals(String username, int limit) {
        return sqlApprovalMapper.selectList(new QueryWrapper<DlSqlApproval>()
                .eq("status", "PENDING")
                .and(w -> w.eq("applicant", username).or().eq("approver", username))
                .orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<DlExportTask> getRecentExports(String username, int limit) {
        return exportTaskMapper.selectList(new QueryWrapper<DlExportTask>()
                .eq("created_by", username).orderByDesc("created_at").last("LIMIT " + limit));
    }

    private List<DlAlertEvent> getRecentAlerts(int limit) {
        return alertEventMapper.selectList(new QueryWrapper<DlAlertEvent>()
                .orderByDesc("created_at").last("LIMIT " + limit));
    }
}