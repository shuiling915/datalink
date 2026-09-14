package com.datalink.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlAlertChannelMapper;
import com.datalink.model.DlAlertChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 告警通知服务 — 支持钉钉/企微/邮件/Webhook
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotifyService {

    private final DlAlertChannelMapper alertChannelMapper;
    private final JavaMailSender mailSender;
    private final MessageService messageService;
    private final AlertCenterService alertCenterService;

    @Value("${spring.mail.from:datalink@company.com}")
    private String mailFrom;

    /**
     * 发送告警通知（遍历所有启用的渠道 + 站内消息），并记录到告警中心
     */
    @Async
    public void sendAlert(String title, String content) {
        sendAlert("system", null, "warning", title, content);
    }

    /**
     * 发送告警通知并记录到告警中心
     */
    @Async
    public void sendAlert(String sourceType, Long sourceId, String severity,
                          String title, String content) {
        alertCenterService.recordAlert(sourceType, sourceId, severity, title, content);

        // 发送站内消息给管理员
        try {
            messageService.send("admin", title, content, "alert", null);
        } catch (Exception e) {
            log.error("站内告警消息发送失败", e);
        }

        List<DlAlertChannel> channels = alertChannelMapper.selectList(
                new QueryWrapper<DlAlertChannel>().eq("enabled", 1));
        if (channels == null || channels.isEmpty()) {
            log.warn("未配置告警通知渠道，告警信息: {}", title);
            return;
        }
        for (DlAlertChannel channel : channels) {
            try {
                switch (channel.getChannelType()) {
                    case "dingtalk":
                        sendDingTalk(channel.getConfig(), title, content);
                        break;
                    case "wechat":
                        sendWeChat(channel.getConfig(), title, content);
                        break;
                    case "email":
                        sendEmail(channel.getConfig(), title, content);
                        break;
                    case "webhook":
                        sendWebhook(channel.getConfig(), title, content);
                        break;
                    default:
                        log.warn("不支持的告警渠道类型: {}", channel.getChannelType());
                }
            } catch (Exception e) {
                log.error("告警通知发送失败 channel={}", channel.getChannelName(), e);
            }
        }
    }

    /** 钉钉机器人 */
    private void sendDingTalk(String configJson, String title, String content) throws Exception {
        JSONObject config = JSON.parseObject(configJson);
        String webhook = config.getString("webhook");
        String secret = config.getString("secret");

        long timestamp = System.currentTimeMillis();
        String sign = "";
        if (secret != null && !secret.isEmpty()) {
            String stringToSign = timestamp + "\n" + secret;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            sign = "&timestamp=" + timestamp + "&sign=" +
                    java.net.URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(signData), "UTF-8");
        }

        JSONObject msg = new JSONObject();
        msg.put("msgtype", "markdown");
        JSONObject md = new JSONObject();
        md.put("title", title);
        md.put("text", "### " + title + "\n\n" + content);
        msg.put("markdown", md);

        postJson(webhook + sign, msg.toJSONString());
    }

    /** 企业微信机器人 */
    private void sendWeChat(String configJson, String title, String content) throws Exception {
        JSONObject config = JSON.parseObject(configJson);
        String webhook = config.getString("webhook");

        JSONObject msg = new JSONObject();
        msg.put("msgtype", "markdown");
        JSONObject md = new JSONObject();
        md.put("content", "### " + title + "\n" + content);
        msg.put("markdown", md);

        postJson(webhook, msg.toJSONString());
    }

    /** 邮件 */
    private void sendEmail(String configJson, String title, String content) {
        JSONObject config = JSON.parseObject(configJson);
        String to = config.getString("to");
        if (to == null || to.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to.split(","));
        message.setSubject("[DataLink告警] " + title);
        message.setText(content);
        mailSender.send(message);
    }

    /** 通用 Webhook */
    private void sendWebhook(String configJson, String title, String content) throws Exception {
        JSONObject config = JSON.parseObject(configJson);
        String url = config.getString("url");
        JSONObject payload = new JSONObject();
        payload.put("title", title);
        payload.put("content", content);
        payload.put("timestamp", System.currentTimeMillis());
        postJson(url, payload.toJSONString());
    }

    private void postJson(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code >= 400) {
            log.error("Webhook 返回错误状态码: {}", code);
        }
        conn.disconnect();
    }
}