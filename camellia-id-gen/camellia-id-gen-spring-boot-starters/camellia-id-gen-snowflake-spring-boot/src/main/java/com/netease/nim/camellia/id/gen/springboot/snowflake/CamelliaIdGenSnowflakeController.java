package com.netease.nim.camellia.id.gen.springboot.snowflake;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IdGenResult;
import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeIdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * Created by caojiajun on 2021/9/26
 */
@RestController
@RequestMapping("/camellia/id/gen/snowflake")
public class CamelliaIdGenSnowflakeController {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdGenSnowflakeController.class);

    @Autowired
    private CamelliaSnowflakeIdGen camelliaSnowflakeIdGen;

    @GetMapping("/genId")
    public IdGenResult genId() {
        String uri = "/camellia/id/gen/snowflake/genId";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSnowflakeServerStatus.updateLastUseTime();
            long id = camelliaSnowflakeIdGen.genId();
            if (logger.isDebugEnabled()) {
                logger.debug("genId, id = {}", id);
            }
            return IdGenResult.success(uri, startTime, id);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/decodeTs")
    public IdGenResult decodeTs(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/snowflake/decodeTs";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSnowflakeServerStatus.updateLastUseTime();
            long ts = camelliaSnowflakeIdGen.decodeTs(id);
            if (logger.isDebugEnabled()) {
                logger.debug("decodeTs, id = {}, ts = {}", id, ts);
            }
            return IdGenResult.success(uri, startTime, ts);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/decodeRegionId")
    public IdGenResult decodeRegionId(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/snowflake/decodeRegionId";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSnowflakeServerStatus.updateLastUseTime();
            long regionId = camelliaSnowflakeIdGen.decodeRegionId(id);
            if (logger.isDebugEnabled()) {
                logger.debug("decodeRegionId, id = {}, regionId = {}", id, regionId);
            }
            return IdGenResult.success(uri, startTime, regionId);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/decodeSequence")
    public IdGenResult decodeSequence(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/snowflake/decodeSequence";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSnowflakeServerStatus.updateLastUseTime();
            long regionId = camelliaSnowflakeIdGen.decodeSequence(id);
            if (logger.isDebugEnabled()) {
                logger.debug("decodeSequence, id = {}, regionId = {}", id, regionId);
            }
            return IdGenResult.success(uri, startTime, regionId);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/decodeWorkerId")
    public IdGenResult decodeWorkerId(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/snowflake/decodeWorkerId";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSnowflakeServerStatus.updateLastUseTime();
            long regionId = camelliaSnowflakeIdGen.decodeWorkerId(id);
            if (logger.isDebugEnabled()) {
                logger.debug("decodeWorkerId, id = {}, regionId = {}", id, regionId);
            }
            return IdGenResult.success(uri, startTime, regionId);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri,startTime, "internal error");
        }
    }
}
