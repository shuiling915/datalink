package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlDatasourceMapper;
import com.datalink.mapper.DlExportTaskMapper;
import com.datalink.model.DlDatasource;
import com.datalink.model.DlExportTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskService {

    private final DlExportTaskMapper exportTaskMapper;
    private final DlDatasourceMapper datasourceMapper;
    private final DatasourceConnectionFactory connectionFactory;

    private static final String EXPORT_DIR = System.getProperty("java.io.tmpdir") + "/datalink-exports";

    public DlExportTask createTask(String taskName, Long datasourceId, String databaseName,
                                    String sql, String fileName, String operator) {
        DlExportTask task = new DlExportTask();
        task.setTaskName(taskName != null ? taskName : "导出任务-" + System.currentTimeMillis());
        task.setDatasourceId(datasourceId);
        task.setDatabaseName(databaseName);
        task.setSqlText(sql);
        task.setFileName(fileName != null ? fileName : "export_" + System.currentTimeMillis() + ".csv");
        task.setStatus("PENDING");
        task.setCreatedBy(operator);
        task.setCreatedAt(LocalDateTime.now());
        exportTaskMapper.insert(task);

        executeAsync(task.getId());
        return task;
    }

    @Async
    public void executeAsync(Long taskId) {
        DlExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) return;

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);

        try {
            DlDatasource ds = datasourceMapper.selectById(task.getDatasourceId());
            if (ds == null) throw new IllegalArgumentException("数据源不存在");

            Path dir = Paths.get(EXPORT_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String filePath = EXPORT_DIR + "/" + task.getId() + "_" + task.getFileName();
            long rowCount = 0;

            try (Connection conn = connectionFactory.getConnection(ds);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(task.getSqlText());
                 BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {

                writer.write('\uFEFF');

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                StringBuilder header = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) header.append(",");
                    header.append(escapeCsv(meta.getColumnName(i)));
                }
                writer.write(header.toString());
                writer.newLine();

                while (rs.next()) {
                    StringBuilder row = new StringBuilder();
                    for (int i = 1; i <= colCount; i++) {
                        if (i > 1) row.append(",");
                        Object val = rs.getObject(i);
                        row.append(escapeCsv(val != null ? val.toString() : ""));
                    }
                    writer.write(row.toString());
                    writer.newLine();
                    rowCount++;
                }
            }

            File file = new File(filePath);
            task.setFilePath(filePath);
            task.setFileSize(file.length());
            task.setRowCount(rowCount);
            task.setStatus("SUCCESS");
            task.setFinishedAt(LocalDateTime.now());
            task.setExpiredAt(LocalDateTime.now().plusHours(24));
            exportTaskMapper.updateById(task);
            log.info("导出任务完成: taskId={}, rows={}, size={}", taskId, rowCount, file.length());

        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);
        }
    }

    public Page<DlExportTask> listTasks(int page, int size, String status, String operator) {
        QueryWrapper<DlExportTask> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        if (operator != null && !operator.isEmpty()) qw.eq("created_by", operator);
        qw.orderByDesc("created_at");
        return exportTaskMapper.selectPage(new Page<>(page, size), qw);
    }

    public DlExportTask getTask(Long id) {
        return exportTaskMapper.selectById(id);
    }

    public byte[] downloadFile(Long id) throws IOException {
        DlExportTask task = exportTaskMapper.selectById(id);
        if (task == null) throw new FileNotFoundException("任务不存在");
        if (!"SUCCESS".equals(task.getStatus())) throw new IllegalStateException("任务未完成");
        if (task.getExpiredAt() != null && task.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("文件已过期");
        }
        Path path = Paths.get(task.getFilePath());
        if (!Files.exists(path)) throw new FileNotFoundException("文件不存在");
        return Files.readAllBytes(path);
    }

    public boolean deleteTask(Long id) {
        DlExportTask task = exportTaskMapper.selectById(id);
        if (task == null) return false;
        if (task.getFilePath() != null) {
            try { Files.deleteIfExists(Paths.get(task.getFilePath())); } catch (Exception ignored) {}
        }
        exportTaskMapper.deleteById(id);
        return true;
    }

    public void cleanupExpired() {
        QueryWrapper<DlExportTask> qw = new QueryWrapper<>();
        qw.lt("expired_at", LocalDateTime.now()).isNotNull("expired_at");
        List<DlExportTask> expired = exportTaskMapper.selectList(qw);
        for (DlExportTask t : expired) {
            if (t.getFilePath() != null) {
                try { Files.deleteIfExists(Paths.get(t.getFilePath())); } catch (Exception ignored) {}
            }
            exportTaskMapper.deleteById(t.getId());
        }
        log.info("清理过期导出任务: {} 个", expired.size());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}