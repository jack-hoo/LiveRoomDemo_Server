package com.liveroom.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 根据 User-Agent 粗略判断是手机还是电脑，用来决定返回移动端单页还是桌面端页面。
 */
public final class UserAgentUtil {

    private static final Set<String> MOBILE_KEYWORDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "iphone", "ipod", "android", "phone", "mobile", "wap", "netfront", "opera mobi",
            "opera mini", "ucweb", "windows ce", "windows phone", "symbian", "series60", "webos",
            "blackberry", "dopod", "nokia", "samsung", "palmsource", "xda", "pieplus", "meizu",
            "midp", "cldc", "motorola", "foma", "docomo", "up.browser", "up.link", "blazer",
            "helio", "hosin", "huawei", "novarra", "coolpad", "techfaith", "alcatel", "amoi",
            "ktouch", "nexian", "ericsson", "philips", "sagem", "wellcom", "bunjalloo", "maui",
            "smartphone", "iemobile", "spice", "zte-", "longcos", "pantech", "gionee", "portalmmm",
            "jig browser", "hiptop", "benq", "haier", "240x320", "176x220", "googlebot-mobile"
    )));

    private UserAgentUtil() {
    }

    /**
     * 注意：原实现的关键词表里混进了 {@code "java"}、{@code "sony"}、{@code "bird"}、
     * {@code "w3c "} 这类过于宽泛的片段，以及大量 4 字符前缀（{@code "play"}、{@code "port"}、
     * {@code "cell"} 等）。它们会把普通桌面浏览器、爬虫和 HTTP 客户端误判成手机，
     * 比如 UA 里带 "Java" 的请求库。这里只保留有明确手机语义的关键词。
     */
    public static boolean isMobile(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return false;
        }
        String lower = userAgent.toLowerCase(Locale.ENGLISH);
        // iPad 自带 "mobile" 关键词，但屏幕更接近桌面，这里按桌面端处理。
        if (lower.contains("ipad")) {
            return false;
        }
        for (String keyword : MOBILE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
