package com.liveroom.service;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

public class IpUtilTest {

    @Test
    public void ignoresProxyHeadersWhenNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        // 默认不信任代理头，否则任何人都能伪造身份（IP 就是这个项目的访客主键）
        assertEquals("10.0.0.9", IpUtil.getIp(request, false));
    }

    @Test
    public void takesFirstHopFromForwardedChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1, 10.0.0.2");

        assertEquals("1.2.3.4", IpUtil.getIp(request, true));
    }

    /**
     * 原实现只在头部字符串长度大于 15 时才按逗号切分，"1.2.3.4,5.6.7.8" 这种
     * 15 字符的转发链会被整串当成 IP 存进数据库。
     */
    @Test
    public void splitsShortForwardedChainToo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "1.2.3.4,5.6.7.8");

        assertEquals("1.2.3.4", IpUtil.getIp(request, true));
    }

    @Test
    public void skipsUnknownEntries() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "unknown, 1.2.3.4");

        assertEquals("1.2.3.4", IpUtil.getIp(request, true));
    }

    /**
     * 原实现拿到 127.0.0.1 时会退回 InetAddress.getLocalHost()，返回服务器自己的网卡地址，
     * 让所有本机访客共用同一个错误身份。
     */
    @Test
    public void keepsLoopbackAddressAsIs() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertEquals("127.0.0.1", IpUtil.getIp(request, true));
    }

    @Test
    public void returnsUnknownInsteadOfNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertEquals("unknown", IpUtil.getIp(request, false));
        assertEquals("unknown", IpUtil.getIp(null, true));
    }

    @Test
    public void truncatesOverlongAddressToColumnWidth() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        StringBuilder overlong = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            overlong.append('a');
        }
        request.setRemoteAddr(overlong.toString());

        assertEquals(64, IpUtil.getIp(request, false).length());
    }
}
