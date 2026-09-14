package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.exception.BusinessException;
import com.datalink.mapper.DlDictItemMapper;
import com.datalink.mapper.DlDictTypeMapper;
import com.datalink.model.DlDictItem;
import com.datalink.model.DlDictType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据字典服务 — 字典类型和字典项的增删改查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final DlDictTypeMapper dictTypeMapper;
    private final DlDictItemMapper dictItemMapper;

    // ========== 字典类型 ==========

    public List<DlDictType> listTypes(String keyword) {
        QueryWrapper<DlDictType> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("dict_code", keyword)
                    .or().like("dict_name", keyword));
        }
        qw.orderByDesc("created_at");
        return dictTypeMapper.selectList(qw);
    }

    public DlDictType getTypeByCode(String dictCode) {
        return dictTypeMapper.selectOne(
                new QueryWrapper<DlDictType>().eq("dict_code", dictCode));
    }

    public DlDictType saveType(DlDictType type) {
        if (type.getDictCode() == null || type.getDictCode().isEmpty()) {
            throw new BusinessException("字典编码不能为空");
        }
        if (type.getDictName() == null || type.getDictName().isEmpty()) {
            throw new BusinessException("字典名称不能为空");
        }

        DlDictType existing = getTypeByCode(type.getDictCode());
        if (existing != null && (type.getId() == null || !existing.getId().equals(type.getId()))) {
            throw new BusinessException("字典编码已存在: " + type.getDictCode());
        }

        if (type.getId() != null) {
            type.setUpdatedAt(LocalDateTime.now());
            dictTypeMapper.updateById(type);
        } else {
            type.setStatus(type.getStatus() != null ? type.getStatus() : 1);
            type.setCreatedAt(LocalDateTime.now());
            type.setUpdatedAt(LocalDateTime.now());
            dictTypeMapper.insert(type);
        }
        return type;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        DlDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException("字典类型不存在");
        }
        // 同时删除该类型下的所有字典项
        dictItemMapper.delete(new QueryWrapper<DlDictItem>().eq("dict_code", type.getDictCode()));
        dictTypeMapper.deleteById(id);
    }

    // ========== 字典项 ==========

    public List<DlDictItem> listItems(String dictCode) {
        QueryWrapper<DlDictItem> qw = new QueryWrapper<>();
        qw.eq("dict_code", dictCode)
          .eq("status", 1)
          .orderByAsc("sort_order")
          .orderByAsc("id");
        return dictItemMapper.selectList(qw);
    }

    public List<DlDictItem> listAllItems(String dictCode) {
        QueryWrapper<DlDictItem> qw = new QueryWrapper<>();
        qw.eq("dict_code", dictCode)
          .orderByAsc("sort_order")
          .orderByAsc("id");
        return dictItemMapper.selectList(qw);
    }

    public DlDictItem saveItem(DlDictItem item) {
        if (item.getDictCode() == null || item.getDictCode().isEmpty()) {
            throw new BusinessException("字典编码不能为空");
        }
        if (item.getItemLabel() == null || item.getItemLabel().isEmpty()) {
            throw new BusinessException("字典项名称不能为空");
        }
        if (item.getItemValue() == null || item.getItemValue().isEmpty()) {
            throw new BusinessException("字典项值不能为空");
        }

        // 校验字典类型存在
        if (getTypeByCode(item.getDictCode()) == null) {
            throw new BusinessException("字典类型不存在: " + item.getDictCode());
        }

        // 校验同类型下值唯一
        QueryWrapper<DlDictItem> dupQw = new QueryWrapper<>();
        dupQw.eq("dict_code", item.getDictCode()).eq("item_value", item.getItemValue());
        if (item.getId() != null) {
            dupQw.ne("id", item.getId());
        }
        if (dictItemMapper.selectCount(dupQw) > 0) {
            throw new BusinessException("字典项值已存在: " + item.getItemValue());
        }

        if (item.getId() != null) {
            item.setUpdatedAt(LocalDateTime.now());
            dictItemMapper.updateById(item);
        } else {
            item.setStatus(item.getStatus() != null ? item.getStatus() : 1);
            item.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            dictItemMapper.insert(item);
        }
        return item;
    }

    public void deleteItem(Long id) {
        DlDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("字典项不存在");
        }
        dictItemMapper.deleteById(id);
    }

    /**
     * 批量获取多个字典类型的字典项（用于前端一次性加载所有字典）
     */
    public Map<String, List<DlDictItem>> batchGetItems(List<String> dictCodes) {
        Map<String, List<DlDictItem>> result = new LinkedHashMap<>();
        for (String code : dictCodes) {
            result.put(code, listItems(code));
        }
        return result;
    }

    /**
     * 获取所有字典类型及其启用的字典项（完整字典树）
     */
    public List<Map<String, Object>> getDictTree() {
        List<DlDictType> types = listTypes(null);
        return types.stream().map(type -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("dictCode", type.getDictCode());
            node.put("dictName", type.getDictName());
            node.put("status", type.getStatus());
            node.put("items", listItems(type.getDictCode()));
            return node;
        }).collect(Collectors.toList());
    }
}