package com.comp.dept.service.repository.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Repository
@Slf4j
public class HmacRepositoryImpl implements HmacRepository {

    private JdbcTemplate jdbcTemplate;

    private AppConfiguration appConfiguration;

    private MonitoringLog monitoringLog;

    private Map<String, String> localHmacMap = new HashMap<>();

    @Autowired
    public HmacRepositoryImpl(JdbcTemplate jdbcTemplate, AppConfiguration appConfiguration, MonitoringLog monitoringLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.appConfiguration = appConfiguration;
        this.monitoringLog = monitoringLog;
    }

    @Override
    @Cacheable(value = "hmac")
    @EventListener(AppEvent.class)
    public Map<String, String> getHmacMap() {

        Map<String, String> hmacMap = null;

        try {
            hmacMap = jdbcTemplate
                    .query(
                            "SELECT app_id, id, secret FROM HMAC WHERE APP_ID=1",
                            new BeanPropertyRowMapper<>(Hmac.class))
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Hmac::getId,
                                    hmac ->
                                            Crypto.decrypt(hmac.getSecret(), appConfiguration.getSecretKey())));
        } catch (DataAccessException e) {
            log.error("failed to create HMAC cache",
                    kv(IS_DB_CALL.value(), true),
                    kv(SQL_RESPONSE.value(), e));
            monitoringLog.sendAlert(
                    "failed to create HMAC cache", e);
        }
        if (hmacMap != null) {
            localHmacMap = hmacMap;
        }
        return localHmacMap;
    }
}
