package tech.allydoes.aws.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import tech.allydoes.aws.Attributes;
import tech.allydoes.aws.Database;

import java.util.List;
import java.util.Map;

public class Moderation {
    private static final Logger LOGGER = LogManager.getLogger(Moderation.class);

    private static final String BAN_TABLE = "BansList";

    public static void IsBanned(String hardwareID, String playerID) {
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

        try {
            Database.databaseClient.query(hardwareIDQuery).whenComplete((response, throwable) -> {
                List<Map<String, AttributeValue>> hardwareIDBans = response.items();
                
            })
        }
    }
}
