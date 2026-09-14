package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.model.dto.BiChartRequest;
import com.datalink.service.AiAssistService;
import com.datalink.service.DataMapService;
import com.datalink.service.HiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 智能助手", description = "自然语言图表、报告、对话")
@RequiredArgsConstructor
public class BiAiController {

    private final AiAssistService aiAssistService;
    private final HiveService hiveService;
    private final DataMapService dataMapService;

    /** 会话历史：sessionId → 最近 N 轮对话，ConcurrentHashMap + 定时清理 */
    private static final ConcurrentHashMap<String, List<Map<String, String>>> SESSIONS = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 10;           // 每会话最多保留 10 轮
    private static final long SESSION_TTL_MINUTES = 30;  // 30 分钟过期

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            SESSIONS.keySet().removeIf(k -> {
                List<Map<String, String>> h = SESSIONS.get(k);
                if (h == null || h.isEmpty()) return true;
                String lastTime = null;
                for (Map<String, String> m : h) {
                    lastTime = m.get("_ts");
                }
                if (lastTime != null) {
                    try {
                        long ts = Long.parseLong(lastTime);
                        return System.currentTimeMillis() - ts > SESSION_TTL_MINUTES * 60_000;
                    } catch (NumberFormatException ignored) {}
                }
                return false;
            });
        }, 5, 5, TimeUnit.MINUTES);
    }

    @Operation(summary = "智能助手（图表/报告/聊天统一入口）")
    @PostMapping("/bi-chart")
    public R<Map<String, Object>> agent(@RequestBody BiChartRequest body) {
        String message = body.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return R.fail("请输入描述信息");
        }
        String sessionId = body.getSessionId() != null ? body.getSessionId() : UUID.randomUUID().toString();

        long t0 = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);

        try {
            // Step 1: 意图分类
            AiAssistService.Intent intent = aiAssistService.classifyIntent(message);
            log.info("意图分类: {} → {}", message.length() > 50 ? message.substring(0, 50) + "..." : message, intent);

            // Step 2: 获取表结构（CHART/REPORT 需要）
            String tableSchemas = null;
            if (intent == AiAssistService.Intent.CHART || intent == AiAssistService.Intent.REPORT) {
                tableSchemas = buildTableSchemas();
            }

            // Step 3: 按意图路由
            switch (intent) {
                case CHART:
                    handleChart(message, tableSchemas, result);
                    break;
                case REPORT:
                    handleReport(message, tableSchemas, result);
                    break;
                default:
                    handleChat(message, sessionId, result);
                    break;
            }

            // Step 4: 记录会话历史
            saveToSession(sessionId, "user", message);
            String replyText = result.containsKey("explanation") ? (String) result.get("explanation")
                    : result.containsKey("content") ? (String) result.get("content")
                    : result.containsKey("reply") ? (String) result.get("reply") : "";
            if (!replyText.isEmpty()) {
                saveToSession(sessionId, "assistant", replyText);
            }

        } catch (Exception e) {
            log.error("Agent 执行失败", e);
            result.put("type", "error");
            result.put("error", "执行失败: " + e.getMessage());
        }

        result.put("duration", System.currentTimeMillis() - t0);
        return R.ok(result);
    }

    // ======================== CHART 图表处理 ========================

    private void handleChart(String message, String tableSchemas, Map<String, Object> result) {
        if (tableSchemas == null || tableSchemas.isEmpty()) {
            result.put("type", "chat");
            result.put("reply", "未找到可用的数据表，请先确认 Hive 连接状态。");
            return;
        }

        // LLM 调用带重试（最多 2 次）
        String llmResponse = retryCall(() -> aiAssistService.generateBiChart(message, tableSchemas), 2, "BI图表生成");
        Map<String, Object> parsed = parseLlmJson(llmResponse);

        if (parsed == null || !parsed.containsKey("sql")) {
            result.put("type", "chat");
            result.put("reply", llmResponse != null && llmResponse.contains("error") ? llmResponse : "AI 返回的格式异常，请重试");
            result.put("rawResponse", llmResponse);
            return;
        }

        String sql = (String) parsed.getOrDefault("sql", "");
        String chartType = (String) parsed.getOrDefault("chartType", "bar");
        String title = (String) parsed.getOrDefault("title", "数据图表");
        String explanation = (String) parsed.getOrDefault("explanation", "");

        result.put("type", "chart");
        result.put("sql", sql);
        result.put("chartType", chartType);
        result.put("title", title);
        result.put("explanation", explanation);

        if (sql.isEmpty()) {
            result.put("type", "chat");
            result.put("reply", explanation.isEmpty() ? "AI 无法为当前需求生成 SQL，请换个问法。" : explanation);
            return;
        }

        // 执行 SQL（单次，Hive 查询通常较慢不再重试）
        try {
            Map<String, Object> queryResult = hiveService.executeSQL(sql);
            result.put("success", queryResult.getOrDefault("success", false));
            result.put("queryDuration", queryResult.getOrDefault("duration", 0));

            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) queryResult.get("columns");
            @SuppressWarnings("unchecked")
            List<List<String>> rows = (List<List<String>>) queryResult.get("rows");

            if (columns != null && rows != null && !rows.isEmpty()) {
                result.put("columns", columns);
                result.put("data", rows);
                result.put("rowCount", rows.size());

                Map<String, Object> chartOption = buildEChartsOption(columns, rows, chartType, title,
                        (String) parsed.get("xAxis"), (String) parsed.get("yAxis"));
                result.put("chartOption", chartOption);
            } else {
                result.put("hint", "查询无数据返回，SQL 语法或数据可能不符预期");
            }
        } catch (Exception e) {
            log.error("SQL 执行失败: {}", e.getMessage());
            result.put("hint", "SQL 执行失败: " + e.getMessage());
        }
    }

    // ======================== REPORT 报告处理 ========================

    private void handleReport(String message, String tableSchemas, Map<String, Object> result) {
        if (tableSchemas == null || tableSchemas.isEmpty()) {
            result.put("type", "chat");
            result.put("reply", "未找到可用的数据表，无法生成报告。");
            return;
        }

        String report = retryCall(() -> aiAssistService.generateReport(message, tableSchemas), 2, "报告生成");
        result.put("type", "report");
        result.put("content", report);
        result.put("format", "markdown");
    }

    // ======================== CHAT 对话处理 ========================

    private void handleChat(String message, String sessionId, Map<String, Object> result) {
        List<Map<String, String>> history = SESSIONS.getOrDefault(sessionId, new ArrayList<>());
        StringBuilder context = new StringBuilder();
        int ctxStart = Math.max(0, history.size() - 6);  // 最近 3 轮（6 条消息）
        for (int i = ctxStart; i < history.size(); i++) {
            Map<String, String> h = history.get(i);
            context.append(h.get("role")).append(": ").append(h.get("content")).append("\n");
        }

        String reply = aiAssistService.chat(message, context.toString());
        result.put("type", "chat");
        result.put("reply", reply);
    }

    // ======================== 会话管理 ========================

    private void saveToSession(String sessionId, String role, String content) {
        if (role == null || content == null || content.trim().isEmpty()) return;
        Map<String, String> entry = new HashMap<>();
        entry.put("role", role);
        entry.put("content", content);
        entry.put("_ts", String.valueOf(System.currentTimeMillis()));
        SESSIONS.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(entry);
        // 裁剪
        List<Map<String, String>> h = SESSIONS.get(sessionId);
        while (h.size() > MAX_HISTORY * 2) {
            h.remove(0);
        }
    }

    // ======================== 重试工具 ========================

    private <T> T retryCall(java.util.function.Supplier<T> call, int maxRetries, String label) {
        Exception lastEx = null;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                return call.get();
            } catch (Exception e) {
                lastEx = e;
                if (i < maxRetries) {
                    long waitMs = (long) Math.pow(2, i) * 500;  // 500ms, 1000ms
                    log.warn("{} 第{}次失败，{}ms后重试: {}", label, i + 1, waitMs, e.getMessage());
                    try { Thread.sleep(waitMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new RuntimeException(label + " 重试 " + maxRetries + " 次后仍失败", lastEx);
    }

    @Operation(summary = "注入测试数据")
    @PostMapping("/seed-data")
    public R<Map<String, Object>> seedTestData() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            StringBuilder batchSQL = new StringBuilder();
            batchSQL.append("SET hive.exec.dynamic.partition.mode=nonstrict;\n");
            batchSQL.append("SET hive.stats.autogather=false;\n");

            int orderCount = 0;
            String[][] orders = {
                {"ORD20260901001","1001","399.00","0.00","10.00","409.00","1002","1","paid","张三","13800138001","广东","深圳","南山区","科技园1号","快点发","APP","2026-09-10 09:00:00","2026-09-10 09:05:00",""},
                {"ORD20260902001","1002","1299.00","100.00","0.00","1199.00","1002","2","paid","李四","13800138002","浙江","杭州","西湖区","文三路2号","","PC","2026-09-10 10:00:00","2026-09-10 10:10:00",""},
                {"ORD20260903001","1003","89.00","10.00","8.00","87.00","1002","1","paid","王五","13800138003","北京","北京","朝阳区","望京3号","周末送","APP","2026-09-10 11:00:00","2026-09-10 11:02:00",""},
                {"ORD20260904001","1001","2599.00","200.00","0.00","2399.00","1001","2","paid","赵六","13800138004","上海","上海","浦东新区","张江4号","","APP","2026-09-10 12:00:00","2026-09-10 12:15:00",""},
                {"ORD20260905001","1004","599.00","50.00","15.00","564.00","1001","1","paid","孙七","13800138005","江苏","南京","鼓楼区","新模范5号","","PC","2026-09-10 13:00:00","2026-09-10 13:08:00",""},
                {"ORD20260906001","1005","199.00","20.00","0.00","179.00","1003","1","paid","周八","13800138006","四川","成都","高新区","天府6号","","APP","2026-09-10 14:00:00","2026-09-10 14:03:00",""},
                {"ORD20260907001","1001","799.00","0.00","20.00","819.00","1002","1","unpaid","吴九","13800138007","广东","广州","天河区","珠江新城7号","","APP","2026-09-11 08:00:00","",""},
                {"ORD20260908001","1002","3299.00","300.00","0.00","2999.00","1001","2","unpaid","郑十","13800138008","浙江","宁波","海曙区","天一8号","包装好","PC","2026-09-11 09:30:00","",""},
                {"ORD20260909001","1003","149.00","0.00","6.00","155.00","1002","1","paid","冯十一","13800138009","北京","北京","海淀区","中关村9号","","APP","2026-09-11 10:00:00","2026-09-11 10:04:00",""},
                {"ORD20260910001","1004","899.00","80.00","0.00","819.00","1001","2","paid","陈十二","13800138010","上海","上海","徐汇区","漕河泾10号","","PC","2026-09-11 11:00:00","2026-09-11 11:12:00",""},
                {"ORD20260911001","1005","259.00","30.00","12.00","241.00","1003","1","paid","褚十三","13800138011","江苏","苏州","工业园区","金鸡湖11号","","APP","2026-09-11 12:00:00","2026-09-11 12:06:00",""},
                {"ORD20260912001","1001","158.00","0.00","5.00","163.00","1002","1","paid","卫十四","13800138012","四川","绵阳","涪城区","临园路12号","","APP","2026-09-11 13:00:00","2026-09-11 13:02:00",""},
                {"ORD20260913001","1002","4999.00","500.00","0.00","4499.00","1001","2","cancelled","蒋十五","13800138013","广东","东莞","南城","鸿福路13号","","PC","2026-09-12 08:00:00","2026-09-12 08:01:00","2026-09-12 09:00:00"},
                {"ORD20260914001","1003","69.00","5.00","0.00","64.00","1002","1","paid","沈十六","13800138014","浙江","温州","鹿城区","五马街14号","","APP","2026-09-12 08:30:00","2026-09-12 08:32:00",""},
                {"ORD20260915001","1004","1199.00","0.00","0.00","1199.00","1001","1","paid","韩十七","13800138015","北京","北京","丰台区","方庄15号","快点","APP","2026-09-12 09:00:00","2026-09-12 09:08:00",""},
                {"ORD20260916001","1005","349.00","40.00","10.00","319.00","1003","1","cancelled","杨十八","13800138016","上海","上海","杨浦区","五角场16号","","PC","2026-09-12 09:30:00","2026-09-12 09:31:00","2026-09-12 10:30:00"},
                {"ORD20260917001","1001","699.00","50.00","0.00","649.00","1002","2","paid","朱十九","13800138017","江苏","无锡","滨湖区","太湖大道17号","","APP","2026-09-12 10:00:00","2026-09-12 10:06:00",""},
                {"ORD20260918001","1002","1899.00","100.00","20.00","1819.00","1001","1","unpaid","秦二十","13800138018","四川","德阳","旌阳区","长江西路18号","","APP","2026-09-12 10:30:00","",""},
                {"ORD20260919001","1003","79.00","0.00","0.00","79.00","1002","1","paid","尤二一","13800138019","广东","佛山","禅城区","祖庙路19号","","PC","2026-09-12 11:00:00","2026-09-12 11:01:00",""},
                {"ORD20260920001","1004","4299.00","0.00","0.00","4299.00","1001","2","paid","许二二","13800138020","浙江","绍兴","越城区","解放路20号","","APP","2026-09-12 11:30:00","2026-09-12 11:45:00",""},
            };
            for (int i = 0; i < orders.length; i++) {
                String[] o = orders[i];
                batchSQL.append(String.format(
                    "INSERT INTO ods.ods_order_center_order_info_df PARTITION (dt='2026-09-12') " +
                    "VALUES ('%d','%s','%s','%s','%s','%s','%s','%s','%s','%s','','','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','');\n",
                    i+1, o[0],o[1],o[2],o[3],o[4],o[5],o[6],o[7],o[8],
                    o[9],o[10],o[11],o[12],o[13],o[14],o[15],o[16],o[17],o[18],
                    o.length>19?o[19]:"", o.length>19?o[19]:""));
                orderCount++;
            }

            int detailCount = 0;
            String[][] details = {
                {"1","101","SKU001","iPhone 15","img1","黑色,128GB","1","5999.00","5999.00","0.00","0.00"},
                {"1","102","SKU007","手机壳","img7","透明","1","39.00","39.00","5.00","0.00"},
                {"2","201","SKU003","AirPods Pro","img3","白色","1","1499.00","1499.00","100.00","50.00"},
                {"3","301","SKU006","充电器30W","img6","白色","1","149.00","149.00","0.00","0.00"},
                {"3","302","SKU008","数据线USB-C","img8","1米","1","89.00","89.00","0.00","0.00"},
                {"4","401","SKU002","MacBook Air","img2","银色,8GB","1","8999.00","8999.00","0.00","0.00"},
                {"4","402","SKU003","AirPods Pro","img3","白色","1","1499.00","1499.00","100.00","50.00"},
                {"4","403","SKU008","数据线USB-C","img8","1米","1","89.00","89.00","0.00","0.00"},
                {"5","501","SKU004","iPad Pro","img4","灰色,128GB","1","6799.00","6799.00","0.00","0.00"},
                {"6","601","SKU005","Apple Watch","img5","星光色,45mm","1","3199.00","3199.00","0.00","0.00"},
                {"7","701","SKU006","充电器30W","img6","白色","2","149.00","298.00","0.00","0.00"},
                {"7","702","SKU007","手机壳","img7","透明","1","39.00","39.00","5.00","0.00"},
                {"8","801","SKU001","iPhone 15","img1","黑色,128GB","1","5999.00","5999.00","0.00","0.00"},
                {"8","802","SKU005","Apple Watch","img5","星光色,45mm","1","3199.00","3199.00","0.00","0.00"},
                {"9","901","SKU002","MacBook Air","img2","银色,8GB","1","8999.00","8999.00","0.00","0.00"},
                {"10","1001","SKU003","AirPods Pro","img3","白色","1","1499.00","1499.00","100.00","50.00"},
                {"11","1101","SKU004","iPad Pro","img4","灰色,128GB","1","6799.00","6799.00","0.00","0.00"},
                {"11","1102","SKU008","数据线USB-C","img8","1米","1","89.00","89.00","0.00","0.00"},
                {"12","1201","SKU005","Apple Watch","img5","星光色,45mm","1","3199.00","3199.00","0.00","0.00"},
                {"13","1301","SKU006","充电器30W","img6","白色","3","149.00","447.00","0.00","0.00"},
                {"13","1302","SKU007","手机壳","img7","透明","1","39.00","39.00","5.00","0.00"},
                {"14","1401","SKU001","iPhone 15","img1","黑色,128GB","1","5999.00","5999.00","0.00","0.00"},
                {"15","1501","SKU002","MacBook Air","img2","银色,8GB","1","8999.00","8999.00","0.00","0.00"},
                {"16","1601","SKU003","AirPods Pro","img3","白色","1","1499.00","1499.00","100.00","50.00"},
                {"17","1701","SKU004","iPad Pro","img4","灰色,128GB","1","6799.00","6799.00","0.00","0.00"},
                {"17","1702","SKU006","充电器30W","img6","白色","1","149.00","149.00","0.00","0.00"},
                {"18","1801","SKU005","Apple Watch","img5","星光色,45mm","1","3199.00","3199.00","0.00","0.00"},
                {"19","1901","SKU006","充电器30W","img6","白色","2","149.00","298.00","0.00","0.00"},
                {"19","1902","SKU008","数据线USB-C","img8","1米","1","89.00","89.00","0.00","0.00"},
                {"20","2001","SKU001","iPhone 15","img1","黑色,128GB","1","5999.00","5999.00","0.00","0.00"},
                {"20","2002","SKU003","AirPods Pro","img3","白色","1","1499.00","1499.00","100.00","50.00"},
                {"20","2003","SKU007","手机壳","img7","透明","1","39.00","39.00","5.00","0.00"},
            };
            for (int i = 0; i < details.length; i++) {
                String[] d = details[i];
                batchSQL.append(String.format(
                    "INSERT INTO ods.ods_order_center_order_detail_df PARTITION (dt='2026-09-12') " +
                    "VALUES ('%d','%s','%s','%s','%s','img','%s','%s','%s','%s','%s','%s','2026-09-10 10:00:00');\n",
                    i+1, d[0],d[1],d[2],d[3],d[5],d[6],d[7],d[8],d[9],d[10]));
                detailCount++;
            }

            int statusCount = 0;
            String[] statusStages = {
                "1,1001,待付款,2026-09-10 09:00:00","1,1002,已付款,2026-09-10 09:05:00","1,1003,已发货,2026-09-10 09:10:00","1,1004,已完成,2026-09-10 09:15:00",
                "2,1001,待付款,2026-09-10 10:00:00","2,1002,已付款,2026-09-10 10:10:00","2,1003,已发货,2026-09-10 10:15:00","2,1004,已完成,2026-09-10 10:20:00",
                "3,1001,待付款,2026-09-10 11:00:00","3,1002,已付款,2026-09-10 11:02:00","3,1004,已完成,2026-09-10 11:07:00",
                "4,1001,待付款,2026-09-10 12:00:00","4,1002,已付款,2026-09-10 12:15:00","4,1003,已发货,2026-09-10 12:20:00","4,1004,已完成,2026-09-10 12:25:00",
                "5,1001,待付款,2026-09-10 13:00:00","5,1002,已付款,2026-09-10 13:08:00","5,1004,已完成,2026-09-10 13:13:00",
                "6,1001,待付款,2026-09-10 14:00:00","6,1002,已付款,2026-09-10 14:03:00","6,1005,已取消,2026-09-10 14:08:00",
                "7,1001,待付款,2026-09-11 08:00:00","7,1002,已付款,2026-09-11 08:05:00","7,1003,已发货,2026-09-11 08:10:00",
                "8,1001,待付款,2026-09-11 09:30:00","8,1002,已付款,2026-09-11 09:35:00","8,1003,已发货,2026-09-11 09:40:00",
                "9,1001,待付款,2026-09-11 10:00:00","9,1002,已付款,2026-09-11 10:05:00","9,1003,已发货,2026-09-11 10:10:00",
                "10,1001,待付款,2026-09-11 11:00:00","10,1002,已付款,2026-09-11 11:05:00","10,1003,已发货,2026-09-11 11:10:00",
                "11,1001,待付款,2026-09-11 12:00:00","11,1002,已付款,2026-09-11 12:05:00","11,1003,已发货,2026-09-11 12:10:00",
                "12,1001,待付款,2026-09-11 13:00:00","12,1002,已付款,2026-09-11 13:05:00","12,1003,已发货,2026-09-11 13:10:00",
                "13,1001,待付款,2026-09-12 08:00:00","13,1002,已付款,2026-09-12 08:05:00",
                "14,1001,待付款,2026-09-12 08:30:00","14,1002,已付款,2026-09-12 08:35:00",
                "15,1001,待付款,2026-09-12 09:00:00","15,1002,已付款,2026-09-12 09:05:00",
                "16,1001,待付款,2026-09-12 09:30:00","16,1002,已付款,2026-09-12 09:35:00",
                "17,1001,待付款,2026-09-12 10:00:00","17,1002,已付款,2026-09-12 10:05:00",
                "18,1001,待付款,2026-09-12 10:30:00","18,1002,已付款,2026-09-12 10:35:00",
                "19,1001,待付款,2026-09-12 11:00:00","19,1002,已付款,2026-09-12 11:05:00",
                "20,1001,待付款,2026-09-12 11:30:00","20,1002,已付款,2026-09-12 11:35:00",
            };
            for (int i = 0; i < statusStages.length; i++) {
                String[] parts = statusStages[i].split(",", 4);
                batchSQL.append(String.format(
                    "INSERT INTO ods.ods_order_center_order_status_log_df PARTITION (dt='2026-09-12') " +
                    "VALUES ('%d','%s','%s','%s','system','','%s');\n",
                    i+1, parts[0], parts[1], parts[2], parts[3]));
                statusCount++;
            }

            // 批量执行
            hiveService.executeSQLWithStream(batchSQL.toString(), new HiveService.LogCallback() {
                public void onLog(String level, String message) { log.info("[Seed] {}: {}", level, message); }
                public void onResult(Map<String, Object> result) { log.info("[Seed] Result: {}", result); }
                public void onError(String error) { log.warn("[Seed] Error: {}", error); }
            });

            stats.put("orderInfo", orderCount + "/20");
            stats.put("orderDetail", detailCount + "/32");
            stats.put("orderStatusLog", statusCount + "/55");
            stats.put("result", "done");
        } catch (Exception e) {
            log.error("种子数据注入失败", e);
            stats.put("error", e.getMessage());
        }
        return R.ok(stats);
    }

    private String buildTableSchemas() {
        try {
            List<String> dbs = dataMapService.getHiveDatabases();
            StringBuilder sb = new StringBuilder();
            int tableCount = 0;
            int maxTables = 15;

            for (String db : dbs) {
                if (tableCount >= maxTables) break;
                try {
                    List<String> tables = dataMapService.getHiveTables(db);
                    for (String table : tables) {
                        if (tableCount >= maxTables) break;
                        List<com.datalink.model.ColumnInfo> cols = dataMapService.getHiveColumns(db, table);
                        if (cols.isEmpty()) continue;
                        sb.append(db).append(".").append(table).append(" (");
                        sb.append(cols.stream()
                                .map(c -> c.getName() + ":" + (c.getType() != null ? c.getType() : "string"))
                                .collect(Collectors.joining(", ")));
                        sb.append(")\n");
                        tableCount++;
                    }
                } catch (Exception ignored) {
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取Hive表结构失败: {}", e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseLlmJson(String llmResponse) {
        if (llmResponse == null) return null;
        String json = llmResponse.trim();
        // 移除 markdown 代码块包裹
        if (json.startsWith("```")) {
            int endFence = json.indexOf('\n');
            if (endFence > 0) {
                json = json.substring(endFence + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```")).trim();
                }
            }
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("LLM JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildEChartsOption(List<String> columns, List<List<String>> rows,
                                                    String chartType, String title,
                                                    String xAxisField, String yAxisField) {
        Map<String, Object> option = new LinkedHashMap<>();

        Map<String, Object> titleMap = new LinkedHashMap<>();
        titleMap.put("text", title != null ? title : "数据图表");
        titleMap.put("left", "center");
        option.put("title", titleMap);

        Map<String, Object> tooltip = new LinkedHashMap<>();
        tooltip.put("trigger", "axis");
        option.put("tooltip", tooltip);

        if (columns == null || columns.size() < 2 || rows == null || rows.isEmpty()) {
            return option;
        }

        int xIdx = 0;
        int yIdx = 1;
        if (xAxisField != null) {
            for (int i = 0; i < columns.size(); i++) {
                if (xAxisField.equalsIgnoreCase(columns.get(i))) { xIdx = i; break; }
            }
        }
        if (yAxisField != null && xIdx != yIdx) {
            for (int i = 0; i < columns.size(); i++) {
                if (yAxisField.equalsIgnoreCase(columns.get(i))) { yIdx = i; break; }
            }
        }
        if (xIdx == yIdx && columns.size() > 2) {
            yIdx = 2;
        }

        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();
        for (List<String> row : rows) {
            xData.add(row.get(xIdx) != null ? row.get(xIdx) : "");
            String val = row.get(yIdx);
            if (val != null) {
                try {
                    yData.add(Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    yData.add(val);
                }
            } else {
                yData.add(0);
            }
        }

        Map<String, Object> xAxis = new LinkedHashMap<>();
        xAxis.put("type", "category");
        xAxis.put("data", xData);
        xAxis.put("axisLabel", Map.of("rotate", xData.size() > 6 ? 45 : 0));

        Map<String, Object> yAxis = new LinkedHashMap<>();
        yAxis.put("type", "value");
        yAxis.put("name", columns.get(yIdx));

        option.put("xAxis", xAxis);
        option.put("yAxis", yAxis);

        Map<String, Object> series = new LinkedHashMap<>();
        series.put("name", columns.get(yIdx));
        series.put("type", mapChartType(chartType));
        series.put("data", yData);
        series.put("smooth", "line".equals(chartType));

        if ("pie".equals(chartType)) {
            option.remove("xAxis");
            option.remove("yAxis");
            Map<String, Object> legend = new LinkedHashMap<>();
            legend.put("type", "scroll");
            legend.put("bottom", 10);
            option.put("legend", legend);

            List<Map<String, Object>> pieData = new ArrayList<>();
            for (int i = 0; i < xData.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", xData.get(i));
                item.put("value", yData.get(i));
                pieData.add(item);
            }
            series.put("data", pieData);
            series.put("radius", "60%");
            sortTooltip(option);
        }

        option.put("series", Collections.singletonList(series));

        Map<String, Object> grid = new LinkedHashMap<>();
        grid.put("left", "3%");
        grid.put("right", "4%");
        grid.put("bottom", "15%");
        grid.put("containLabel", true);
        option.put("grid", grid);

        return option;
    }

    private String mapChartType(String ct) {
        if (ct == null) return "bar";
        switch (ct.toLowerCase()) {
            case "line":
                return "line";
            case "pie":
                return "pie";
            case "scatter":
                return "scatter";
            default:
                return "bar";
        }
    }

    private void sortTooltip(Map<String, Object> option) {
        Map<String, Object> tt = new LinkedHashMap<>();
        tt.put("trigger", "item");
        tt.put("formatter", "{b}: {c} ({d}%)");
        option.put("tooltip", tt);
    }
}