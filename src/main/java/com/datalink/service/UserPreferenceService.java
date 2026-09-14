package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlUserPreferenceMapper;
import com.datalink.model.DlUserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final DlUserPreferenceMapper preferenceMapper;

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("theme", "light");
        DEFAULTS.put("language", "zh-CN");
        DEFAULTS.put("defaultDatasourceId", "");
        DEFAULTS.put("pageSize", "20");
        DEFAULTS.put("sqlRowLimit", "1000");
        DEFAULTS.put("fontSize", "14");
        DEFAULTS.put("sidebarCollapsed", "false");
        DEFAULTS.put("autoSave", "true");
        DEFAULTS.put("notifySound", "true");
        DEFAULTS.put("dateFormat", "YYYY-MM-DD HH:mm:ss");
        DEFAULTS.put("timezone", "Asia/Shanghai");
    }

    public Map<String, String> getAll(String username) {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        List<DlUserPreference> prefs = preferenceMapper.selectList(
                new QueryWrapper<DlUserPreference>().eq("username", username));
        for (DlUserPreference p : prefs) {
            result.put(p.getPrefKey(), p.getPrefValue());
        }
        return result;
    }

    public String get(String username, String key) {
        DlUserPreference pref = preferenceMapper.selectOne(
                new QueryWrapper<DlUserPreference>().eq("username", username).eq("pref_key", key));
        if (pref != null) return pref.getPrefValue();
        return DEFAULTS.get(key);
    }

    public void set(String username, String key, String value) {
        DlUserPreference existing = preferenceMapper.selectOne(
                new QueryWrapper<DlUserPreference>().eq("username", username).eq("pref_key", key));
        if (existing != null) {
            existing.setPrefValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.updateById(existing);
        } else {
            DlUserPreference pref = new DlUserPreference();
            pref.setUsername(username);
            pref.setPrefKey(key);
            pref.setPrefValue(value);
            pref.setCreatedAt(LocalDateTime.now());
            pref.setUpdatedAt(LocalDateTime.now());
            preferenceMapper.insert(pref);
        }
    }

    public void setBatch(String username, Map<String, String> prefs) {
        for (Map.Entry<String, String> entry : prefs.entrySet()) {
            set(username, entry.getKey(), entry.getValue());
        }
    }

    public boolean reset(String username) {
        int deleted = preferenceMapper.delete(
                new QueryWrapper<DlUserPreference>().eq("username", username));
        return deleted >= 0;
    }

    public boolean resetKey(String username, String key) {
        int deleted = preferenceMapper.delete(
                new QueryWrapper<DlUserPreference>().eq("username", username).eq("pref_key", key));
        return deleted > 0;
    }

    public Map<String, String> getDefaults() {
        return new LinkedHashMap<>(DEFAULTS);
    }
}