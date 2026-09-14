package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.datalink.mapper.DlMessageMapper;
import com.datalink.model.DlMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息服务 — 消息发送、查询、已读、统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final DlMessageMapper messageMapper;

    /**
     * 发送站内消息给指定用户
     */
    public DlMessage send(String receiver, String title, String content, String type, Long bizId) {
        DlMessage msg = new DlMessage();
        msg.setReceiver(receiver);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType(type != null ? type : "system");
        msg.setBizId(bizId);
        msg.setIsRead(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    /**
     * 异步发送消息
     */
    @Async
    public void sendAsync(String receiver, String title, String content, String type, Long bizId) {
        try {
            send(receiver, title, content, type, bizId);
        } catch (Exception e) {
            log.error("异步发送站内消息失败: receiver={}, title={}", receiver, title, e);
        }
    }

    /**
     * 批量发送消息给多个用户
     */
    public void batchSend(List<String> receivers, String title, String content, String type, Long bizId) {
        for (String receiver : receivers) {
            try {
                send(receiver, title, content, type, bizId);
            } catch (Exception e) {
                log.error("批量发送消息失败: receiver={}", receiver, e);
            }
        }
    }

    /**
     * 查询用户消息列表（分页）
     */
    public List<DlMessage> listByReceiver(String receiver, String type, Boolean isRead, int limit) {
        QueryWrapper<DlMessage> qw = new QueryWrapper<>();
        qw.eq("receiver", receiver);
        if (type != null && !type.isEmpty()) {
            qw.eq("type", type);
        }
        if (isRead != null) {
            qw.eq("is_read", isRead ? 1 : 0);
        }
        qw.orderByDesc("created_at");
        qw.last("LIMIT " + Math.min(limit, 200));
        return messageMapper.selectList(qw);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount(String receiver) {
        return messageMapper.selectCount(
                new QueryWrapper<DlMessage>().eq("receiver", receiver).eq("is_read", 0));
    }

    /**
     * 标记单条消息为已读
     */
    public void markAsRead(Long id, String receiver) {
        UpdateWrapper<DlMessage> uw = new UpdateWrapper<>();
        uw.eq("id", id).eq("receiver", receiver).set("is_read", 1);
        messageMapper.update(null, uw);
    }

    /**
     * 标记用户所有消息为已读
     */
    public void markAllAsRead(String receiver) {
        UpdateWrapper<DlMessage> uw = new UpdateWrapper<>();
        uw.eq("receiver", receiver).eq("is_read", 0).set("is_read", 1);
        messageMapper.update(null, uw);
    }

    /**
     * 删除消息
     */
    public void delete(Long id, String receiver) {
        QueryWrapper<DlMessage> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("receiver", receiver);
        messageMapper.delete(qw);
    }
}