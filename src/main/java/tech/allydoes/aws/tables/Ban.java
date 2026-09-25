package tech.allydoes.aws.tables;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
public class Ban {
    private String Banned;
    private String SteamID;
    private String BanTime;
    private String Message;
    private String Till;

    @DynamoDbPartitionKey
    public String getBanned() {
        return Banned;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "SteamIDIndex")
    public String getSteamID() {
        return SteamID;
    }

    public String getBanTime() {
        return BanTime;
    }

    public String getMessage() {
        return Message;
    }

    public String getTill() {
        return Till;
    }

    ///////////////////////////////////////////////////////////////////////////

    public void setBanned(String banned) {
        Banned = banned;
    }

    public void setSteamID(String steamID) {
        SteamID = steamID;
    }

    public void setBanTime(String banTime) {
        BanTime = banTime;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public void setTill(String till) {
        Till = till;
    }
}
