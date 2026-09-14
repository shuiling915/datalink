package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.common.Constants;
import com.datalink.mapper.DlAiConfigMapper;
import com.datalink.model.DlAiConfig;
import com.datalink.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 配置服务 — 多套 AI 模型配置的增删改查、默认切换、密钥加解密
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigService {

    private final DlAiConfigMapper mapper;

    /** 环境变量中的 API Key，作为兜底 */
    @Value("${datalink.ai.api-key:}")
    private String envApiKey;

    @Value("${datalink.crypto.key:}")
    private String cryptoKey;

    /** 列表（api_key 脱敏为掩码） */
    public List<DlAiConfig> list() {
        List<DlAiConfig> list = mapper.selectList(
                new QueryWrapper<DlAiConfig>().orderByDesc("is_default").orderByDesc("id"));
        for (DlAiConfig c : list) {
            if (c.getApiKey() != null && !c.getApiKey().isEmpty()) {
                c.setApiKey(Constants.PASSWORD_MASK);
            } else {
                c.setApiKey("");
            }
        }
        return list;
    }

    /** 新建 */
    public DlAiConfig create(DlAiConfig c) {
        if (c.getName() == null || c.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        c.setId(null);
        c.setApiKey(encryptKey(c.getApiKey()));
        c.setStatus(c.getStatus() == null ? 1 : c.getStatus());
        c.setIsDefault(0);
        c.setCreatedAt(LocalDateTime.now());
        mapper.insert(c);
        // 第一条自动设为默认
        Long count = mapper.selectCount(null);
        if (count != null && count == 1) {
            setDefault(c.getId());
        }
        c.setApiKey(c.getApiKey() != null ? Constants.PASSWORD_MASK : "");
        return c;
    }

    /** 编辑（key 只有传了新明文才更新） */
    public void update(Long id, DlAiConfig c) {
        DlAiConfig ex = mapper.selectById(id);
        if (ex == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        ex.setName(c.getName());
        ex.setProvider(c.getProvider());
        ex.setBaseUrl(c.getBaseUrl());
        ex.setModel(c.getModel());
        if (c.getStatus() != null) {
            ex.setStatus(c.getStatus());
        }
        // 只有传入了新明文 key 才更新；空或掩码表示不修改
        String newKey = encryptKey(c.getApiKey());
        if (newKey != null) {
            ex.setApiKey(newKey);
        }
        mapper.updateById(ex);
    }

    /** 删除 */
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /** 设为默认（其余取消默认） */
    public void setDefault(Long id) {
        DlAiConfig clear = new DlAiConfig();
        clear.setIsDefault(0);
        mapper.update(clear, new QueryWrapper<DlAiConfig>().isNotNull("id"));
        DlAiConfig target = mapper.selectById(id);
        if (target != null) {
            target.setIsDefault(1);
            target.setStatus(1);
            mapper.updateById(target);
        }
    }

    /** 测试连接用的 key：优先用传入的明文，其次从数据库解密，最后兜底环境变量 */
    public String resolveTestKey(DlAiConfig c) {
        // 1. 页面传入了新明文 key
        String plain = c.getApiKey();
        if (plain != null && !plain.isEmpty() && !Constants.PASSWORD_MASK.equals(plain)) {
            return plain;
        }
        // 2. 从数据库解密已保存的 key
        if (c.getId() != null) {
            DlAiConfig saved = mapper.selectById(c.getId());
            if (saved != null && saved.getApiKey() != null && !saved.getApiKey().isEmpty()) {
                try {
                    return CryptoUtil.decryptSafe(saved.getApiKey(), cryptoKey);
                } catch (Exception e) {
                    log.warn("解密已保存的 API Key 失败", e);
                }
            }
        }
        // 3. 兜底环境变量
        return envApiKey;
    }

    /** 将明文 key 加密；空/掩码返回 null 表示不更新 */
    private String encryptKey(String plain) {
        if (plain == null || plain.isEmpty() || Constants.PASSWORD_MASK.equals(plain)) {
            return null;
        }
        try {
            return CryptoUtil.encrypt(plain, cryptoKey);
        } catch (Exception e) {
            log.error("加密 API Key 失败", e);
            throw new RuntimeException("保存 API Key 失败: " + e.getMessage());
        }
    }
}