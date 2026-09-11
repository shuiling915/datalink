package com.datanote.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datanote.mapper.*;
import com.datanote.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataModelingService {

    @Autowired private DnDataDomainMapper dataDomainMapper;
    @Autowired private DnBusinessProcessMapper businessProcessMapper;
    @Autowired private DnWordRootMapper wordRootMapper;
    @Autowired private DnModifierMapper modifierMapper;
    @Autowired private DnTimePeriodMapper timePeriodMapper;
    @Autowired private DnDimensionMapper dimensionMapper;
    @Autowired private DnDimensionFieldMapper dimensionFieldMapper;
    @Autowired private DnFactTableMapper factTableMapper;
    @Autowired private DnFactFieldMapper factFieldMapper;
    @Autowired private DnSummaryTableMapper summaryTableMapper;
    @Autowired private DnSummaryFieldMapper summaryFieldMapper;
    @Autowired private DnModelPublishHistoryMapper publishHistoryMapper;
    @Autowired private HiveService hiveService;

    // ==================== 数据域 ====================

    public Page<DnDataDomain> listDataDomains(int pageNum, int pageSize, String keyword) {
        Page<DnDataDomain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnDataDomain> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnDataDomain::getDomainName, keyword).or().like(DnDataDomain::getDomainCode, keyword);
        }
        wrapper.orderByDesc(DnDataDomain::getCreatedAt);
        return dataDomainMapper.selectPage(page, wrapper);
    }

    public DnDataDomain getDataDomain(Long id) {
        return dataDomainMapper.selectById(id);
    }

    @Transactional
    public DnDataDomain createDataDomain(DnDataDomain domain) {
        domain.setCreatedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());
        if (domain.getStatus() == null) domain.setStatus(1);
        dataDomainMapper.insert(domain);
        return domain;
    }

    @Transactional
    public void updateDataDomain(DnDataDomain domain) {
        domain.setUpdatedAt(LocalDateTime.now());
        dataDomainMapper.updateById(domain);
    }

    @Transactional
    public void deleteDataDomain(Long id) {
        dataDomainMapper.deleteById(id);
    }

    public List<DnDataDomain> listAllDataDomains() {
        return dataDomainMapper.selectList(new LambdaQueryWrapper<DnDataDomain>().eq(DnDataDomain::getStatus, 1).orderByAsc(DnDataDomain::getDomainCode));
    }

    // ==================== 业务过程 ====================

    public Page<DnBusinessProcess> listBusinessProcesses(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DnBusinessProcess> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnBusinessProcess> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DnBusinessProcess::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnBusinessProcess::getProcessName, keyword).or().like(DnBusinessProcess::getProcessCode, keyword);
        }
        wrapper.orderByDesc(DnBusinessProcess::getCreatedAt);
        return businessProcessMapper.selectPage(page, wrapper);
    }

    public DnBusinessProcess getBusinessProcess(Long id) {
        return businessProcessMapper.selectById(id);
    }

    @Transactional
    public DnBusinessProcess createBusinessProcess(DnBusinessProcess process) {
        process.setCreatedAt(LocalDateTime.now());
        process.setUpdatedAt(LocalDateTime.now());
        if (process.getStatus() == null) process.setStatus(1);
        businessProcessMapper.insert(process);
        return process;
    }

    @Transactional
    public void updateBusinessProcess(DnBusinessProcess process) {
        process.setUpdatedAt(LocalDateTime.now());
        businessProcessMapper.updateById(process);
    }

    @Transactional
    public void deleteBusinessProcess(Long id) {
        businessProcessMapper.deleteById(id);
    }

    // ==================== 词根 ====================

    public Page<DnWordRoot> listWordRoots(int pageNum, int pageSize, String keyword) {
        Page<DnWordRoot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnWordRoot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnWordRoot::getWordName, keyword).or().like(DnWordRoot::getWordCode, keyword);
        }
        wrapper.orderByDesc(DnWordRoot::getCreatedAt);
        return wordRootMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DnWordRoot createWordRoot(DnWordRoot wordRoot) {
        wordRoot.setCreatedAt(LocalDateTime.now());
        wordRoot.setUpdatedAt(LocalDateTime.now());
        if (wordRoot.getStatus() == null) wordRoot.setStatus(1);
        wordRootMapper.insert(wordRoot);
        return wordRoot;
    }

    @Transactional
    public void updateWordRoot(DnWordRoot wordRoot) {
        wordRoot.setUpdatedAt(LocalDateTime.now());
        wordRootMapper.updateById(wordRoot);
    }

    @Transactional
    public void deleteWordRoot(Long id) {
        wordRootMapper.deleteById(id);
    }

    // ==================== 修饰词 ====================

    public Page<DnModifier> listModifiers(int pageNum, int pageSize, String keyword) {
        Page<DnModifier> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnModifier> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnModifier::getModifierName, keyword).or().like(DnModifier::getModifierCode, keyword);
        }
        wrapper.orderByDesc(DnModifier::getCreatedAt);
        return modifierMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DnModifier createModifier(DnModifier modifier) {
        modifier.setCreatedAt(LocalDateTime.now());
        modifier.setUpdatedAt(LocalDateTime.now());
        if (modifier.getStatus() == null) modifier.setStatus(1);
        modifierMapper.insert(modifier);
        return modifier;
    }

    @Transactional
    public void updateModifier(DnModifier modifier) {
        modifier.setUpdatedAt(LocalDateTime.now());
        modifierMapper.updateById(modifier);
    }

    @Transactional
    public void deleteModifier(Long id) {
        modifierMapper.deleteById(id);
    }

    // ==================== 时间周期 ====================

    public Page<DnTimePeriod> listTimePeriods(int pageNum, int pageSize, String keyword) {
        Page<DnTimePeriod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnTimePeriod> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnTimePeriod::getPeriodName, keyword).or().like(DnTimePeriod::getPeriodCode, keyword);
        }
        wrapper.orderByDesc(DnTimePeriod::getCreatedAt);
        return timePeriodMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DnTimePeriod createTimePeriod(DnTimePeriod period) {
        period.setCreatedAt(LocalDateTime.now());
        period.setUpdatedAt(LocalDateTime.now());
        if (period.getStatus() == null) period.setStatus(1);
        timePeriodMapper.insert(period);
        return period;
    }

    @Transactional
    public void updateTimePeriod(DnTimePeriod period) {
        period.setUpdatedAt(LocalDateTime.now());
        timePeriodMapper.updateById(period);
    }

    @Transactional
    public void deleteTimePeriod(Long id) {
        timePeriodMapper.deleteById(id);
    }

    // ==================== 维度表 ====================

    public Page<DnDimension> listDimensions(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DnDimension> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnDimension> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DnDimension::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnDimension::getDimName, keyword).or().like(DnDimension::getDimCode, keyword);
        }
        wrapper.orderByDesc(DnDimension::getCreatedAt);
        return dimensionMapper.selectPage(page, wrapper);
    }

    public DnDimension getDimension(Long id) {
        return dimensionMapper.selectById(id);
    }

    public List<DnDimensionField> getDimensionFields(Long dimId) {
        return dimensionFieldMapper.selectList(new LambdaQueryWrapper<DnDimensionField>().eq(DnDimensionField::getDimId, dimId).orderByAsc(DnDimensionField::getSortOrder));
    }

    @Transactional
    public DnDimension createDimension(DnDimension dimension) {
        dimension.setCreatedAt(LocalDateTime.now());
        dimension.setUpdatedAt(LocalDateTime.now());
        if (dimension.getStatus() == null) dimension.setStatus("draft");
        if (dimension.getLayer() == null) dimension.setLayer("DIM");
        if (dimension.getPublishVersion() == null) dimension.setPublishVersion(0);
        dimensionMapper.insert(dimension);
        return dimension;
    }

    @Transactional
    public void updateDimension(DnDimension dimension) {
        dimension.setUpdatedAt(LocalDateTime.now());
        dimensionMapper.updateById(dimension);
    }

    @Transactional
    public void deleteDimension(Long id) {
        dimensionFieldMapper.delete(new LambdaQueryWrapper<DnDimensionField>().eq(DnDimensionField::getDimId, id));
        dimensionMapper.deleteById(id);
    }

    @Transactional
    public void saveDimensionFields(Long dimId, List<DnDimensionField> fields) {
        dimensionFieldMapper.delete(new LambdaQueryWrapper<DnDimensionField>().eq(DnDimensionField::getDimId, dimId));
        for (int i = 0; i < fields.size(); i++) {
            DnDimensionField field = fields.get(i);
            field.setDimId(dimId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            dimensionFieldMapper.insert(field);
        }
    }

    public String generateDimensionDDL(DnDimension dimension) {
        List<DnDimensionField> fields = getDimensionFields(dimension.getId());
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS dim.").append(dimension.getDimCode()).append(" (\n");
        for (int i = 0; i < fields.size(); i++) {
            DnDimensionField f = fields.get(i);
            if (f.getIsPartition() != null && f.getIsPartition() == 1) continue;
            ddl.append("    ").append(f.getFieldName()).append(" ").append(f.getFieldType());
            if (f.getFieldComment() != null && !f.getFieldComment().isEmpty()) {
                ddl.append(" COMMENT '").append(f.getFieldComment()).append("'");
            }
            if (i < fields.size() - 1) ddl.append(",");
            ddl.append("\n");
        }
        ddl.append(") COMMENT '").append(dimension.getDescription() != null ? dimension.getDescription() : dimension.getDimName()).append("'\n");
        ddl.append("PARTITIONED BY (dt STRING COMMENT '日期分区')\n");
        ddl.append("STORED AS ORC\n");
        ddl.append("TBLPROPERTIES ('transactional'='false');");
        return ddl.toString();
    }

    @Transactional
    public DnDimension publishDimension(Long id, String publishedBy) {
        DnDimension dimension = dimensionMapper.selectById(id);
        if (dimension == null) throw new RuntimeException("维度表不存在");
        String ddl = generateDimensionDDL(dimension);
        int newVersion = dimension.getPublishVersion() + 1;
        try {
            hiveService.executeDDL(ddl);
            dimension.setStatus("published");
            dimension.setPublishVersion(newVersion);
            dimension.setPublishedBy(publishedBy);
            dimension.setPublishedAt(LocalDateTime.now());
            dimension.setUpdatedAt(LocalDateTime.now());
            dimensionMapper.updateById(dimension);
            savePublishHistory("dimension", id, dimension.getDimCode(), dimension.getDimName(), newVersion, ddl, "success", "发布成功", publishedBy);
        } catch (Exception e) {
            savePublishHistory("dimension", id, dimension.getDimCode(), dimension.getDimName(), newVersion, ddl, "failed", e.getMessage(), publishedBy);
            throw new RuntimeException("发布失败: " + e.getMessage());
        }
        return dimension;
    }

    // ==================== 事实表 ====================

    public Page<DnFactTable> listFactTables(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DnFactTable> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnFactTable> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DnFactTable::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnFactTable::getFactName, keyword).or().like(DnFactTable::getFactCode, keyword);
        }
        wrapper.orderByDesc(DnFactTable::getCreatedAt);
        return factTableMapper.selectPage(page, wrapper);
    }

    public DnFactTable getFactTable(Long id) {
        return factTableMapper.selectById(id);
    }

    public List<DnFactField> getFactFields(Long factId) {
        return factFieldMapper.selectList(new LambdaQueryWrapper<DnFactField>().eq(DnFactField::getFactId, factId).orderByAsc(DnFactField::getSortOrder));
    }

    @Transactional
    public DnFactTable createFactTable(DnFactTable factTable) {
        factTable.setCreatedAt(LocalDateTime.now());
        factTable.setUpdatedAt(LocalDateTime.now());
        if (factTable.getStatus() == null) factTable.setStatus("draft");
        if (factTable.getLayer() == null) factTable.setLayer("DWD");
        if (factTable.getPublishVersion() == null) factTable.setPublishVersion(0);
        factTableMapper.insert(factTable);
        return factTable;
    }

    @Transactional
    public void updateFactTable(DnFactTable factTable) {
        factTable.setUpdatedAt(LocalDateTime.now());
        factTableMapper.updateById(factTable);
    }

    @Transactional
    public void deleteFactTable(Long id) {
        factFieldMapper.delete(new LambdaQueryWrapper<DnFactField>().eq(DnFactField::getFactId, id));
        factTableMapper.deleteById(id);
    }

    @Transactional
    public void saveFactFields(Long factId, List<DnFactField> fields) {
        factFieldMapper.delete(new LambdaQueryWrapper<DnFactField>().eq(DnFactField::getFactId, factId));
        for (int i = 0; i < fields.size(); i++) {
            DnFactField field = fields.get(i);
            field.setFactId(factId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            factFieldMapper.insert(field);
        }
    }

    public String generateFactTableDDL(DnFactTable factTable) {
        List<DnFactField> fields = getFactFields(factTable.getId());
        StringBuilder ddl = new StringBuilder();
        String layerPrefix = "dwd";
        if ("DWS".equals(factTable.getLayer())) layerPrefix = "dws";
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS ").append(layerPrefix).append(".").append(factTable.getFactCode()).append(" (\n");
        List<DnFactField> partitionFields = new java.util.ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            DnFactField f = fields.get(i);
            if (f.getIsPartition() != null && f.getIsPartition() == 1) {
                partitionFields.add(f);
                continue;
            }
            ddl.append("    ").append(f.getFieldName()).append(" ").append(f.getFieldType());
            if (f.getFieldComment() != null && !f.getFieldComment().isEmpty()) {
                ddl.append(" COMMENT '").append(f.getFieldComment()).append("'");
            }
            ddl.append(",\n");
        }
        if (!partitionFields.isEmpty()) {
            ddl.deleteCharAt(ddl.length() - 2);
            ddl.append(")\nCOMMENT '").append(factTable.getDescription() != null ? factTable.getDescription() : factTable.getFactName()).append("'\n");
            ddl.append("PARTITIONED BY (");
            for (int i = 0; i < partitionFields.size(); i++) {
                DnFactField pf = partitionFields.get(i);
                ddl.append(pf.getFieldName()).append(" ").append(pf.getFieldType());
                if (pf.getFieldComment() != null && !pf.getFieldComment().isEmpty()) {
                    ddl.append(" COMMENT '").append(pf.getFieldComment()).append("'");
                }
                if (i < partitionFields.size() - 1) ddl.append(", ");
            }
            ddl.append(")\n");
        } else {
            ddl.deleteCharAt(ddl.length() - 2);
            ddl.append(")\nCOMMENT '").append(factTable.getDescription() != null ? factTable.getDescription() : factTable.getFactName()).append("'\n");
            ddl.append("PARTITIONED BY (dt STRING COMMENT '日期分区')\n");
        }
        ddl.append("STORED AS ORC\n");
        ddl.append("TBLPROPERTIES ('transactional'='false');");
        return ddl.toString();
    }

    @Transactional
    public DnFactTable publishFactTable(Long id, String publishedBy) {
        DnFactTable factTable = factTableMapper.selectById(id);
        if (factTable == null) throw new RuntimeException("事实表不存在");
        String ddl = generateFactTableDDL(factTable);
        int newVersion = factTable.getPublishVersion() + 1;
        try {
            hiveService.executeDDL(ddl);
            factTable.setStatus("published");
            factTable.setPublishVersion(newVersion);
            factTable.setPublishedBy(publishedBy);
            factTable.setPublishedAt(LocalDateTime.now());
            factTable.setUpdatedAt(LocalDateTime.now());
            factTableMapper.updateById(factTable);
            savePublishHistory("fact", id, factTable.getFactCode(), factTable.getFactName(), newVersion, ddl, "success", "发布成功", publishedBy);
        } catch (Exception e) {
            savePublishHistory("fact", id, factTable.getFactCode(), factTable.getFactName(), newVersion, ddl, "failed", e.getMessage(), publishedBy);
            throw new RuntimeException("发布失败: " + e.getMessage());
        }
        return factTable;
    }

    // ==================== 汇总表 ====================

    public Page<DnSummaryTable> listSummaryTables(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DnSummaryTable> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnSummaryTable> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DnSummaryTable::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DnSummaryTable::getSummaryName, keyword).or().like(DnSummaryTable::getSummaryCode, keyword);
        }
        wrapper.orderByDesc(DnSummaryTable::getCreatedAt);
        return summaryTableMapper.selectPage(page, wrapper);
    }

    public DnSummaryTable getSummaryTable(Long id) {
        return summaryTableMapper.selectById(id);
    }

    public List<DnSummaryField> getSummaryFields(Long summaryId) {
        return summaryFieldMapper.selectList(new LambdaQueryWrapper<DnSummaryField>().eq(DnSummaryField::getSummaryId, summaryId).orderByAsc(DnSummaryField::getSortOrder));
    }

    @Transactional
    public DnSummaryTable createSummaryTable(DnSummaryTable summaryTable) {
        summaryTable.setCreatedAt(LocalDateTime.now());
        summaryTable.setUpdatedAt(LocalDateTime.now());
        if (summaryTable.getStatus() == null) summaryTable.setStatus("draft");
        if (summaryTable.getLayer() == null) summaryTable.setLayer("DWS");
        if (summaryTable.getPublishVersion() == null) summaryTable.setPublishVersion(0);
        summaryTableMapper.insert(summaryTable);
        return summaryTable;
    }

    @Transactional
    public void updateSummaryTable(DnSummaryTable summaryTable) {
        summaryTable.setUpdatedAt(LocalDateTime.now());
        summaryTableMapper.updateById(summaryTable);
    }

    @Transactional
    public void deleteSummaryTable(Long id) {
        summaryFieldMapper.delete(new LambdaQueryWrapper<DnSummaryField>().eq(DnSummaryField::getSummaryId, id));
        summaryTableMapper.deleteById(id);
    }

    @Transactional
    public void saveSummaryFields(Long summaryId, List<DnSummaryField> fields) {
        summaryFieldMapper.delete(new LambdaQueryWrapper<DnSummaryField>().eq(DnSummaryField::getSummaryId, summaryId));
        for (int i = 0; i < fields.size(); i++) {
            DnSummaryField field = fields.get(i);
            field.setSummaryId(summaryId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            summaryFieldMapper.insert(field);
        }
    }

    public String generateSummaryTableDDL(DnSummaryTable summaryTable) {
        List<DnSummaryField> fields = getSummaryFields(summaryTable.getId());
        StringBuilder ddl = new StringBuilder();
        String layerPrefix = summaryTable.getLayer().toLowerCase();
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS ").append(layerPrefix).append(".").append(summaryTable.getSummaryCode()).append(" (\n");
        for (int i = 0; i < fields.size(); i++) {
            DnSummaryField f = fields.get(i);
            if (f.getIsPartition() != null && f.getIsPartition() == 1) continue;
            ddl.append("    ").append(f.getFieldName()).append(" ").append(f.getFieldType());
            if (f.getFieldComment() != null && !f.getFieldComment().isEmpty()) {
                ddl.append(" COMMENT '").append(f.getFieldComment()).append("'");
            }
            if (i < fields.size() - 1) ddl.append(",");
            ddl.append("\n");
        }
        ddl.append(") COMMENT '").append(summaryTable.getDescription() != null ? summaryTable.getDescription() : summaryTable.getSummaryName()).append("'\n");
        ddl.append("PARTITIONED BY (dt STRING COMMENT '日期分区')\n");
        ddl.append("STORED AS ORC\n");
        ddl.append("TBLPROPERTIES ('transactional'='false');");
        return ddl.toString();
    }

    @Transactional
    public DnSummaryTable publishSummaryTable(Long id, String publishedBy) {
        DnSummaryTable summaryTable = summaryTableMapper.selectById(id);
        if (summaryTable == null) throw new RuntimeException("汇总表不存在");
        String ddl = generateSummaryTableDDL(summaryTable);
        int newVersion = summaryTable.getPublishVersion() + 1;
        try {
            hiveService.executeDDL(ddl);
            summaryTable.setStatus("published");
            summaryTable.setPublishVersion(newVersion);
            summaryTable.setPublishedBy(publishedBy);
            summaryTable.setPublishedAt(LocalDateTime.now());
            summaryTable.setUpdatedAt(LocalDateTime.now());
            summaryTableMapper.updateById(summaryTable);
            savePublishHistory("summary", id, summaryTable.getSummaryCode(), summaryTable.getSummaryName(), newVersion, ddl, "success", "发布成功", publishedBy);
        } catch (Exception e) {
            savePublishHistory("summary", id, summaryTable.getSummaryCode(), summaryTable.getSummaryName(), newVersion, ddl, "failed", e.getMessage(), publishedBy);
            throw new RuntimeException("发布失败: " + e.getMessage());
        }
        return summaryTable;
    }

    // ==================== 发布历史 ====================

    private void savePublishHistory(String modelType, Long modelId, String modelCode, String modelName, int version, String ddl, String status, String msg, String publishedBy) {
        DnModelPublishHistory history = new DnModelPublishHistory();
        history.setModelType(modelType);
        history.setModelId(modelId);
        history.setModelCode(modelCode);
        history.setModelName(modelName);
        history.setVersion(version);
        history.setDdlContent(ddl);
        history.setPublishStatus(status);
        history.setPublishMsg(msg);
        history.setPublishedBy(publishedBy);
        history.setPublishedAt(LocalDateTime.now());
        publishHistoryMapper.insert(history);
    }

    public Page<DnModelPublishHistory> listPublishHistory(int pageNum, int pageSize, String modelType, Long modelId) {
        Page<DnModelPublishHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DnModelPublishHistory> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null && !modelType.isEmpty()) wrapper.eq(DnModelPublishHistory::getModelType, modelType);
        if (modelId != null) wrapper.eq(DnModelPublishHistory::getModelId, modelId);
        wrapper.orderByDesc(DnModelPublishHistory::getPublishedAt);
        return publishHistoryMapper.selectPage(page, wrapper);
    }
}