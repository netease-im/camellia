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
        try {
            long id = camelliaStrictIdGen.genId(tag);
            if (logger.isDebugEnabled()) {
                logger.debug("genId, tag = {}, id = {}", tag, id);
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

    @GetMapping("/peekId")
    public IdGenResult peekId(@RequestParam("tag") String tag) {
        try {
            long id = camelliaStrictIdGen.peekId(tag);
            if (logger.isDebugEnabled()) {
                logger.debug("peekId, tag = {}, id = {}", tag, id);
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

    @PostMapping("/update")
    public IdGenResult update(@RequestParam("tag") String tag, @RequestParam("id") long id) {
        try {
            boolean result = camelliaStrictIdGen.getIdLoader().update(tag, id);
            if (logger.isDebugEnabled()) {
                logger.debug("update, tag = {}, id = {}, result = {}", tag, id, result);
            }
            return IdGenResult.success(result);
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }
}
