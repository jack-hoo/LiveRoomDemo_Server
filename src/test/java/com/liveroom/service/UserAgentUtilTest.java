package com.liveroom.service;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserAgentUtilTest {

    private static boolean isMobile(String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        return UserAgentUtil.isMobile(request);
    }

    @Test
    public void detectsCommonMobileBrowsers() {
        assertTrue(isMobile("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15"));
        assertTrue(isMobile("Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36 Mobile Safari/537.36"));
        assertTrue(isMobile("Mozilla/5.0 (Linux; U; Android 4.4.4;) UCWEB/2.0"));
    }

    @Test
    public void desktopBrowsersAreNotMobile() {
        assertFalse(isMobile("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"));
        assertFalse(isMobile("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15"));
    }

    /**
     * 原实现的关键词表里有 "java"，任何 UA 里带 Java 的 HTTP 客户端都会被判成手机，
     * 从而拿到移动端单页而不是桌面端页面。
     */
    @Test
    public void javaHttpClientsAreNotMobile() {
        assertFalse(isMobile("Java/1.8.0_292"));
        assertFalse(isMobile("Apache-HttpClient/4.5.13 (Java/1.8.0_292)"));
    }

    @Test
    public void ipadIsTreatedAsDesktop() {
        assertFalse(isMobile("Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148"));
    }

    @Test
    public void missingUserAgentIsNotMobile() {
        assertFalse(isMobile(null));
        assertFalse(UserAgentUtil.isMobile(null));
    }
}
