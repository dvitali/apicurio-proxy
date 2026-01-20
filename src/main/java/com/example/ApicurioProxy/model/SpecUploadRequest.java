package com.example.ApicurioProxy.model;

public class SpecUploadRequest {
    private String groupid;
    private String artifactid;
    private String filename;

    public SpecUploadRequest() {
    }

    public SpecUploadRequest(String groupid, String artifactid, String filename) {
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.filename = filename;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public String getArtifactid() {
        return artifactid;
    }

    public void setArtifactid(String artifactid) {
        this.artifactid = artifactid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
