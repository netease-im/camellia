package com.netease.nim.camellia.dashboard.util;

import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.dashboard.dto.CreateOrUpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class IpCheckerUtil {

    private static final String REGEX_SPLIT_IP = ", ";
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}(/\\d{1,2})*$");

    public static String parseIpListToString(Collection<String> ipList) {
        return ipList.toString();
    }

    public static Set<String> parseIpListToSet(String ipListString) {
        String substring = ipListString.substring(1, ipListString.length() - 1);
        return new HashSet<>(Arrays.asList(substring.split(REGEX_SPLIT_IP)));
    }

    public static boolean checkValidIp(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }

    public static boolean checkValidIpList(Collection<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return false;
        }
        return ipList.stream().allMatch(IpCheckerUtil::checkValidIp);
    }

    public static IpChecker convertToModel(CreateOrUpdateIpCheckerRequest request) {
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
