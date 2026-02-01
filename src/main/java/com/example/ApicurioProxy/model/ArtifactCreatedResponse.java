package com.example.ApicurioProxy.model;

/**
 * Response object returned when an artifact is successfully created in Apicurio Registry.
 */
public class ArtifactCreatedResponse {

    private String groupId;
    private String artifactId;
    private String artifactVersion;
    private String mockedEndpoint;

    public ArtifactCreatedResponse() {
    }

    public ArtifactCreatedResponse(String groupId, String artifactId, String artifactVersion, String mockedEndpoint) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.artifactVersion = artifactVersion;
        this.mockedEndpoint = mockedEndpoint;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getMockedEndpoint() {
        return mockedEndpoint;
    }

    public void setMockedEndpoint(String mockedEndpoint) {
        this.mockedEndpoint = mockedEndpoint;
    }
}
