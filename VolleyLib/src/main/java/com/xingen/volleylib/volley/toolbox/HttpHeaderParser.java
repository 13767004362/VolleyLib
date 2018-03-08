/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingen.volleylib.volley.toolbox;


import com.xingen.volleylib.volley.Cache;
import com.xingen.volleylib.volley.NetworkResponse;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;

import java.util.Map;

/**
 * Utility methods for parsing HTTP headers.
 *
 * 用途：
 *    解析http标头
 */
public class HttpHeaderParser {

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     *
     * @param response The network response to parse headers from
     * @return a cache entry for the given response, or null if the response is not cacheable.
     *
     * 从NetworkRespone 生成一个Cache.Entry，即将http的标头信息写入Entry中
     */
    public static Cache.Entry parseCacheHeaders(NetworkResponse response) {

        //当前时间
        long now = System.currentTimeMillis();
        //服务器返回的标头
        Map<String, String> headers = response.headers;

        long serverDate = 0;
        long serverExpires = 0;
        long softExpire = 0;
        long maxAge = 0;
        boolean hasCacheControl = false;

        String serverEtag = null;
        String headerValue;
        //服务器响应的时间
        headerValue = headers.get("Date");
        if (headerValue != null) {
            //获取到long类型的服务器响应时间
            serverDate = parseDateAsEpoch(headerValue);
        }
        /*  HTTP 1.1 引入 Cache-Control 响应头参数，弥补了 Expires的局限。
         *  Http的缓存设置，Cache-Control用于控制缓存，常见的取值有private、no-cache、max-age、must- revalidate等，默认Private.
         *
         *  private ：响应只能够作为私有的缓存，默认。
         *   no-cache:每次访问，都刷新，实时向服务器端请求资源 。
          *  max-age:设置缓存最大的有效时间，过完指定时间后再访问刷新
          *  no-store：响应不缓存,不写进磁盘中，基于某些安全考虑。
          *  must-revalidate :响应在特定条件下会被重用，以满足接下来的请求，但是它必须到服务器端去验证它是不是仍然是最新的。
          *  proxy-revalidate ：类似于 must-revalidate,但不适用于代理缓存.
         */
        headerValue = headers.get("Cache-Control");
        if (headerValue != null) {
            hasCacheControl = true;
            String[] tokens = headerValue.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (token.equals("no-cache") || token.equals("no-store")) {
                    return null;
                } else if (token.startsWith("max-age=")) {
                    try {
                        maxAge = Long.parseLong(token.substring(8));
                    } catch (Exception e) {
                    }
                } else if (token.equals("must-revalidate") || token.equals("proxy-revalidate")) {
                    maxAge = 0;
                }
            }
        }
        //服务器返回的数据过期
        headerValue = headers.get("Expires");
        if (headerValue != null) {
            //服务器返回的数据过期时间
            serverExpires = parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        // Cache-Control takes precedence over an Expires header, even if both exist and Expires
        // is more restrictive.
        /**
         * 若是两者同时存在 ，Cache-Control是优先于Expires标头，Expires是更多的局限性。
         *
         * HTTP1.1的典型标头：
         *
         *  HTTP/1.1 200 OK
         *  Date: Fri, 30 Oct 1998 13:19:41 GMT
         *  Server: Apache/1.3.3 (Unix)
         *  Cache-Control: max-age=3600, must-revalidate
         *  Expires: Fri, 30 Oct 1998 14:19:41 GMT
         *  Last-Modified: Mon, 29 Jun 1998 02:28:12 GMT
         *  ETag: "3e86-410-3596fbbc"
         *  Content-Length: 1040
         *  Content-Type: text/html
         */
        if (hasCacheControl) {  //Cache-Control标头存在的情况
            //过期时间=（当前时间+缓存的有效时间*1000）
            softExpire = now + maxAge * 1000;
        } else if (serverDate > 0 && serverExpires >= serverDate) { //Cache-Control标头不存在的情况
            // Default semantic for Expire header in HTTP specification is softExpire.
            //在Http规范中Expire标头的语义是softExpire.
            //过期时间=现在时间+（服务器返回数据的过期时间-服务器响应时间）
            softExpire = now + (serverExpires - serverDate);
        }

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = entry.softTtl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }

    /**
     * Parse date in RFC1123 format, and return its value as epoch
     *
     * 按RFC1123格式解析时间，返回long类型的date
     */
    public static long parseDateAsEpoch(String dateStr) {
        try {
            // Parse date in RFC1123 format if this header contains one
            return DateUtils.parseDate(dateStr).getTime();
        } catch (DateParseException e) {
            // Date in invalid format, fallback to 0
            return 0;
        }
    }

    /**
     * Returns the charset specified in the Content-Type of this header,
     * or the HTTP default (ISO-8859-1) if none can be found.
     *
     * 返回标头的内容编码类型，若是服务器没有返回内容编码类型，这里默认ISO-8859-1格式。
     */
    public static String parseCharset(Map<String, String> headers) {
        String contentType = headers.get(HTTP.CONTENT_TYPE);
        if (contentType != null) {
            String[] params = contentType.split(";");
            for (int i = 1; i < params.length; i++) {
                String[] pair = params[i].trim().split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("charset")) {
                        return pair[1];
                    }
                }
            }
        }

        return HTTP.DEFAULT_CONTENT_CHARSET;
    }
}
