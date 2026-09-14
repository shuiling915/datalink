package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlPromptMapper;
import com.datalink.model.DlPrompt;
import com.datalink.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final DlPromptMapper promptMapper;

    @GetMapping
    public R<List<DlPrompt>> list() {
        return R.ok(promptMapper.selectList(new QueryWrapper<DlPrompt>().orderByAsc("id")));
    }

    @GetMapping("/{id}")
    public R<DlPrompt> get(@PathVariable Long id) {
        return R.ok(promptMapper.selectById(id));
    }

    @PostMapping
    public R<DlPrompt> create(@RequestBody DlPrompt body) {
        if (body.getCode() == null || body.getCode().trim().isEmpty()) return R.fail("code 不能为空");
        if (body.getName() == null || body.getName().trim().isEmpty()) return R.fail("名称不能为空");
        if (body.getContent() == null || body.getContent().trim().isEmpty()) return R.fail("提示词内容不能为空");
        promptMapper.insert(body);
        return R.ok(body);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody DlPrompt body) {
        body.setId(id);
        promptMapper.updateById(body);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        promptMapper.deleteById(id);
        return R.ok();
    }
}
