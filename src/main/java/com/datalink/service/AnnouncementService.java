package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlAnnouncementMapper;
import com.datalink.model.DlAnnouncement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统公告服务 — 公告的发布、查询、删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final DlAnnouncementMapper announcementMapper;

    /**
     * 保存公告（新增或更新）
     */
    public DlAnnouncement save(DlAnnouncement announcement, String createdBy) {
        if (announcement.getTitle() == null || announcement.getTitle().isEmpty()) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().isEmpty()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }

        if (announcement.getId() != null) {
            announcement.setUpdatedAt(LocalDateTime.now());
            announcementMapper.updateById(announcement);
        } else {
            announcement.setCreatedBy(createdBy);
            announcement.setType(announcement.getType() != null ? announcement.getType() : "notice");
            announcement.setPriority(announcement.getPriority() != null ? announcement.getPriority() : 0);
            announcement.setStatus(announcement.getStatus() != null ? announcement.getStatus() : 1);
            if (announcement.getStatus() == 1 && announcement.getPublishTime() == null) {
                announcement.setPublishTime(LocalDateTime.now());
            }
            announcement.setCreatedAt(LocalDateTime.now());
            announcement.setUpdatedAt(LocalDateTime.now());
            announcementMapper.insert(announcement);
        }
        return announcement;
    }

    /**
     * 查询已发布的公告列表
     */
    public List<DlAnnouncement> listPublished(int limit) {
        QueryWrapper<DlAnnouncement> qw = new QueryWrapper<>();
        qw.eq("status", 1).orderByDesc("priority").orderByDesc("publish_time");
        qw.last("LIMIT " + Math.min(limit, 100));
        return announcementMapper.selectList(qw);
    }

    /**
     * 查询所有公告（管理用，含草稿）
     */
    public List<DlAnnouncement> listAll(String keyword) {
        QueryWrapper<DlAnnouncement> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("title", keyword);
        }
        qw.orderByDesc("created_at");
        return announcementMapper.selectList(qw);
    }

    /**
     * 公告详情
     */
    public DlAnnouncement getById(Long id) {
        return announcementMapper.selectById(id);
    }

    /**
     * 发布公告
     */
    public DlAnnouncement publish(Long id) {
        DlAnnouncement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        ann.setStatus(1);
        ann.setPublishTime(LocalDateTime.now());
        ann.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(ann);
        return ann;
    }

    /**
     * 删除公告
     */
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}