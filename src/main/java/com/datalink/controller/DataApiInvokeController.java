package com.datalink.controller;

import com.datalink.service.DataApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据服务 API 开放调用 Controller
 * 路径: /api/open/{apiCode}
 * 鉴权方式: Header X-API-Key（API配置了apiKey时必填）
 */
@Slf4j
@Tag(name = "数据服务API调用")
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class DataApiInvokeController {

    private final DataApiService dataApiService;

    @Operation(summary = "调用数据API(GET)")
    @GetMapping("/{apiCode}")
    public Map<String, Object> invokeGet(@PathVariable String apiCode,
                                         @RequestParam Map<String, String> params,
                                         HttpServletRequest request) {
        return invoke(apiCode, params, request);
    }

    @Operation(summary = "调用数据API(POST)")
    @PostMapping("/{apiCode}")
    public Map<String, Object> invokePost(@PathVariable String apiCode,
                                          @RequestBody(required = false) Map<String, String> body,
                                          HttpServletRequest request) {
        Map<String, String> params = body != null ? body : new HashMap<>();
        // POST 也支持 query 参数
        Map<String, String> queryParams = new HashMap<>(request.getParameterMap().size());
        request.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0) queryParams.put(k, v[0]);
        });
        params.putAll(queryParams);
        return invoke(apiCode, params, request);
    }

    private Map<String, Object> invoke(String apiCode, Map<String, String> params,
                                       HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        String caller = request.getHeader("X-Caller");
        if (caller == null || caller.isEmpty()) {
            caller = request.getRemoteAddr();
        }

        // 将 Header 中的 API Key 放入参数供 Service 验证
        if (params == null) params = new HashMap<>();
        if (apiKey != null && !apiKey.isEmpty()) {
            params.put("_apiKey", apiKey);
        }

        try {
            Map<String, Object> result = dataApiService.invoke(apiCode, params, caller);
            return result;
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return Map.of("success", false, "message", e.getMessage());
        } catch (Exception e) {
            log.error("API调用异常: {}", apiCode, e);
            return Map.of("success", false, "message", "服务异常: " + e.getMessage());
        }
    }
}