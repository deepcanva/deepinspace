package com.canva.deepinspace;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.canva.deepinspace.model.Order;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hello world
 */
public class App {
    List<String> productList = new ArrayList<>();
    Random random = new Random();

    public static AmazonKinesis getKinesisClient() {
        var credentialsProvider = createCredentialProvider("identity", "dev");

        return AmazonKinesisClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    private static AWSStaticCredentialsProvider createCredentialProvider(String baseProfile, String assumedProfile) {
        var profilesConfigFile = new ProfilesConfigFile();
        var credentialsProvider = new ProfileCredentialsProvider(profilesConfigFile, baseProfile);
        var credentials = credentialsProvider.getCredentials();

        Map<String, BasicProfile> basicProfiles = profilesConfigFile.getAllBasicProfiles();
        var assumedRoleProfile = basicProfiles.get(assumedProfile);
        if (assumedRoleProfile == null) {
            throw new IllegalStateException("Could not find role profile '" + assumedProfile
                    + "' in config file; options are: " + basicProfiles.keySet());
        }
        Credentials assumedCredentials = assumeProfile(assumedRoleProfile, new AWSStaticCredentialsProvider(credentials));
        return new AWSStaticCredentialsProvider(new BasicSessionCredentials(assumedCredentials.getAccessKeyId(), assumedCredentials.getSecretAccessKey(), assumedCredentials.getSessionToken()));
    }

    static Credentials assumeProfile(BasicProfile basicProfile, AWSCredentialsProvider identityCredentialProvider) {
        var sts = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(basicProfile.getRegion())
                .withCredentials(identityCredentialProvider)
                .build();

        int durationSeconds = Integer.parseInt(basicProfile.getPropertyValue("duration"));
        AssumeRoleRequest roleRequest = new AssumeRoleRequest() //
                .withRoleArn(basicProfile.getRoleArn())
                .withRoleSessionName(basicProfile.getRoleSessionName())
                .withDurationSeconds(durationSeconds);

        var roleResult = sts.assumeRole(roleRequest);
        return roleResult.getCredentials();
    }


    public static void main(String[] args) throws InterruptedException {
        AmazonKinesis kinesisClient = getKinesisClient();

        App app = new App();
        app.populateProductList();
        //1. get client
        app.sendData(kinesisClient);
    }

    private void sendData(AmazonKinesis kinesisClient) {
        //2. PutRecordRequest
        PutRecordsRequest recordsRequest = new PutRecordsRequest();
        recordsRequest.setStreamName("deep-stream");
        recordsRequest.setRecords(getRecordsRequestList());

        //3. putRecord or putRecords - 500 records with single API call
        PutRecordsResult results = kinesisClient.putRecords(recordsRequest);
        if (results.getFailedRecordCount() > 0) {
            System.out.println("Error occurred for records " + results.getFailedRecordCount());
        } else {
            System.out.println("Data sent successfully...");
        }

    }

    private List<PutRecordsRequestEntry> getRecordsRequestList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<PutRecordsRequestEntry> putRecordsRequestEntries = new ArrayList<>();
        for (Order order : getOrderList()) {
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setData(ByteBuffer.wrap(gson.toJson(order).getBytes()));
            requestEntry.setPartitionKey(UUID.randomUUID().toString());
            putRecordsRequestEntries.add(requestEntry);
        }
        return putRecordsRequestEntries;
    }

    private List<Order> getOrderList() {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Order order = new Order();
            order.setOrderId(random.nextInt());
            order.setProduct(productList.get(random.nextInt(productList.size())));
            order.setQuantity(random.nextInt(20));
            orders.add(order);
        }
        return orders;
    }

    private void populateProductList() {
        productList.add("shirt");
        productList.add("t-shirt");
        productList.add("shorts");
        productList.add("tie");
        productList.add("shoes");
        productList.add("jeans);");
        productList.add("belt");
    }


}
