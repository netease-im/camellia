package com.netease.nim.camellia.id.gen.springboot.strict;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IdGenResult;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            return IdGenResult.success(camelliaStrictIdGen.genId(tag));
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
            return IdGenResult.success(camelliaStrictIdGen.peekId(tag));
        } catch (CamelliaIdGenException e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return IdGenResult.error("internal error");
        }
    }
}
