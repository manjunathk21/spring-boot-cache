package com.comp.dept.service.config;

import com.comp.dept.service.client.HttpClient;
import com.comp.dept.service.repository.HmacRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class CacheConfig {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private HmacRepository hmacRepository;

    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshCacheManager() {

        cacheManager.getCache("strategies").clear();
        httpClient.getStrategies();

        cacheManager.getCache("hmac").clear();
        hmacRepository.getHmacMap();

    }
}
