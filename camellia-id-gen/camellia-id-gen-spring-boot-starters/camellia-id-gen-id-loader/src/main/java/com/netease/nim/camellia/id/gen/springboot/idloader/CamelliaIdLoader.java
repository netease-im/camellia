package com.netease.nim.camellia.id.gen.springboot.idloader;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @Transactional(rollbackForClassName = { "Exception" })
    public boolean update(String tag, long id) {
        long now = System.currentTimeMillis();
        Long oldId = mapper.selectForUpdate(tag);
        boolean result;
        if (oldId == null) {
            result = mapper.insert(tag, id, now, now) > 0;
        } else {
            if (oldId > id) {
                logger.warn("update tag = {} fail for old.id = {} > new.id = {}", tag, oldId, id);
                return false;
            }
            result = mapper.update(tag, id, now) > 0;
        }
        logger.info("update tag = {}, old.id = {}, new.id = {}, result = {}", tag, oldId, id, result);
        return result;
    }

    @Override
    public Map<String, Long> selectTagIdMaps() {
        List<TagIdInfo> tagIdInfos = mapper.selectTagIdMaps();
        Map<String, Long> map = new HashMap<>();
        for (TagIdInfo tagIdInfo : tagIdInfos) {
            map.put(tagIdInfo.getTag(), tagIdInfo.getId());
        }
        if (logger.isInfoEnabled()) {
            logger.info("select tag-id maps, size = {}", tagIdInfos.size());
        }
        return map;
    }

    @Override
    public Long selectId(String tag) {
        return mapper.selectId(tag);
    }
}
