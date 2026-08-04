package com.lingqu.executor.engine;

import com.google.common.util.concurrent.RateLimiter;
import com.lingqu.executor.common.BizException;
import com.lingqu.executor.entity.Api;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地令牌桶限流（Guava RateLimiter，需求 3.6.1）。
 * 接口级 QPS 配置存储在配置库，由 ConfigCache 定期刷新；此处按 apiId+qps 建立独立令牌桶。
 */
@Component
public class RateLimiterManager {

    private final ConcurrentMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public void acquire(Api api) {
        double qps = api.getRateLimitQps() == null ? 0 : api.getRateLimitQps().doubleValue();
        if (qps <= 0) {
            return;
        }
        String key = api.getId() + "#" + qps;
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> RateLimiter.create(qps));
        if (!limiter.tryAcquire()) {
            throw new BizException(429, "请求过于频繁，请稍后再试（QPS 限制 " + qps + "）");
        }
    }
}
