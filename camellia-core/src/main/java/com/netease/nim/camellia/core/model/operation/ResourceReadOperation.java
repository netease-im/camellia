package com.netease.nim.camellia.core.model.operation;

import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ResourceReadOperation {

    private Type type;
    private Resource readResource;
    private List<Resource> readResources;

    public ResourceReadOperation() {
    }

    public ResourceReadOperation(Resource readResource) {
        this.type = Type.SIMPLE;
        this.readResource = readResource;
    }

    public ResourceReadOperation(Type type, List<Resource> readResources) {
        this.type = type;
        this.readResources = readResources;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Resource getReadResource() {
        return readResource;
    }

    public void setReadResource(Resource readResource) {
        this.readResource = readResource;
    }

    public List<Resource> getReadResources() {
        return readResources;
    }

    public void setReadResources(List<Resource> readResources) {
        this.readResources = readResources;
    }

    public static enum Type {

        //只有一个resource
        SIMPLE,

        //多个resource，取多次，一个失败了取下一个
        ORDER,

        //多个resource，随机取一个，取一次
        RANDOM,
        ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceReadOperation that = (ResourceReadOperation) o;

        if (type != that.type) return false;
        if (readResource != null ? !readResource.equals(that.readResource) : that.readResource != null) return false;
        return readResources != null ? readResources.equals(that.readResources) : that.readResources == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (readResource != null ? readResource.hashCode() : 0);
        result = 31 * result + (readResources != null ? readResources.hashCode() : 0);
        return result;
    }
}
