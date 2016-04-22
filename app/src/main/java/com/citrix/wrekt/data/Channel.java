package com.citrix.wrekt.data;

import android.support.annotation.NonNull;

public class Channel implements Comparable<Channel> {

    private String id;
    private String name;
    private Long createTime;
    private String category;
    private String description;
    private String imageUrl;
    private int memberCount;
    private String adminUid;
    private String adminName;

    public Channel() {
    }

    public Channel(String id, String name, Long createTime, String category, String description, String imageUrl, int memberCount, String adminUid, String adminName) {
        this.id = id;
        this.name = name;
        this.createTime = createTime;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.memberCount = memberCount;
        this.adminUid = adminUid;
        this.adminName = adminName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getAdminUid() {
        return adminUid;
    }

    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    @Override
    public int compareTo(@NonNull Channel another) {
        if (createTime.longValue() == another.createTime.longValue()) {
            return name.compareTo(another.name);
        } else {
            return -1 * createTime.compareTo(another.createTime);
        }
    }
}
