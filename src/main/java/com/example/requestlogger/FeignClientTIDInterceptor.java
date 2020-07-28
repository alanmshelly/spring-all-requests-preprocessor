package com.example.requestlogger;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Component
public class FeignClientTIDInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return;
        }
        String tid = (String) requestAttributes.getAttribute("X-Tracing-ID", SCOPE_REQUEST);
        requestTemplate.header("X-Tracing-ID", tid);
    }
}
