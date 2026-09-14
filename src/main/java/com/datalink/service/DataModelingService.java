package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.*;
import com.datalink.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataModelingService {

    @Autowired private DlDataDomainMapper dataDomainMapper;
    @Autowired private DlBusinessProcessMapper businessProcessMapper;
    @Autowired private DlWordRootMapper wordRootMapper;
    @Autowired private DlModifierMapper modifierMapper;
    @Autowired private DlTimePeriodMapper timePeriodMapper;
    @Autowired private DlDimensionMapper dimensionMapper;
    @Autowired private DlDimensionFieldMapper dimensionFieldMapper;
    @Autowired private DlFactTableMapper factTableMapper;
    @Autowired private DlFactFieldMapper factFieldMapper;
    @Autowired private DlSummaryTableMapper summaryTableMapper;
    @Autowired private DlSummaryFieldMapper summaryFieldMapper;
    @Autowired private DlModelPublishHistoryMapper publishHistoryMapper;
    @Autowired private HiveService hiveService;

    // ==================== 数据域 ====================

    public Page<DlDataDomain> listDataDomains(int pageNum, int pageSize, String keyword) {
        Page<DlDataDomain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlDataDomain> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlDataDomain::getDomainName, keyword).or().like(DlDataDomain::getDomainCode, keyword);
        }
        wrapper.orderByDesc(DlDataDomain::getCreatedAt);
        return dataDomainMapper.selectPage(page, wrapper);
    }

    public DlDataDomain getDataDomain(Long id) {
        return dataDomainMapper.selectById(id);
    }

    @Transactional
    public DlDataDomain createDataDomain(DlDataDomain domain) {
        domain.setCreatedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());
        if (domain.getStatus() == null) domain.setStatus(1);
        dataDomainMapper.insert(domain);
        return domain;
    }

    @Transactional
    public void updateDataDomain(DlDataDomain domain) {
        domain.setUpdatedAt(LocalDateTime.now());
        dataDomainMapper.updateById(domain);
    }

    @Transactional
    public void deleteDataDomain(Long id) {
        dataDomainMapper.deleteById(id);
    }

    public List<DlDataDomain> listAllDataDomains() {
        return dataDomainMapper.selectList(new LambdaQueryWrapper<DlDataDomain>().eq(DlDataDomain::getStatus, 1).orderByAsc(DlDataDomain::getDomainCode));
    }

    // ==================== 业务过程 ====================

    public Page<DlBusinessProcess> listBusinessProcesses(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DlBusinessProcess> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlBusinessProcess> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DlBusinessProcess::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlBusinessProcess::getProcessName, keyword).or().like(DlBusinessProcess::getProcessCode, keyword);
        }
        wrapper.orderByDesc(DlBusinessProcess::getCreatedAt);
        return businessProcessMapper.selectPage(page, wrapper);
    }

    public DlBusinessProcess getBusinessProcess(Long id) {
        return businessProcessMapper.selectById(id);
    }

    @Transactional
    public DlBusinessProcess createBusinessProcess(DlBusinessProcess process) {
        process.setCreatedAt(LocalDateTime.now());
        process.setUpdatedAt(LocalDateTime.now());
        if (process.getStatus() == null) process.setStatus(1);
        businessProcessMapper.insert(process);
        return process;
    }

    @Transactional
    public void updateBusinessProcess(DlBusinessProcess process) {
        process.setUpdatedAt(LocalDateTime.now());
        businessProcessMapper.updateById(process);
    }

    @Transactional
    public void deleteBusinessProcess(Long id) {
        businessProcessMapper.deleteById(id);
    }

    // ==================== 词根 ====================

    public Page<DlWordRoot> listWordRoots(int pageNum, int pageSize, String keyword) {
        Page<DlWordRoot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlWordRoot> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlWordRoot::getWordName, keyword).or().like(DlWordRoot::getWordCode, keyword);
        }
        wrapper.orderByDesc(DlWordRoot::getCreatedAt);
        return wordRootMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DlWordRoot createWordRoot(DlWordRoot wordRoot) {
        wordRoot.setCreatedAt(LocalDateTime.now());
        wordRoot.setUpdatedAt(LocalDateTime.now());
        if (wordRoot.getStatus() == null) wordRoot.setStatus(1);
        wordRootMapper.insert(wordRoot);
        return wordRoot;
    }

    @Transactional
    public void updateWordRoot(DlWordRoot wordRoot) {
        wordRoot.setUpdatedAt(LocalDateTime.now());
        wordRootMapper.updateById(wordRoot);
    }

    @Transactional
    public void deleteWordRoot(Long id) {
        wordRootMapper.deleteById(id);
    }

    // ==================== 修饰词 ====================

    public Page<DlModifier> listModifiers(int pageNum, int pageSize, String keyword) {
        Page<DlModifier> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlModifier> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlModifier::getModifierName, keyword).or().like(DlModifier::getModifierCode, keyword);
        }
        wrapper.orderByDesc(DlModifier::getCreatedAt);
        return modifierMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DlModifier createModifier(DlModifier modifier) {
        modifier.setCreatedAt(LocalDateTime.now());
        modifier.setUpdatedAt(LocalDateTime.now());
        if (modifier.getStatus() == null) modifier.setStatus(1);
        modifierMapper.insert(modifier);
        return modifier;
    }

    @Transactional
    public void updateModifier(DlModifier modifier) {
        modifier.setUpdatedAt(LocalDateTime.now());
        modifierMapper.updateById(modifier);
    }

    @Transactional
    public void deleteModifier(Long id) {
        modifierMapper.deleteById(id);
    }

    // ==================== 时间周期 ====================

    public Page<DlTimePeriod> listTimePeriods(int pageNum, int pageSize, String keyword) {
        Page<DlTimePeriod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlTimePeriod> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlTimePeriod::getPeriodName, keyword).or().like(DlTimePeriod::getPeriodCode, keyword);
        }
        wrapper.orderByDesc(DlTimePeriod::getCreatedAt);
        return timePeriodMapper.selectPage(page, wrapper);
    }

    @Transactional
    public DlTimePeriod createTimePeriod(DlTimePeriod period) {
        period.setCreatedAt(LocalDateTime.now());
        period.setUpdatedAt(LocalDateTime.now());
        if (period.getStatus() == null) period.setStatus(1);
        timePeriodMapper.insert(period);
        return period;
    }

    @Transactional
    public void updateTimePeriod(DlTimePeriod period) {
        period.setUpdatedAt(LocalDateTime.now());
        timePeriodMapper.updateById(period);
    }

    @Transactional
    public void deleteTimePeriod(Long id) {
        timePeriodMapper.deleteById(id);
    }

    // ==================== 维度表 ====================

    public Page<DlDimension> listDimensions(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DlDimension> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlDimension> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DlDimension::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlDimension::getDimName, keyword).or().like(DlDimension::getDimCode, keyword);
        }
        wrapper.orderByDesc(DlDimension::getCreatedAt);
        return dimensionMapper.selectPage(page, wrapper);
    }

    public DlDimension getDimension(Long id) {
        return dimensionMapper.selectById(id);
    }

    public List<DlDimensionField> getDimensionFields(Long dimId) {
        return dimensionFieldMapper.selectList(new LambdaQueryWrapper<DlDimensionField>().eq(DlDimensionField::getDimId, dimId).orderByAsc(DlDimensionField::getSortOrder));
    }

    @Transactional
    public DlDimension createDimension(DlDimension dimension) {
        dimension.setCreatedAt(LocalDateTime.now());
        dimension.setUpdatedAt(LocalDateTime.now());
        if (dimension.getStatus() == null) dimension.setStatus("draft");
        if (dimension.getLayer() == null) dimension.setLayer("DIM");
        if (dimension.getPublishVersion() == null) dimension.setPublishVersion(0);
        dimensionMapper.insert(dimension);
        return dimension;
    }

    @Transactional
    public void updateDimension(DlDimension dimension) {
        dimension.setUpdatedAt(LocalDateTime.now());
        dimensionMapper.updateById(dimension);
    }

    @Transactional
    public void deleteDimension(Long id) {
        dimensionFieldMapper.delete(new LambdaQueryWrapper<DlDimensionField>().eq(DlDimensionField::getDimId, id));
        dimensionMapper.deleteById(id);
    }

    @Transactional
    public void saveDimensionFields(Long dimId, List<DlDimensionField> fields) {
        dimensionFieldMapper.delete(new LambdaQueryWrapper<DlDimensionField>().eq(DlDimensionField::getDimId, dimId));
        for (int i = 0; i < fields.size(); i++) {
            DlDimensionField field = fields.get(i);
            field.setDimId(dimId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            dimensionFieldMapper.insert(field);
        }
    }

    public String generateDimensionDDL(DlDimension dimension) {
        List<DlDimensionField> fields = getDimensionFields(dimension.getId());
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS dim.").append(dimension.getDimCode()).append(" (\n");
        for (int i = 0; i < fields.size(); i++) {
            DlDimensionField f = fields.get(i);
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
    public DlDimension publishDimension(Long id, String publishedBy) {
        DlDimension dimension = dimensionMapper.selectById(id);
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

    public Page<DlFactTable> listFactTables(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DlFactTable> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlFactTable> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DlFactTable::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlFactTable::getFactName, keyword).or().like(DlFactTable::getFactCode, keyword);
        }
        wrapper.orderByDesc(DlFactTable::getCreatedAt);
        return factTableMapper.selectPage(page, wrapper);
    }

    public DlFactTable getFactTable(Long id) {
        return factTableMapper.selectById(id);
    }

    public List<DlFactField> getFactFields(Long factId) {
        return factFieldMapper.selectList(new LambdaQueryWrapper<DlFactField>().eq(DlFactField::getFactId, factId).orderByAsc(DlFactField::getSortOrder));
    }

    @Transactional
    public DlFactTable createFactTable(DlFactTable factTable) {
        factTable.setCreatedAt(LocalDateTime.now());
        factTable.setUpdatedAt(LocalDateTime.now());
        if (factTable.getStatus() == null) factTable.setStatus("draft");
        if (factTable.getLayer() == null) factTable.setLayer("DWD");
        if (factTable.getPublishVersion() == null) factTable.setPublishVersion(0);
        factTableMapper.insert(factTable);
        return factTable;
    }

    @Transactional
    public void updateFactTable(DlFactTable factTable) {
        factTable.setUpdatedAt(LocalDateTime.now());
        factTableMapper.updateById(factTable);
    }

    @Transactional
    public void deleteFactTable(Long id) {
        factFieldMapper.delete(new LambdaQueryWrapper<DlFactField>().eq(DlFactField::getFactId, id));
        factTableMapper.deleteById(id);
    }

    @Transactional
    public void saveFactFields(Long factId, List<DlFactField> fields) {
        factFieldMapper.delete(new LambdaQueryWrapper<DlFactField>().eq(DlFactField::getFactId, factId));
        for (int i = 0; i < fields.size(); i++) {
            DlFactField field = fields.get(i);
            field.setFactId(factId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            factFieldMapper.insert(field);
        }
    }

    public String generateFactTableDDL(DlFactTable factTable) {
        List<DlFactField> fields = getFactFields(factTable.getId());
        StringBuilder ddl = new StringBuilder();
        String layerPrefix = "dwd";
        if ("DWS".equals(factTable.getLayer())) layerPrefix = "dws";
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS ").append(layerPrefix).append(".").append(factTable.getFactCode()).append(" (\n");
        List<DlFactField> partitionFields = new java.util.ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            DlFactField f = fields.get(i);
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
                DlFactField pf = partitionFields.get(i);
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
    public DlFactTable publishFactTable(Long id, String publishedBy) {
        DlFactTable factTable = factTableMapper.selectById(id);
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

    public Page<DlSummaryTable> listSummaryTables(int pageNum, int pageSize, Long domainId, String keyword) {
        Page<DlSummaryTable> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlSummaryTable> wrapper = new LambdaQueryWrapper<>();
        if (domainId != null) wrapper.eq(DlSummaryTable::getDomainId, domainId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DlSummaryTable::getSummaryName, keyword).or().like(DlSummaryTable::getSummaryCode, keyword);
        }
        wrapper.orderByDesc(DlSummaryTable::getCreatedAt);
        return summaryTableMapper.selectPage(page, wrapper);
    }

    public DlSummaryTable getSummaryTable(Long id) {
        return summaryTableMapper.selectById(id);
    }

    public List<DlSummaryField> getSummaryFields(Long summaryId) {
        return summaryFieldMapper.selectList(new LambdaQueryWrapper<DlSummaryField>().eq(DlSummaryField::getSummaryId, summaryId).orderByAsc(DlSummaryField::getSortOrder));
    }

    @Transactional
    public DlSummaryTable createSummaryTable(DlSummaryTable summaryTable) {
        summaryTable.setCreatedAt(LocalDateTime.now());
        summaryTable.setUpdatedAt(LocalDateTime.now());
        if (summaryTable.getStatus() == null) summaryTable.setStatus("draft");
        if (summaryTable.getLayer() == null) summaryTable.setLayer("DWS");
        if (summaryTable.getPublishVersion() == null) summaryTable.setPublishVersion(0);
        summaryTableMapper.insert(summaryTable);
        return summaryTable;
    }

    @Transactional
    public void updateSummaryTable(DlSummaryTable summaryTable) {
        summaryTable.setUpdatedAt(LocalDateTime.now());
        summaryTableMapper.updateById(summaryTable);
    }

    @Transactional
    public void deleteSummaryTable(Long id) {
        summaryFieldMapper.delete(new LambdaQueryWrapper<DlSummaryField>().eq(DlSummaryField::getSummaryId, id));
        summaryTableMapper.deleteById(id);
    }

    @Transactional
    public void saveSummaryFields(Long summaryId, List<DlSummaryField> fields) {
        summaryFieldMapper.delete(new LambdaQueryWrapper<DlSummaryField>().eq(DlSummaryField::getSummaryId, summaryId));
        for (int i = 0; i < fields.size(); i++) {
            DlSummaryField field = fields.get(i);
            field.setSummaryId(summaryId);
            field.setSortOrder(i);
            field.setCreatedAt(LocalDateTime.now());
            summaryFieldMapper.insert(field);
        }
    }

    public String generateSummaryTableDDL(DlSummaryTable summaryTable) {
        List<DlSummaryField> fields = getSummaryFields(summaryTable.getId());
        StringBuilder ddl = new StringBuilder();
        String layerPrefix = summaryTable.getLayer().toLowerCase();
        ddl.append("CREATE EXTERNAL TABLE IF NOT EXISTS ").append(layerPrefix).append(".").append(summaryTable.getSummaryCode()).append(" (\n");
        for (int i = 0; i < fields.size(); i++) {
            DlSummaryField f = fields.get(i);
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
    public DlSummaryTable publishSummaryTable(Long id, String publishedBy) {
        DlSummaryTable summaryTable = summaryTableMapper.selectById(id);
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
        DlModelPublishHistory history = new DlModelPublishHistory();
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

    public Page<DlModelPublishHistory> listPublishHistory(int pageNum, int pageSize, String modelType, Long modelId) {
        Page<DlModelPublishHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DlModelPublishHistory> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null && !modelType.isEmpty()) wrapper.eq(DlModelPublishHistory::getModelType, modelType);
        if (modelId != null) wrapper.eq(DlModelPublishHistory::getModelId, modelId);
        wrapper.orderByDesc(DlModelPublishHistory::getPublishedAt);
        return publishHistoryMapper.selectPage(page, wrapper);
    }
}