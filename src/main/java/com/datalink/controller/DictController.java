package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlDictItem;
import com.datalink.model.DlDictType;
import com.datalink.model.R;
import com.datalink.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据字典 Controller — 字典类型和字典项管理
 */
@Tag(name = "数据字典")
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ========== 字典类型 ==========

    @Operation(summary = "字典类型列表")
    @GetMapping("/types")
    public R<List<DlDictType>> listTypes(@RequestParam(required = false) String keyword) {
        return R.ok(dictService.listTypes(keyword));
    }

    @Operation(summary = "保存字典类型")
    @PostMapping("/type/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "保存字典类型")
    public R<DlDictType> saveType(@RequestBody DlDictType type) {
        return R.ok(dictService.saveType(type));
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "删除字典类型")
    public R<String> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return R.ok("删除成功");
    }

    // ========== 字典项 ==========

    @Operation(summary = "字典项列表（仅启用）")
    @GetMapping("/items/{dictCode}")
    public R<List<DlDictItem>> listItems(@PathVariable String dictCode) {
        return R.ok(dictService.listItems(dictCode));
    }

    @Operation(summary = "字典项列表（全部，含禁用）")
    @GetMapping("/items/{dictCode}/all")
    public R<List<DlDictItem>> listAllItems(@PathVariable String dictCode) {
        return R.ok(dictService.listAllItems(dictCode));
    }

    @Operation(summary = "保存字典项")
    @PostMapping("/item/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "保存字典项")
    public R<DlDictItem> saveItem(@RequestBody DlDictItem item) {
        return R.ok(dictService.saveItem(item));
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/item/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "系统管理", operation = "删除字典项")
    public R<String> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return R.ok("删除成功");
    }

    // ========== 批量查询 ==========

    @Operation(summary = "批量获取字典项")
    @PostMapping("/batch")
    public R<Map<String, List<DlDictItem>>> batchGetItems(@RequestBody List<String> dictCodes) {
        return R.ok(dictService.batchGetItems(dictCodes));
    }

    @Operation(summary = "获取完整字典树")
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getDictTree() {
        return R.ok(dictService.getDictTree());
    }
}