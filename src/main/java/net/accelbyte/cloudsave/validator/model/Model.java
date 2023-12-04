package net.accelbyte.cloudsave.validator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;

public abstract class Model {

    @JsonIgnore
    public Set<ConstraintViolation<Model>> validate(Validator validator) {
            return validator.validate(this);
    }

}
