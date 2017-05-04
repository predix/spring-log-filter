package com.ge.predix.vcap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class VcapApplication {

    @JsonProperty("application_id")
    private String appId;

    @JsonProperty("application_name")
    private String appName;

    @JsonProperty("instance_id")
    private String instanceId;

    @JsonProperty("instance_index")
    private String instanceIndex;
}
