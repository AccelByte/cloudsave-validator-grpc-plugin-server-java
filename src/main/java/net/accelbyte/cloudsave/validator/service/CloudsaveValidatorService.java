package net.accelbyte.cloudsave.validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.cloudsave.validator.Error;
import net.accelbyte.cloudsave.validator.*;
import net.accelbyte.cloudsave.validator.model.CustomGameRecord;
import net.accelbyte.cloudsave.validator.model.CustomPlayerRecord;
import net.accelbyte.cloudsave.validator.model.DailyMessage;
import net.accelbyte.cloudsave.validator.model.PlayerActivity;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@GRpcService
public class CloudsaveValidatorService extends CloudsaveValidatorServiceGrpc.CloudsaveValidatorServiceImplBase {

    private final ObjectMapper objectMapper;

    private final Validator validator;

    public CloudsaveValidatorService(
        @Autowired ObjectMapper objectMapper,
        @Autowired Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void beforeWriteGameRecord(GameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (request.getKey().startsWith("map")) {
            CustomGameRecord record;
            try {
                record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomGameRecord.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
            var res = record.validate(validator);
            if (!res.isEmpty()) {
                String invalidFields = res.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining(";"));
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                        .build();

                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
        }

        responseGameRecordSuccess(responseObserver, request.getKey());
    }

    @Override
    public void afterReadGameRecord(GameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (request.getKey().startsWith("daily_msg")) {
            DailyMessage message;
            try {
                message = objectMapper.readValue(request.getPayload().toStringUtf8(), DailyMessage.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }
            if (message.getAvailableOn().isBefore(Instant.now())) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(2)
                        .setErrorMessage("not accessible yet")
                        .build();

                responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
                return;
            }

        }

        responseGameRecordSuccess(responseObserver, request.getKey());
    }

    @Override
    public void afterBulkReadGameRecord(BulkGameRecord request, StreamObserver<BulkGameRecordValidationResult> responseObserver) {
        var result = request.getGameRecordsList().stream().map(it ->  {
            var success = GameRecordValidationResult.newBuilder()
                    .setIsSuccess(true)
                    .setKey(it.getKey())
                    .build();

            if (!it.getKey().startsWith("daily_msg")) {
                return success;
            }

            DailyMessage message;
            try {
                message = objectMapper.readValue(it.getPayload().toStringUtf8(), DailyMessage.class);
            } catch (Exception e) {
                var errorDetail = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                        .build();
                return GameRecordValidationResult.newBuilder()
                        .setIsSuccess(false)
                        .setKey(it.getKey())
                        .setError(errorDetail)
                        .build();
            }
            if (!message.getAvailableOn().isBefore(Instant.now())) {
                return success;
            }

            // failed
            var errorDetail = Error.newBuilder()
                    .setErrorCode(2)
                    .setErrorMessage("not accessible yet")
                    .build();

            return GameRecordValidationResult.newBuilder()
                    .setIsSuccess(false)
                    .setKey(it.getKey())
                    .setError(errorDetail)
                    .build();
        }).toList();

        var response = BulkGameRecordValidationResult.newBuilder()
                .addAllValidationResults(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void beforeWritePlayerRecord(PlayerRecord request,
            StreamObserver<PlayerRecordValidationResult> responseObserver) {
        if (request.getKey().startsWith("favourite_weapon")) {
            try {
                CustomPlayerRecord record;
                try {
                    record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomPlayerRecord.class);
                } catch (Exception e) {
                    var errorDetail = Error.newBuilder()
                            .setErrorCode(1)
                            .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                            .build();
                    responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
                    return;
                }
                var result = record.validate(validator);
                if (!result.isEmpty()) {
                    var failedValidations = result.stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining(";"));
                    var error = Error.newBuilder()
                            .setErrorCode(1)
                            .setErrorMessage("Validation failed: [%s]".formatted(failedValidations))
                            .build();
                    responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), error);
                    return;
                }
            } catch (Exception ex) {
                var error = Error.newBuilder()
                        .setErrorCode(1)
                        .setErrorMessage("Parsing failed")
                        .build();
                responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), error);
                return;
            }
        }
        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    @Override
    public void afterReadPlayerRecord(PlayerRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    @Override
    public void afterBulkReadPlayerRecord(BulkPlayerRecord request, StreamObserver<BulkPlayerRecordValidationResult> responseObserver) {
        var result = request.getPlayerRecordsList().stream()
                .map(it -> PlayerRecordValidationResult.newBuilder()
                        .setIsSuccess(true)
                        .setKey(it.getKey())
                        .setUserId(it.getUserId())
                        .build()
                ).toList();

        var response = BulkPlayerRecordValidationResult.newBuilder()
                .addAllValidationResults(result)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void beforeWriteAdminGameRecord(AdminGameRecord request, StreamObserver<GameRecordValidationResult> responseObserver) {
        if (!request.getKey().startsWith("map")) {
            responseGameRecordSuccess(responseObserver, request.getKey());
            return;
        }

        CustomGameRecord record;
        try {
            record = objectMapper.readValue(request.getPayload().toStringUtf8(), CustomGameRecord.class);
        } catch (Exception e) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                    .build();
            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
            return;
        }
        var res = record.validate(validator);
        if (!res.isEmpty()) {
            String invalidFields = res.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(";"));
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                    .build();

            responseGameRecordFailed(responseObserver, request.getKey(), errorDetail);
            return;
        }

        responseGameRecordSuccess(responseObserver, request.getKey());

    }

    @Override
    public void beforeWriteAdminPlayerRecord(AdminPlayerRecord request, StreamObserver<PlayerRecordValidationResult> responseObserver) {
        if (!request.getKey().startsWith("player_activity")) {
            responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
            return;
        }

        PlayerActivity activity;
        try {
            activity = objectMapper.readValue(request.getPayload().toStringUtf8(), PlayerActivity.class);
        } catch (Exception e) {
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(e.getMessage()))
                    .build();
            responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
            return;
        }
        var res = activity.validate(validator);

        if (!res.isEmpty()) {
            String invalidFields = res.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(";"));
            var errorDetail = Error.newBuilder()
                    .setErrorCode(1)
                    .setErrorMessage("Parsing failed: [%s]".formatted(invalidFields))
                    .build();
            responsePlayerRecordFailed(responseObserver, request.getKey(), request.getUserId(), errorDetail);
            return;
        }

        responsePlayerRecordSuccess(responseObserver, request.getKey(), request.getUserId());
    }

    private void responseGameRecordSuccess(StreamObserver<GameRecordValidationResult> responseObserver, String key) {
        var response =  GameRecordValidationResult.newBuilder()
                .setIsSuccess(true)
                .setKey(key)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    private void responseGameRecordFailed(StreamObserver<GameRecordValidationResult> responseObserver, String key, Error errorDetail) {
        var response = GameRecordValidationResult.newBuilder()
                .setIsSuccess(false)
                .setKey(key)
                .setError(errorDetail)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void responsePlayerRecordFailed(
            StreamObserver<PlayerRecordValidationResult> responseObserver, String key, String userId, Error errorDetail
    ) {
        var response = PlayerRecordValidationResult.newBuilder()
                .setIsSuccess(false)
                .setKey(key)
                .setUserId(userId)
                .setError(errorDetail)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void responsePlayerRecordSuccess(
            StreamObserver<PlayerRecordValidationResult> responseObserver, String key, String userId
    ) {
        var response = PlayerRecordValidationResult.newBuilder()
                .setIsSuccess(true)
                .setKey(key)
                .setUserId(userId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
