package tech.allydoes.aws.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.model.*;
import tech.allydoes.aws.Attributes;
import tech.allydoes.aws.Database;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Moderation {
    private static final Logger LOGGER = LogManager.getLogger(Moderation.class);

    private static final String BAN_TABLE = "BansList";
    private static final String AWAITING_BAN_TIME = "NULL";
    private static final String BAN_EVADING_BAN_MESSAGE = "AUTOMATED: You have been banned for ban evading.";

    public static final long PERMANENT_BAN = -1;

    private static final ArrayList<BanProfile> bansToRemove = new ArrayList<>();
    private static final LinkedHashSet<BanProfile> bansToUpdate = new LinkedHashSet<>();

    public static CompletableFuture<BanProfile> getBanProfile(String hardwareID, String playerID, boolean banEvadingProtection) {
        QueryRequest hardwareIDQuery = QueryRequest.builder()
                .tableName(BAN_TABLE)
                .keyConditionExpression(Attributes.HARDWARE_ID + " = :hw")
                .expressionAttributeValues(Map.of(
                        ":hw", AttributeValue.fromS(hardwareID)
                ))
                .build();

        QueryRequest playerIDQuery = QueryRequest.builder()
                .tableName(BAN_TABLE)
                .indexName(Attributes.STEAM_ID + "Index")
                .keyConditionExpression(Attributes.STEAM_ID + " = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.fromS(playerID)
                ))
                .build();

        CompletableFuture<QueryResponse> hardwareIDResultFuture = Database.databaseClient.query(hardwareIDQuery);
        CompletableFuture<QueryResponse> playerIDResultFuture = Database.databaseClient.query(playerIDQuery);

        return hardwareIDResultFuture
                .thenCombine(playerIDResultFuture, (hardwareIDResponse, playerIDResponse) -> {
                    if (hardwareIDResponse.hasItems()) {
                        return parseBanProfile(hardwareIDResponse.items().getFirst());
                    }

                    if (playerIDResponse.hasItems()) {
                        return parseBanProfile(playerIDResponse.items().getFirst());
                    }

                    return null;
                })
                .thenCompose(banProfile -> {
                    if (banProfile == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    if (!banEvadingProtection || Objects.equals(banProfile.steamID(), playerID)) {
                        return CompletableFuture.completedFuture(banProfile);
                    }

                    return uploadBan(banProfile.hardwareID(), playerID, PERMANENT_BAN, BAN_EVADING_BAN_MESSAGE)
                            .thenApply(successful -> {
                                if (!successful) {
                                    return banProfile;
                                }

                                return new BanProfile(
                                        banProfile.hardwareID(),
                                        playerID,
                                        PERMANENT_BAN,
                                        BAN_EVADING_BAN_MESSAGE,
                                        PERMANENT_BAN
                                );
                            });
                })
                .exceptionally(error -> {
                    LOGGER.error("Unable to fetch ban profile", error);
                    return null;
                });
    }

    private static BanProfile parseBanProfile(Map<String, AttributeValue> item) {
        BanProfile banProfile = null;
        try {
            String hwID = item.get(Attributes.HARDWARE_ID).s();
            String steamID = item.get(Attributes.STEAM_ID).s();
            long banTime = Long.parseLong(item.get(Attributes.BAN_DURATION).s());
            String message = item.get(Attributes.BAN_MESSAGE).s();

            String bannedTillString = item.get(Attributes.BANNED_TILL).s();
            long bannedTill;
            if (Objects.equals(bannedTillString, AWAITING_BAN_TIME)) {
                bannedTill = -1;
            } else {
                bannedTill = Long.parseLong(bannedTillString);
            }

            banProfile = new BanProfile(hwID, steamID, banTime, message, bannedTill);
        } catch (NumberFormatException error) {
            LOGGER.error("Failed to parse ban profile", error);
        }

        return banProfile;
    }

    public static CompletableFuture<Boolean> checkBan(String hardwareID, String playerID, boolean updateBannedTill) {
        CompletableFuture<BanProfile> banProfileFuture = getBanProfile(hardwareID, playerID, updateBannedTill);
        return banProfileFuture.thenApply((banProfile) -> {
            if (banProfile == null) {
                return false;
            }

            if (updateBannedTill && (banProfile.till() == -1L && banProfile.banTime() != PERMANENT_BAN)) {
                bansToUpdate.add(banProfile);
                return true;
            }

            if (!isValidBannedTill(banProfile.till())) {
                bansToRemove.add(banProfile);
                return false;
            }

            return true;
        });
    }

    private static boolean isValidBannedTill(long unixBanTime) {
        if (unixBanTime == -1) {
            return true;
        }

        long currentUnixTime = Instant.now().getEpochSecond();
        return unixBanTime > currentUnixTime;
    }

    public static CompletableFuture<Boolean> uploadBan(String hardwareID, String playerID, long duration, String message) {
        Map<String, AttributeValue> keys = Map.of(
                Attributes.HARDWARE_ID, AttributeValue.fromS(hardwareID),
                Attributes.STEAM_ID, AttributeValue.fromS(playerID)
        );

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(BAN_TABLE)
                .key(keys)
                .updateExpression("SET #D = :duration, #M = :message, #T = :till")
                .expressionAttributeNames(Map.of(
                        "#D", Attributes.BAN_DURATION,
                        "#M", Attributes.BAN_MESSAGE,
                        "#T", Attributes.BANNED_TILL
                ))
                .expressionAttributeValues(Map.of(
                        ":duration", AttributeValue.fromS(String.valueOf(duration)),
                        ":message", AttributeValue.fromS(message),
                        ":till", AttributeValue.fromS("-1")
                ))
                .build();

        CompletableFuture<UpdateItemResponse> responseFuture = Database.databaseClient.updateItem(updateRequest);
        return responseFuture
                .thenApply(response -> true)
                .exceptionally(e -> {
                    LOGGER.error("Failed to upload ban", e);
                    return false;
                });
    }

    public static CompletableFuture<Boolean> updateBannedTill(String hardwareID, String playerID) {
        Map<String, AttributeValue> keys = Map.of(
                Attributes.HARDWARE_ID, AttributeValue.fromS(hardwareID),
                Attributes.STEAM_ID, AttributeValue.fromS(playerID)
        );

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(BAN_TABLE)
                .key(keys)
                .updateExpression("SET #B = :currentTime")
                .expressionAttributeNames(Map.of("#B", Attributes.BANNED_TILL))
                .expressionAttributeValues(Map.of(":currentTime", AttributeValue.fromS(String.valueOf(Instant.now().getEpochSecond()))))
                .build();

        CompletableFuture<UpdateItemResponse> responseFuture = Database.databaseClient.updateItem(updateRequest);
        return responseFuture
                .thenApply(response -> true)
                .exceptionally(e -> {
                    LOGGER.error("Failed to update banned till", e);
                    return false;
                });
    }

    public record BanProfile(String hardwareID, String steamID, long banTime, String message, long till) {}
}
