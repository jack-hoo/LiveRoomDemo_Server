package com.liveroom.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 从请求中解析访客 IP。
 *
 * <p><strong>安全提示：</strong>本项目用 IP 作为访客身份（数据库主键、昵称绑定），
 * 而 {@code X-Forwarded-For} 之类的头部是客户端可以随便伪造的。只有当应用确实部署在
 * 可信反向代理（Nginx 等）后面、且代理会覆写这些头部时，才应该信任它们。
 * 因此是否读取代理头由 {@code liveroom.trust-proxy-headers} 控制，默认关闭。
 *
 * <p>原实现无条件信任代理头，且在拿到 127.0.0.1 时会退回
 * {@code InetAddress.getLocalHost()}——那返回的是服务器自己的地址，会让所有本机访客
 * 共用同一个错误身份，在无法解析主机名时还会抛异常。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    /** 数据库 user.ip 列的长度上限，超长直接截断，避免插入时报错。 */
    private static final int MAX_IP_LENGTH = 64;

    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtil() {
    }

    /**
     * @param trustProxyHeaders 是否信任反向代理写入的客户端 IP 头部
     * @return 访客 IP；实在取不到时返回 {@code "unknown"}，而不是 null
     */
    public static String getIp(HttpServletRequest request, boolean trustProxyHeaders) {
        if (request == null) {
            return UNKNOWN;
        }
        if (trustProxyHeaders) {
            for (String header : PROXY_HEADERS) {
                String candidate = firstAddress(request.getHeader(header));
                if (candidate != null) {
                    return truncate(candidate);
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isEmpty() ? UNKNOWN : truncate(remoteAddr);
    }

    /**
     * 取出 {@code X-Forwarded-For: client, proxy1, proxy2} 中的第一段（最接近客户端的那个）。
     *
     * <p>原实现只有在整个头部字符串长度大于 15 时才按逗号切分，IPv6 地址和
     * {@code "10.0.0.1, 10.0.0.2"} 这种短串都会被漏掉，直接把整串当成 IP 存进数据库。
     */
    private static String firstAddress(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        for (String part : headerValue.split(",")) {
            String candidate = part.trim();
            if (!candidate.isEmpty() && !UNKNOWN.equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String truncate(String ip) {
        return ip.length() > MAX_IP_LENGTH ? ip.substring(0, MAX_IP_LENGTH) : ip;
    }
}
