package com.example.ApicurioProxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpecUploadRequest {
    @JsonProperty("groupid")
    private String groupid;
    @JsonProperty("artifactid")
    private String artifactid;
    @JsonProperty("filename")
    private String filename;
    @JsonProperty("content")
    private String content;
    @JsonProperty("contentType")
    private String contentType;
    @JsonProperty("mockedEndpoint")
    private boolean mockedEndpoint;

    public SpecUploadRequest() {
    }

    public SpecUploadRequest(String groupid, String artifactid, String filename, String content, String contentType, boolean mockedEndpoint) {
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.filename = filename;
        this.content = content;
        this.contentType = contentType;
        this.mockedEndpoint = mockedEndpoint;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isMockedEndpoint() {
        return mockedEndpoint;
    }

    public void setMockedEndpoint(boolean mockedEndpoint) {
        this.mockedEndpoint = mockedEndpoint;
    }
}
