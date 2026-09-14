package com.datalink.aspect;

import com.alibaba.fastjson.JSON;
import com.datalink.annotation.AuditLog;
import com.datalink.model.DlAuditLog;
import com.datalink.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作审计日志切面
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        DlAuditLog logEntry = new DlAuditLog();

        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            logEntry.setUsername(auth.getName());
        }

        logEntry.setModule(auditLog.module());
        logEntry.setOperation(auditLog.operation());
        logEntry.setCreatedAt(LocalDateTime.now());

        // 请求信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntry.setMethod(request.getMethod());
                logEntry.setRequestUri(request.getRequestURI());
                logEntry.setIpAddress(getIpAddress(request));
                // 请求参数（截断避免过长）
                try {
                    Object[] args = joinPoint.getArgs();
                    if (args != null && args.length > 0) {
                        String params = JSON.toJSONString(args);
                        if (params.length() > 2000) {
                            params = params.substring(0, 2000) + "...";
                        }
                        logEntry.setRequestParams(params);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }

        Object result;
        try {
            result = joinPoint.proceed();
            logEntry.setStatus(1);
        } catch (Throwable e) {
            logEntry.setStatus(0);
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.length() > 1024) {
                errMsg = errMsg.substring(0, 1024);
            }
            logEntry.setErrorMsg(errMsg);
            logEntry.setCostMs(System.currentTimeMillis() - start);
            auditLogService.record(logEntry);
            throw e;
        }

        logEntry.setCostMs(System.currentTimeMillis() - start);
        auditLogService.record(logEntry);
        return result;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}