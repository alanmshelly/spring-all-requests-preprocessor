package com.example.requestlogger;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Component
public class GetTIDFilter implements Filter {
    final private Logger logger;

    public GetTIDFilter(TransactionLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger();
    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String tid = request.getHeader("X-Tracing-ID");
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        requestAttributes.setAttribute("X-Tracing-ID", tid, SCOPE_REQUEST);
        logger.info("Logging X-Tracing-ID: {}", tid);


        filterChain.doFilter(servletRequest, servletResponse);
    }
}
