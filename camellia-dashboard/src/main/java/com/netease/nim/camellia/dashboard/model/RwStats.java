package com.netease.nim.camellia.dashboard.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2019/6/5.
 */
public class RwStats {

    private List<Total> totalList = new ArrayList<>();
    private List<Detail> detailList = new ArrayList<>();
    private List<BusinessTotal> businessTotalList = new ArrayList<>();
    private List<BusinessDetail> businessDetailList = new ArrayList<>();

    public List<Total> getTotalList() {
        return totalList;
    }

    public void setTotalList(List<Total> totalList) {
        this.totalList = totalList;
    }

    public List<Detail> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<Detail> detailList) {
        this.detailList = detailList;
    }

    public List<BusinessTotal> getBusinessTotalList() {
        return businessTotalList;
    }

    public void setBusinessTotalList(List<BusinessTotal> businessTotalList) {
        this.businessTotalList = businessTotalList;
    }

    public List<BusinessDetail> getBusinessDetailList() {
        return businessDetailList;
    }

    public void setBusinessDetailList(List<BusinessDetail> businessDetailList) {
        this.businessDetailList = businessDetailList;
    }

    public static class Total {
        private String resource;
        private String ope;
        private long count;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class Detail {
        private String resource;
        private String className;
        private String methodName;
        private String ope;
        private long count;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class BusinessTotal {
        private String bid;
        private String bgroup;
        private String resource;
        private String ope;
        private long count;

        public String getBid() {
            return bid;
        }

        public void setBid(String bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class BusinessDetail {
        private String bid;
        private String bgroup;
        private String resource;
        private String className;
        private String methodName;
        private String ope;
        private long count;

        public String getBid() {
            return bid;
        }

        public void setBid(String bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getOpe() {
            return ope;
        }

        public void setOpe(String ope) {
            this.ope = ope;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
