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
        try {
            long id = camelliaSnowflakeIdGen.genId();
            if (logger.isDebugEnabled()) {
                logger.debug("genId, id = {}", id);
            }
            return IdGenResult.success(id);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @GetMapping("/decodeTs")
    public IdGenResult decodeTs(@RequestParam("id") long id) {
        try {
            long ts = camelliaSnowflakeIdGen.decodeTs(id);
            if (logger.isDebugEnabled()) {
                logger.debug("decodeTs, id = {}, ts = {}", id, ts);
            }
            return IdGenResult.success(ts);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }
}
