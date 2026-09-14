package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.exception.ResourceNotFoundException;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.mapper.DlScriptFolderMapper;
import com.datalink.mapper.DlScriptMapper;
import com.datalink.mapper.DlScriptVersionMapper;
import com.datalink.mapper.DlSyncTaskMapper;
import com.datalink.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.*;

/**
 * 脚本管理 Service — 树构建、版本管理、脚本/文件夹/同步任务 CRUD
 */
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final DlScriptFolderMapper folderMapper;
    private final DlScriptMapper scriptMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DlScriptVersionMapper scriptVersionMapper;
    private final DlSyncTaskMapper syncTaskMapper;

    // ========== 文件树 ==========

    public List<Map<String, Object>> getTree() {
        QueryWrapper<DlScriptFolder> qw = new QueryWrapper<>();
        qw.orderByAsc("sort_order", "id");
        List<DlScriptFolder> folders = folderMapper.selectList(qw);
        List<DlScript> scripts = scriptMapper.selectList(null);
        List<DlDatasource> datasources = datasourceMapper.selectList(null);
        List<DlSyncTask> syncTasks = syncTaskMapper.selectList(null);
        return buildTree(folders, scripts, datasources, syncTasks, 0L);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<DlScriptFolder> folders,
                                                 List<DlScript> scripts,
                                                 List<DlDatasource> datasources,
                                                 List<DlSyncTask> syncTasks,
                                                 Long parentId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (DlScriptFolder f : folders) {
            if (!Objects.equals(f.getParentId(), parentId)) continue;

            Map<String, Object> node = new HashMap<>();
            node.put("id", f.getId());
            node.put("name", f.getFolderName());
            node.put("type", "folder");
            node.put("layer", f.getLayer());
            List<Map<String, Object>> children = buildTree(folders, scripts, datasources, syncTasks, f.getId());

            if ("数据源".equals(f.getLayer())) {
                for (DlDatasource ds : datasources) {
                    Map<String, Object> dsNode = new HashMap<>();
                    dsNode.put("id", ds.getId());
                    dsNode.put("name", ds.getName());
                    dsNode.put("type", "datasource");
                    dsNode.put("host", ds.getHost());
                    dsNode.put("port", ds.getPort());
                    dsNode.put("dbType", ds.getType());
                    children.add(dsNode);
                }
            } else if ("ODS".equals(f.getLayer())) {
                for (DlSyncTask t : syncTasks) {
                    Map<String, Object> tNode = new HashMap<>();
                    tNode.put("id", t.getId());
                    tNode.put("name", t.getTaskName());
                    tNode.put("type", "syncTask");
                    tNode.put("sourceDb", t.getSourceDb());
                    tNode.put("sourceTable", t.getSourceTable());
                    tNode.put("syncMode", t.getSyncMode());
                    tNode.put("sourceDsId", t.getSourceDsId());
                    tNode.put("scheduleStatus", t.getScheduleStatus());
                    children.add(tNode);
                }
            } else {
                for (DlScript s : scripts) {
                    if (Objects.equals(s.getFolderId(), f.getId())) {
                        Map<String, Object> sNode = new HashMap<>();
                        sNode.put("id", s.getId());
                        sNode.put("name", s.getScriptName());
                        sNode.put("type", "script");
                        sNode.put("scriptType", s.getScriptType());
                        sNode.put("scheduleStatus", s.getScheduleStatus());
                        children.add(sNode);
                    }
                }
            }
            node.put("children", children);
            nodes.add(node);
        }
        return nodes;
    }

    // ========== 脚本 CRUD ==========

    public DlScript getById(Long id) {
        return scriptMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public DlScript save(DlScript script) {
        if (script.getId() != null) {
            DlScript existing = scriptMapper.selectById(script.getId());
            if (existing == null) {
                throw new ResourceNotFoundException("脚本");
            }
            createScriptVersion(existing);
            script.setCreatedAt(existing.getCreatedAt());
            script.setUpdatedAt(LocalDateTime.now());
            scriptMapper.updateById(script);
        } else {
            script.setCreatedAt(LocalDateTime.now());
            script.setUpdatedAt(LocalDateTime.now());
            // 新建脚本默认：每天凌晨2点执行，失败告警，重试1次
            if (script.getScheduleCron() == null) script.setScheduleCron("0 0 2 * * ?");
            if (script.getWarningType() == null) script.setWarningType("FAILURE");
            if (script.getRetryTimes() == null) script.setRetryTimes(1);
            if (script.getRetryInterval() == null) script.setRetryInterval(60);
            if (script.getTimeoutSeconds() == null) script.setTimeoutSeconds(3600);
            scriptMapper.insert(script);
        }
        return script;
    }

    public void updateBasicInfo(Long id, Map<String, String> body) {
        DlScript script = new DlScript();
        script.setId(id);
        if (body.containsKey("taskType")) script.setTaskType(body.get("taskType"));
        if (body.containsKey("modelDesc")) script.setModelDesc(body.get("modelDesc"));
        if (body.containsKey("subject")) script.setSubject(body.get("subject"));
        script.setUpdatedAt(LocalDateTime.now());
        scriptMapper.updateById(script);
    }

    public void updateDatabaseName(Long id, String databaseName) {
        DlScript script = new DlScript();
        script.setId(id);
        script.setDatabaseName(databaseName);
        script.setUpdatedAt(LocalDateTime.now());
        scriptMapper.updateById(script);
    }

    public void delete(Long id) {
        scriptMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveScript(Long id, Long targetFolderId) {
        DlScript script = scriptMapper.selectById(id);
        if (script == null) throw new ResourceNotFoundException("脚本");
        DlScriptFolder targetFolder = folderMapper.selectById(targetFolderId);
        if (targetFolder == null) throw new ResourceNotFoundException("目标文件夹");
        DlScript update = new DlScript();
        update.setId(id);
        update.setFolderId(targetFolderId);
        update.setUpdatedAt(LocalDateTime.now());
        scriptMapper.updateById(update);
    }

    public List<Map<String, Object>> allWithContent() {
        List<DlScript> scripts = scriptMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlScript s : scripts) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getScriptName());
            m.put("content", s.getContent());
            m.put("scheduleStatus", s.getScheduleStatus());
            m.put("type", "script");
            result.add(m);
        }
        // 同步任务也加入，让前端依赖分析能匹配到 ODS 任务
        List<DlSyncTask> syncTasks = syncTaskMapper.selectList(null);
        for (DlSyncTask t : syncTasks) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getTargetTable());
            m.put("content", null);
            m.put("scheduleStatus", t.getScheduleStatus());
            m.put("type", "syncTask");
            result.add(m);
        }
        return result;
    }

    // ========== 版本管理 ==========

    public List<DlScriptVersion> listVersions(Long scriptId) {
        QueryWrapper<DlScriptVersion> qw = new QueryWrapper<>();
        qw.eq("script_id", scriptId)
          .orderByDesc("committed_at", "id")
          .last("LIMIT 10");
        return scriptVersionMapper.selectList(qw);
    }

    private void createScriptVersion(DlScript script) {
        String content = script.getContent() == null ? "" : script.getContent();
        if (content.trim().isEmpty()) return;

        QueryWrapper<DlScriptVersion> latestQuery = new QueryWrapper<>();
        latestQuery.eq("script_id", script.getId())
                .orderByDesc("version", "id")
                .last("LIMIT 1");
        DlScriptVersion latestVersion = scriptVersionMapper.selectOne(latestQuery);

        DlScriptVersion version = new DlScriptVersion();
        version.setScriptId(script.getId());
        version.setVersion(latestVersion == null ? 1 : latestVersion.getVersion() + 1);
        version.setContent(content);
        version.setCommitMsg("自动保存历史版本");
        version.setCommittedBy("system");
        version.setCommittedAt(LocalDateTime.now());
        version.setVersionType("save");
        scriptVersionMapper.insert(version);

        // 清理旧版本，只保留最近 10 个 save 版本（online 版本永久保留）
        QueryWrapper<DlScriptVersion> cleanupQuery = new QueryWrapper<>();
        cleanupQuery.eq("script_id", script.getId())
                .eq("version_type", "save")
                .orderByDesc("committed_at", "id")
                .last("LIMIT 10, 1000000");
        List<DlScriptVersion> expired = scriptVersionMapper.selectList(cleanupQuery);
        for (DlScriptVersion v : expired) {
            scriptVersionMapper.deleteById(v.getId());
        }
    }

    // ========== 文件夹 CRUD ==========

    public DlScriptFolder createFolder(DlScriptFolder folder) {
        folder.setCreatedAt(LocalDateTime.now());
        folderMapper.insert(folder);
        return folder;
    }

    public void renameFolder(Long id, String name) {
        DlScriptFolder folder = folderMapper.selectById(id);
        if (folder == null) throw new ResourceNotFoundException("文件夹");
        folder.setFolderName(name);
        folderMapper.updateById(folder);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id) {
        deleteFolderRecursively(id);
    }

    private void deleteFolderRecursively(Long folderId) {
        QueryWrapper<DlScriptFolder> childQuery = new QueryWrapper<>();
        childQuery.eq("parent_id", folderId);
        List<DlScriptFolder> children = folderMapper.selectList(childQuery);
        for (DlScriptFolder child : children) {
            deleteFolderRecursively(child.getId());
        }
        QueryWrapper<DlScript> scriptQuery = new QueryWrapper<>();
        scriptQuery.eq("folder_id", folderId);
        scriptMapper.delete(scriptQuery);
        folderMapper.deleteById(folderId);
    }

    // ========== 同步任务 CRUD ==========

    public DlSyncTask saveSyncTask(DlSyncTask task) {
        if (task.getId() != null) {
            task.setUpdatedAt(LocalDateTime.now());
            syncTaskMapper.updateById(task);
        } else {
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            syncTaskMapper.insert(task);
        }
        return task;
    }

    public void deleteSyncTask(Long id) {
        syncTaskMapper.deleteById(id);
    }
}
