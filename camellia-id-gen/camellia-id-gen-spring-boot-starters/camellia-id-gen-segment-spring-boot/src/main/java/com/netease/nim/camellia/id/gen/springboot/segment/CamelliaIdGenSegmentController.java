package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IdGenResult;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2021/9/27
 */
@RestController
@RequestMapping("/camellia/id/gen/segment")
public class CamelliaIdGenSegmentController {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdGenSegmentController.class);

    @Autowired
    private CamelliaSegmentIdGen camelliaSegmentIdGen;

    @Autowired
    private IdSyncInMultiRegionService idSyncInMultiRegionService;

    @GetMapping("/genIds")
    public IdGenResult genIds(@RequestParam("tag") String tag,
                              @RequestParam("count") int count) {
        String uri = "/camellia/id/gen/segment/genIds?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            List<Long> ids = camelliaSegmentIdGen.genIds(tag, count);
            if (logger.isDebugEnabled()) {
                logger.debug("genIds, tag = {}, count = {}, ids = {}", tag, count, ids);
            }
            return IdGenResult.success(uri, startTime, ids);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/genId")
    public IdGenResult genId(@RequestParam("tag") String tag) {
        String uri = "/camellia/id/gen/segment/genId?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            long id = camelliaSegmentIdGen.genId(tag);
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

    @GetMapping("/decodeRegionId")
    public IdGenResult decodeRegionId(@RequestParam("id") long id) {
        String uri = "/camellia/id/gen/segment/decodeRegionId";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            long regionId = camelliaSegmentIdGen.decodeRegionId(id);
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
        String uri = "/camellia/id/gen/segment/update?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            boolean result = camelliaSegmentIdGen.getIdLoader().update(tag, id);
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

    @GetMapping("/selectTagIdMaps")
    public IdGenResult selectTagIdMaps() {
        String uri = "/camellia/id/gen/segment/selectTagIdMaps";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            Map<String, Long> map = camelliaSegmentIdGen.getIdLoader().selectTagIdMaps();
            if (logger.isDebugEnabled()) {
                logger.debug("selectTagIdMaps, map = {}", map);
            }
            return IdGenResult.success(uri, startTime, map);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }

    @GetMapping("/selectId")
    public IdGenResult selectId(@RequestParam("tag") String tag) {
        String uri = "/camellia/id/gen/segment/selectId?tag=" + tag;
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            Long id = camelliaSegmentIdGen.getIdLoader().selectId(tag);
            if (logger.isDebugEnabled()) {
                logger.debug("selectId, tag = {}, id = {}", id, tag);
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

    @GetMapping("/sync")
    public IdGenResult sync() {
        String uri = "/camellia/id/gen/segment/sync";
        long startTime = System.currentTimeMillis();
        try {
            CamelliaIdGenSegmentServerStatus.updateLastUseTime();
            boolean success = idSyncInMultiRegionService.sync();
            if (logger.isDebugEnabled()) {
                logger.debug("sync, result = {}", success);
            }
            return IdGenResult.success(uri, startTime, success);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(uri, startTime, "internal error");
        }
    }
}
