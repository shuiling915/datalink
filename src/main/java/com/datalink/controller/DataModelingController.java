package com.datalink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.model.*;
import com.datalink.model.R;
import com.datalink.service.DataModelingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datamodeling")
public class DataModelingController {

    @Autowired
    private DataModelingService dataModelingService;

    // ==================== 数据域 ====================

    @GetMapping("/domains")
    public R listDomains(@RequestParam(defaultValue = "1") int pageNum,
                         @RequestParam(defaultValue = "20") int pageSize,
                         @RequestParam(required = false) String keyword) {
        Page<DlDataDomain> page = dataModelingService.listDataDomains(pageNum, pageSize, keyword);
        return R.ok(page);
    }

    @GetMapping("/domains/all")
    public R listAllDomains() {
        return R.ok(dataModelingService.listAllDataDomains());
    }

    @GetMapping("/domains/{id}")
    public R getDomain(@PathVariable Long id) {
        return R.ok(dataModelingService.getDataDomain(id));
    }

    @PostMapping("/domains")
    public R createDomain(@RequestBody DlDataDomain domain) {
        return R.ok(dataModelingService.createDataDomain(domain));
    }

    @PutMapping("/domains/{id}")
    public R updateDomain(@PathVariable Long id, @RequestBody DlDataDomain domain) {
        domain.setId(id);
        dataModelingService.updateDataDomain(domain);
        return R.ok();
    }

    @DeleteMapping("/domains/{id}")
    public R deleteDomain(@PathVariable Long id) {
        dataModelingService.deleteDataDomain(id);
        return R.ok();
    }

    // ==================== 业务过程 ====================

    @GetMapping("/processes")
    public R listProcesses(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) Long domainId,
                           @RequestParam(required = false) String keyword) {
        Page<DlBusinessProcess> page = dataModelingService.listBusinessProcesses(pageNum, pageSize, domainId, keyword);
        return R.ok(page);
    }

    @GetMapping("/processes/{id}")
    public R getProcess(@PathVariable Long id) {
        return R.ok(dataModelingService.getBusinessProcess(id));
    }

    @PostMapping("/processes")
    public R createProcess(@RequestBody DlBusinessProcess process) {
        return R.ok(dataModelingService.createBusinessProcess(process));
    }

    @PutMapping("/processes/{id}")
    public R updateProcess(@PathVariable Long id, @RequestBody DlBusinessProcess process) {
        process.setId(id);
        dataModelingService.updateBusinessProcess(process);
        return R.ok();
    }

    @DeleteMapping("/processes/{id}")
    public R deleteProcess(@PathVariable Long id) {
        dataModelingService.deleteBusinessProcess(id);
        return R.ok();
    }

    // ==================== 词根 ====================

    @GetMapping("/wordroots")
    public R listWordRoots(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) String keyword) {
        Page<DlWordRoot> page = dataModelingService.listWordRoots(pageNum, pageSize, keyword);
        return R.ok(page);
    }

    @PostMapping("/wordroots")
    public R createWordRoot(@RequestBody DlWordRoot wordRoot) {
        return R.ok(dataModelingService.createWordRoot(wordRoot));
    }

    @PutMapping("/wordroots/{id}")
    public R updateWordRoot(@PathVariable Long id, @RequestBody DlWordRoot wordRoot) {
        wordRoot.setId(id);
        dataModelingService.updateWordRoot(wordRoot);
        return R.ok();
    }

    @DeleteMapping("/wordroots/{id}")
    public R deleteWordRoot(@PathVariable Long id) {
        dataModelingService.deleteWordRoot(id);
        return R.ok();
    }

    // ==================== 修饰词 ====================

    @GetMapping("/modifiers")
    public R listModifiers(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "20") int pageSize,
                           @RequestParam(required = false) String keyword) {
        Page<DlModifier> page = dataModelingService.listModifiers(pageNum, pageSize, keyword);
        return R.ok(page);
    }

    @PostMapping("/modifiers")
    public R createModifier(@RequestBody DlModifier modifier) {
        return R.ok(dataModelingService.createModifier(modifier));
    }

    @PutMapping("/modifiers/{id}")
    public R updateModifier(@PathVariable Long id, @RequestBody DlModifier modifier) {
        modifier.setId(id);
        dataModelingService.updateModifier(modifier);
        return R.ok();
    }

    @DeleteMapping("/modifiers/{id}")
    public R deleteModifier(@PathVariable Long id) {
        dataModelingService.deleteModifier(id);
        return R.ok();
    }

    // ==================== 时间周期 ====================

    @GetMapping("/timeperiods")
    public R listTimePeriods(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "20") int pageSize,
                             @RequestParam(required = false) String keyword) {
        Page<DlTimePeriod> page = dataModelingService.listTimePeriods(pageNum, pageSize, keyword);
        return R.ok(page);
    }

    @PostMapping("/timeperiods")
    public R createTimePeriod(@RequestBody DlTimePeriod period) {
        return R.ok(dataModelingService.createTimePeriod(period));
    }

    @PutMapping("/timeperiods/{id}")
    public R updateTimePeriod(@PathVariable Long id, @RequestBody DlTimePeriod period) {
        period.setId(id);
        dataModelingService.updateTimePeriod(period);
        return R.ok();
    }

    @DeleteMapping("/timeperiods/{id}")
    public R deleteTimePeriod(@PathVariable Long id) {
        dataModelingService.deleteTimePeriod(id);
        return R.ok();
    }

    // ==================== 维度表 ====================

    @GetMapping("/dimensions")
    public R listDimensions(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "20") int pageSize,
                            @RequestParam(required = false) Long domainId,
                            @RequestParam(required = false) String keyword) {
        Page<DlDimension> page = dataModelingService.listDimensions(pageNum, pageSize, domainId, keyword);
        return R.ok(page);
    }

    @GetMapping("/dimensions/{id}")
    public R getDimension(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        DlDimension dimension = dataModelingService.getDimension(id);
        result.put("dimension", dimension);
        result.put("fields", dataModelingService.getDimensionFields(id));
        return R.ok(result);
    }

    @PostMapping("/dimensions")
    public R createDimension(@RequestBody DlDimension dimension) {
        return R.ok(dataModelingService.createDimension(dimension));
    }

    @PutMapping("/dimensions/{id}")
    public R updateDimension(@PathVariable Long id, @RequestBody DlDimension dimension) {
        dimension.setId(id);
        dataModelingService.updateDimension(dimension);
        return R.ok();
    }

    @DeleteMapping("/dimensions/{id}")
    public R deleteDimension(@PathVariable Long id) {
        dataModelingService.deleteDimension(id);
        return R.ok();
    }

    @PostMapping("/dimensions/{id}/fields")
    public R saveDimensionFields(@PathVariable Long id, @RequestBody List<DlDimensionField> fields) {
        dataModelingService.saveDimensionFields(id, fields);
        return R.ok();
    }

    @PostMapping("/dimensions/{id}/generate-ddl")
    public R generateDimensionDDL(@PathVariable Long id) {
        DlDimension dimension = dataModelingService.getDimension(id);
        String ddl = dataModelingService.generateDimensionDDL(dimension);
        return R.ok(ddl);
    }

    @PostMapping("/dimensions/{id}/publish")
    public R publishDimension(@PathVariable Long id, @RequestParam(defaultValue = "admin") String publishedBy) {
        DlDimension dimension = dataModelingService.publishDimension(id, publishedBy);
        return R.ok(dimension);
    }

    // ==================== 事实表 ====================

    @GetMapping("/facttables")
    public R listFactTables(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "20") int pageSize,
                            @RequestParam(required = false) Long domainId,
                            @RequestParam(required = false) String keyword) {
        Page<DlFactTable> page = dataModelingService.listFactTables(pageNum, pageSize, domainId, keyword);
        return R.ok(page);
    }

    @GetMapping("/facttables/{id}")
    public R getFactTable(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        DlFactTable factTable = dataModelingService.getFactTable(id);
        result.put("factTable", factTable);
        result.put("fields", dataModelingService.getFactFields(id));
        return R.ok(result);
    }

    @PostMapping("/facttables")
    public R createFactTable(@RequestBody DlFactTable factTable) {
        return R.ok(dataModelingService.createFactTable(factTable));
    }

    @PutMapping("/facttables/{id}")
    public R updateFactTable(@PathVariable Long id, @RequestBody DlFactTable factTable) {
        factTable.setId(id);
        dataModelingService.updateFactTable(factTable);
        return R.ok();
    }

    @DeleteMapping("/facttables/{id}")
    public R deleteFactTable(@PathVariable Long id) {
        dataModelingService.deleteFactTable(id);
        return R.ok();
    }

    @PostMapping("/facttables/{id}/fields")
    public R saveFactFields(@PathVariable Long id, @RequestBody List<DlFactField> fields) {
        dataModelingService.saveFactFields(id, fields);
        return R.ok();
    }

    @PostMapping("/facttables/{id}/generate-ddl")
    public R generateFactTableDDL(@PathVariable Long id) {
        DlFactTable factTable = dataModelingService.getFactTable(id);
        String ddl = dataModelingService.generateFactTableDDL(factTable);
        return R.ok(ddl);
    }

    @PostMapping("/facttables/{id}/publish")
    public R publishFactTable(@PathVariable Long id, @RequestParam(defaultValue = "admin") String publishedBy) {
        DlFactTable factTable = dataModelingService.publishFactTable(id, publishedBy);
        return R.ok(factTable);
    }

    // ==================== 汇总表 ====================

    @GetMapping("/summarytables")
    public R listSummaryTables(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "20") int pageSize,
                               @RequestParam(required = false) Long domainId,
                               @RequestParam(required = false) String keyword) {
        Page<DlSummaryTable> page = dataModelingService.listSummaryTables(pageNum, pageSize, domainId, keyword);
        return R.ok(page);
    }

    @GetMapping("/summarytables/{id}")
    public R getSummaryTable(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        DlSummaryTable summaryTable = dataModelingService.getSummaryTable(id);
        result.put("summaryTable", summaryTable);
        result.put("fields", dataModelingService.getSummaryFields(id));
        return R.ok(result);
    }

    @PostMapping("/summarytables")
    public R createSummaryTable(@RequestBody DlSummaryTable summaryTable) {
        return R.ok(dataModelingService.createSummaryTable(summaryTable));
    }

    @PutMapping("/summarytables/{id}")
    public R updateSummaryTable(@PathVariable Long id, @RequestBody DlSummaryTable summaryTable) {
        summaryTable.setId(id);
        dataModelingService.updateSummaryTable(summaryTable);
        return R.ok();
    }

    @DeleteMapping("/summarytables/{id}")
    public R deleteSummaryTable(@PathVariable Long id) {
        dataModelingService.deleteSummaryTable(id);
        return R.ok();
    }

    @PostMapping("/summarytables/{id}/fields")
    public R saveSummaryFields(@PathVariable Long id, @RequestBody List<DlSummaryField> fields) {
        dataModelingService.saveSummaryFields(id, fields);
        return R.ok();
    }

    @PostMapping("/summarytables/{id}/generate-ddl")
    public R generateSummaryTableDDL(@PathVariable Long id) {
        DlSummaryTable summaryTable = dataModelingService.getSummaryTable(id);
        String ddl = dataModelingService.generateSummaryTableDDL(summaryTable);
        return R.ok(ddl);
    }

    @PostMapping("/summarytables/{id}/publish")
    public R publishSummaryTable(@PathVariable Long id, @RequestParam(defaultValue = "admin") String publishedBy) {
        DlSummaryTable summaryTable = dataModelingService.publishSummaryTable(id, publishedBy);
        return R.ok(summaryTable);
    }

    // ==================== 发布历史 ====================

    @GetMapping("/publish-history")
    public R listPublishHistory(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(required = false) String modelType,
                                @RequestParam(required = false) Long modelId) {
        Page<DlModelPublishHistory> page = dataModelingService.listPublishHistory(pageNum, pageSize, modelType, modelId);
        return R.ok(page);
    }
}