package com.ottsz.stationpublicity.bean;

public class Resource {

    public Resource(int id, int type, String url, String md5, String localName, int sort) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.md5 = md5;
        this.localName = localName;
        this.sort = sort;
    }

    private int id;

    private int type;

    private String url;

    private String md5;

    private String localName;

    private int sort;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        Resource resource = (Resource) o;
        return id == resource.id
                && type == resource.type
                && sort == resource.sort
                && url.equals(resource.url)
                && md5.equals(resource.md5);
    }

}