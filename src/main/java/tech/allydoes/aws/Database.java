package tech.allydoes.aws;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class Database {
    public static final DynamoDbAsyncClient databaseClient =
            DynamoDbAsyncClient.builder()
                    .build();
}
