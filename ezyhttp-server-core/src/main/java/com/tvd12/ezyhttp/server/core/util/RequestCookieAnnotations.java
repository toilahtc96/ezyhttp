package com.tvd12.ezyhttp.server.core.util;

import com.tvd12.ezyfox.io.EzyStrings;
import com.tvd12.ezyhttp.server.core.annotation.RequestCookie;

public final class RequestCookieAnnotations {

    private RequestCookieAnnotations() {}
    
    public static String getCookieKeyString(RequestCookie annotation, int index) {
        String keyString = annotation.value();
        if (EzyStrings.isNoContent(keyString)) {
            keyString = annotation.name();
        }
        if (EzyStrings.isNoContent(keyString)) {
            keyString = String.valueOf(index);
        } else {
            keyString = EzyStrings.quote(keyString);
        }
        return keyString;
    }
    
}
