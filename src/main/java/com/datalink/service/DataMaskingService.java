package com.datalink.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 数据脱敏服务 — 对敏感字段进行脱敏处理
 */
@Slf4j
@Service
public class DataMaskingService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");

    /**
     * 自动识别并脱敏敏感数据
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (PHONE_PATTERN.matcher(value).matches()) {
            return maskPhone(value);
        }
        if (ID_CARD_PATTERN.matcher(value).matches()) {
            return maskIdCard(value);
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return maskEmail(value);
        }
        if (BANK_CARD_PATTERN.matcher(value).matches()) {
            return maskBankCard(value);
        }
        return value;
    }

    /** 手机号脱敏：138****1234 */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 身份证脱敏：110101********1234 */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /** 邮箱脱敏：a***@example.com */
    public String maskEmail(String email) {
        if (email == null) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        String prefix = email.substring(0, 1);
        String domain = email.substring(atIdx);
        return prefix + "***" + domain;
    }

    /** 银行卡脱敏：6222********1234 */
    public String maskBankCard(String card) {
        if (card == null || card.length() < 8) return card;
        return card.substring(0, 4) + "********" + card.substring(card.length() - 4);
    }

    /** 姓名脱敏：张* */
    public String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }

    /** 通用脱敏：保留首尾，中间用 * 替换 */
    public String maskGeneric(String value) {
        if (value == null || value.length() <= 2) return value;
        int len = value.length();
        int keep = Math.max(1, len / 4);
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, keep);
        for (int i = 0; i < len - keep * 2; i++) {
            sb.append("*");
        }
        sb.append(value, len - keep, len);
        return sb.toString();
    }
}