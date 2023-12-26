package com.netease.nim.camellia.id.gen.springboot.strict;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IdGenResult;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by caojiajun on 2021/9/27
 */
@RestController
@RequestMapping("/camellia/id/gen/strict")
public class CamelliaIdGenStrictController {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdGenStrictController.class);

    @Autowired
    private CamelliaStrictIdGen camelliaStrictIdGen;

    @GetMapping("/genId")
    public IdGenResult genId(@RequestParam("tag") String tag) {
        String uri = "/camellia/id/gen/strict/genId?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenStrictServerStatus.updateLastUseTime();
            long id = camelliaStrictIdGen.genId(tag);
            if (logger.isDebugEnabled()) {
                logger.debug("genId, tag = {}, id = {}", tag, id);
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

    @GetMapping("/peekId")
    public IdGenResult peekId(@RequestParam("tag") String tag) {
        String uri = "/camellia/id/gen/strict/peekId?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenStrictServerStatus.updateLastUseTime();
            long id = camelliaStrictIdGen.peekId(tag);
            if (logger.isDebugEnabled()) {
                logger.debug("peekId, tag = {}, id = {}", tag, id);
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

    @GetMapping("/decodeRegionId")
    public IdGenResult decodeRegionId(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/strict/decodeRegionId";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenStrictServerStatus.updateLastUseTime();
            long regionId = camelliaStrictIdGen.decodeRegionId(id);
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

    @PostMapping("/update")
    public IdGenResult update(@RequestParam("tag") String tag, @RequestParam("id") long id) {
        String uri = "/camellia/id/gen/strict/update?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenStrictServerStatus.updateLastUseTime();
            boolean result = camelliaStrictIdGen.getIdLoader().update(tag, id);
            if (logger.isDebugEnabled()) {
                logger.debug("update, tag = {}, id = {}, result = {}", tag, id, result);
            }
            return IdGenResult.success(uri, startTime, result);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }
}
