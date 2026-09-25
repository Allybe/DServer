package tech.allydoes.aws.helper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import tech.allydoes.aws.Database;
import tech.allydoes.aws.tables.Ban;

public class Moderation {
    DynamoDbAsyncTable<Ban> banTable = Database.databaseClient.table("Bans", TableSchema.fromBean(Ban.class));

    public static void IsBanned(String id, String hardwareID) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo()

        QueryEnhancedRequest query = QueryEnhancedRequest.builder().build();
        query.
    }
}
