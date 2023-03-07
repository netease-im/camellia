package com.netease.nim.camellia.dashboard.dao;

import com.netease.nim.camellia.dashboard.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by caojiajun on 2023/3/7
 */
@Repository
public interface ConfigDao extends JpaRepository<Config, Long> {

    List<Config> findByNamespace(String namespace);

    Config findByNamespaceAndKey(String namespace, String key);
}
