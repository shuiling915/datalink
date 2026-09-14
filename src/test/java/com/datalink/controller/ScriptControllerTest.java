package com.datalink.controller;

import com.datalink.model.*;
import com.datalink.service.ScriptService;
import com.datalink.model.dto.MoveScriptRequest;
import com.datalink.model.dto.RenameFolderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptControllerTest {

    @Mock
    private ScriptService scriptService;

    @InjectMocks
    private ScriptController controller;

    @Test
    void tree_returnsOk() {
        Map<String, Object> node = new HashMap<>();
        node.put("id", 1L);
        node.put("name", "DWD 层");
        when(scriptService.getTree()).thenReturn(Collections.singletonList(node));
        R<List<Map<String, Object>>> r = controller.tree();
        assertEquals(0, r.getCode());
        assertEquals("DWD 层", r.getData().get(0).get("name"));
    }

    @Test
    void getById_returnsScript() {
        DlScript script = new DlScript();
        script.setId(2L);
        script.setScriptName("test");
        when(scriptService.getById(2L)).thenReturn(script);
        R<DlScript> r = controller.getById(2L);
        assertEquals(0, r.getCode());
        assertEquals("test", r.getData().getScriptName());
    }

    @Test
    void save_returnsOk() {
        DlScript saved = new DlScript();
        saved.setId(10L);
        when(scriptService.save(any(DlScript.class))).thenReturn(saved);
        R<DlScript> r = controller.save(new DlScript());
        assertEquals(0, r.getCode());
        assertEquals(10L, r.getData().getId());
    }

    @Test
    void listVersions_returnsOk() {
        DlScriptVersion v = new DlScriptVersion();
        v.setVersion(1);
        when(scriptService.listVersions(2L)).thenReturn(Collections.singletonList(v));
        R<List<DlScriptVersion>> r = controller.listVersions(2L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void deleteScript_callsService() {
        R<String> r = controller.delete(999L);
        assertEquals(0, r.getCode());
        verify(scriptService).delete(999L);
    }

    @Test
    void createFolder_returnsOk() {
        DlScriptFolder folder = new DlScriptFolder();
        folder.setId(10L);
        folder.setFolderName("新文件夹");
        when(scriptService.createFolder(any(DlScriptFolder.class))).thenReturn(folder);
        R<DlScriptFolder> r = controller.createFolder(new DlScriptFolder());
        assertEquals(0, r.getCode());
    }

    @Test
    void saveSyncTask_returnsOk() {
        DlSyncTask task = new DlSyncTask();
        task.setId(1L);
        task.setTaskName("ods_test_df");
        when(scriptService.saveSyncTask(any(DlSyncTask.class))).thenReturn(task);
        R<DlSyncTask> r = controller.saveSyncTask(new DlSyncTask());
        assertEquals(0, r.getCode());
        assertEquals("ods_test_df", r.getData().getTaskName());
    }

    @Test
    void deleteSyncTask_callsService() {
        R<String> r = controller.deleteSyncTask(1L);
        assertEquals(0, r.getCode());
        verify(scriptService).deleteSyncTask(1L);
    }

    @Test
    void allWithContent_returnsOk() {
        when(scriptService.allWithContent()).thenReturn(Collections.emptyList());
        R<List<Map<String, Object>>> r = controller.allWithContent();
        assertEquals(0, r.getCode());
    }
}
