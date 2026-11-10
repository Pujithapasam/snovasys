package com.ledgerline.config;

import com.ledgerline.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitingFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip static resources, health checks, and options preflights
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/health")
                || path.startsWith("/h2-console")
                || path.startsWith("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(clientIp, path);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));
        response.setHeader("X-RateLimit-Backend", result.isRedisBacked() ? "Redis-Cluster" : "In-Memory-SlidingWindow");

        if (!result.isAllowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.getResetSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"limit\":%d}",
                    result.getResetSeconds(), result.getLimit()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.trim().isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
