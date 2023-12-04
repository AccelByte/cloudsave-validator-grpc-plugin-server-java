package net.accelbyte.cloudsave.validator.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomGameRecord extends Model {

    @JsonProperty
    @NotBlank(message = "locationId cannot be empty")
    private String locationId;

    @JsonProperty
    @NotBlank(message = "name cannot be empty")
    private String name;

    @NotNull(message = "totalResource cannot be empty")
    @JsonProperty
    private Integer totalResource;

    @JsonProperty
    @NotNull(message = "totalEnemy cannot be empty")
    private Integer totalEnemy;

}
