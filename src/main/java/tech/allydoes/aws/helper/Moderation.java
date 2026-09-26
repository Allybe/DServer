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
import java.util.concurrent.CompletableFuture;

public class Moderation {
    private static final Logger LOGGER = LogManager.getLogger(Moderation.class);

    private static final String BAN_TABLE = "BansList";
    private static final String AWAITING_BAN_TIME = "NULL";

    private static final ArrayList<String> bansToRemove = new ArrayList<>();
    private static final LinkedHashSet<String> bansToUpdate = new LinkedHashSet<>();

    public static CompletableFuture<Boolean> checkBanAsync(String hardwareID, String playerID, boolean updateBannedTill) {
        QueryRequest hardwareIDQuery = QueryRequest.builder()
                .tableName(BAN_TABLE)
                .keyConditionExpression(Attributes.HARDWARE_ID + " = :hw")
                .expressionAttributeValues(Map.of(
                        ":hw", AttributeValue.fromS(hardwareID)
                ))
                .build();

        QueryRequest playerIDQuery = QueryRequest.builder()
                .tableName(BAN_TABLE)
                .indexName(Attributes.STEAM_ID + "index")
                .keyConditionExpression(Attributes.STEAM_ID + " = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.fromS(playerID)
                ))
                .build();

        CompletableFuture<QueryResponse> hardwareIDResultFuture = Database.databaseClient.query(hardwareIDQuery);
        CompletableFuture<QueryResponse> playerIDResultFuture = Database.databaseClient.query(playerIDQuery);

        return hardwareIDResultFuture.thenCombine(playerIDResultFuture, (hardwareIDResponse, playerIDResponse) -> processAndCheckPlayerBan(updateBannedTill, hardwareIDResponse) || processAndCheckPlayerBan(updateBannedTill, playerIDResponse)).exceptionally(e -> {
            LOGGER.error("Failed to query bans list", e);
            return false;
        });
    }

    private static boolean processAndCheckPlayerBan(boolean updateBannedTill, QueryResponse playerIDResponse) {
        for (Map<String, AttributeValue> item : playerIDResponse.items()) {
            String bannedTill = item.get(Attributes.BANNED_TILL).s();
            if (updateBannedTill && bannedTill.equalsIgnoreCase(AWAITING_BAN_TIME)) {
                bansToUpdate.add(bannedTill);
            }

            if (isValidBanTime(bannedTill)) {
                return true;
            } else {
                bansToRemove.add(item.get(Attributes.HARDWARE_ID).s());
            }
        }
        return false;
    }

    private static boolean isValidBanTime(String banTime) {
        if (banTime.equalsIgnoreCase(AWAITING_BAN_TIME)) {
            return true;
        }

        long unixBanTime;
        try {
            unixBanTime = Long.parseLong(banTime);
        } catch (NumberFormatException e) {
            LOGGER.error("Unable to parse bannedTill as a long", e);
            return false;
        }

        long currentUnixTime = Instant.now().getEpochSecond();
        return unixBanTime > currentUnixTime;
    }

    public static CompletableFuture<Boolean> uploadBan(String hardwareID, String playerID, long duration, String message) {
        Map<String, AttributeValue> keys = Map.of(
                Attributes.HARDWARE_ID, AttributeValue.fromS(hardwareID)
        );

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(BAN_TABLE)
                .key(keys)
                .updateExpression("SET #S = ")
                .expressionAttributeNames(Map.of(
                        "#S", Attributes.STEAM_ID,
                        "#D", Attributes.BAN_DURATION,
                        "#M", Attributes.BAN_MESSAGE,
                        "#T", Attributes.BANNED_TILL
                ))
                .expressionAttributeValues(Map.of(
                        ":steamID", AttributeValue.fromN(playerID),
                        ":duration", AttributeValue.fromS(String.valueOf(duration)),
                        ":message", AttributeValue.fromS(message),
                        ":till", AttributeValue.fromS(AWAITING_BAN_TIME)
                ))
                .build();

        try {
            CompletableFuture<UpdateItemResponse> responseFuture = Database.databaseClient.updateItem(updateRequest);
            return responseFuture.thenApply((response) -> {
                return response.hasAttributes();
            });

        } catch (DynamoDbException e) {
            LOGGER.error("Failed to upload ban", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
