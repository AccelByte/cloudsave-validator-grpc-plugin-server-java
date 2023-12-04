package net.accelbyte.cloudsave.validator.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerActivity extends Model {

    @JsonProperty
    @NotBlank(message = "user ID cannot be empty")
    private String userId;

    @JsonProperty
    @NotBlank(message = "activity cannot be empty")
    private String activity;

}
