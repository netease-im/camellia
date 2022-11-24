package com.netease.nim.camellia.dashboard.util;

import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.dashboard.dto.CreateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class IpCheckerUtil {

    private static final String REGEX_SPLIT_IP = ", ";
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}(/\\d{1,2})*$");

    public static boolean checkValidIp(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidIpList(Collection<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return false;
        }
        return ipList.stream().allMatch(IpCheckerUtil::checkValidIp);
    }

    public static IpChecker convertToModel(CreateIpCheckerRequest request) {
        IpChecker ipChecker = new IpChecker();
        ipChecker.setBid(request.getBid());
        ipChecker.setBgroup(request.getBgroup());
        ipChecker.setMode(request.getMode());
        ipChecker.setIpList(request.getIpList());
        return ipChecker;
    }

    public static IpCheckerDto convertToDto(IpChecker ipChecker) {
        IpCheckerDto dto = new IpCheckerDto();
        dto.setBid(ipChecker.getBid());
        dto.setBgroup(ipChecker.getBgroup());
        dto.setMode(ipChecker.getMode());
        dto.setIpList(ipChecker.getIpList());
        return dto;
    }

}
