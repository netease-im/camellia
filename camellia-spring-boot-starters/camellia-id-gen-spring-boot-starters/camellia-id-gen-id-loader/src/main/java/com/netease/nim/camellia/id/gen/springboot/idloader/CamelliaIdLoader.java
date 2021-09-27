package com.netease.nim.camellia.id.gen.springboot.idloader;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 *
 * Created by caojiajun on 2021/9/27
 */
@Service
public class CamelliaIdLoader implements IDLoader {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdLoader.class);

    @Resource
    private CamelliaIdLoaderMapper mapper;

    @Override
    @Transactional(rollbackForClassName = { "Exception" })
    public IDRange load(String tag, int step) {
        long now = System.currentTimeMillis();
        Long id = mapper.selectForUpdate(tag);
        if (id == null) {
            int insert = mapper.insert(tag, step, now, now);
            IDRange idRange = new IDRange(1, step);
            if (logger.isDebugEnabled()) {
                logger.debug("insert tag = {}, step = {}, result = {}, ret = {}", tag, step, insert, idRange);
            }
            return idRange;
        } else {
            int update = mapper.update(tag, id + step, now);
            IDRange idRange = new IDRange(id + 1, id + step);
            if (logger.isDebugEnabled()) {
                logger.debug("update tag = {}, step = {}, result = {}, ret = {}", tag, step, update, idRange);
            }
            return idRange;
        }
    }
}
