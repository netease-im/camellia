package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IdGenResult;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        try {
            return IdGenResult.success(camelliaSegmentIdGen.genIds(tag, count));
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @GetMapping("/genId")
    public IdGenResult genId(@RequestParam("tag") String tag) {
        try {
            return IdGenResult.success(camelliaSegmentIdGen.genId(tag));
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @PostMapping("/update")
    public IdGenResult update(@RequestParam("tag") String tag, @RequestParam("id") long id) {
        try {
            boolean result = camelliaSegmentIdGen.getIdLoader().update(tag, id);
            return IdGenResult.success(result);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @GetMapping("/selectTagIdMaps")
    public IdGenResult selectTagIdMaps() {
        try {
            Map<String, Long> map = camelliaSegmentIdGen.getIdLoader().selectTagIdMaps();
            return IdGenResult.success(map);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @GetMapping("/selectId")
    public IdGenResult selectId(@RequestParam("tag") String tag) {
        try {
            Long id = camelliaSegmentIdGen.getIdLoader().selectId(tag);
            return IdGenResult.success(id);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }

    @GetMapping("/sync")
    public IdGenResult sync() {
        try {
            boolean success = idSyncInMultiRegionService.sync();
            return IdGenResult.success(success);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }
}
