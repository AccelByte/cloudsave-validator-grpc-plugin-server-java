package net.accelbyte.cloudsave.validator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyMessage extends Model {

    @JsonProperty
    private String message;

    @JsonProperty
    private String title;

    @JsonProperty
    private Instant availableOn;

}
