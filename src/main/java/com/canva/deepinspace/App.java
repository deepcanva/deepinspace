package com.canva.deepinspace;

// Version 1 AWS Credentials Deprecated
//import com.amazonaws.auth.*;
//import com.amazonaws.auth.profile.ProfileCredentialsProvider;
//import com.amazonaws.auth.profile.ProfilesConfigFile;
//import com.amazonaws.auth.profile.internal.BasicProfile;
//import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
//import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
//import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.canva.deepinspace.model.Record;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Hello world
 */
public class App {
    private List<String> recordList = new ArrayList<>();
    private Random random = new Random();
    private static StaticCredentialsProvider credentialsProvider = null;
    private static Profile assumedRoleProfile = null;

    public static KinesisAsyncClient getKinesisClient() {
        if (credentialsProvider == null) {
            credentialsProvider = createCredentialProvider("identity", "dev");
        }

        // TODO(yolanda): not compatible with AWSStaticCredentialsProvider in Version1, StaticCredentialsProvider in Version2
        // none of the authentication classes carry over ... need to reconstruct the whole thing
        return KinesisAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();
    }

    public static LambdaClient getLambdaClient() {
        if (credentialsProvider == null) {
            credentialsProvider = createCredentialProvider("identity", "dev");
        }

        return LambdaClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();
    }

    private static StaticCredentialsProvider createCredentialProvider(String baseProfile, String assumedProfile) {
        var profilesFile = ProfileFile.builder().build();
        var credentialsProvider = ProfileCredentialsProvider.create(baseProfile); // created with default ProfileFile and baseProfile name
        var credentials = credentialsProvider.resolveCredentials();

        Map<String, Profile> profiles = profilesFile.profiles();
        assumedRoleProfile = profiles.get(assumedProfile); // save role profile to create aws lambda function
        if (assumedRoleProfile == null) {
            throw new IllegalStateException("Could not find role profile '" + assumedProfile
                    + "' in config file; options are: " + profiles.keySet());
        }
        Credentials assumedCredentials = assumeProfile(assumedRoleProfile, StaticCredentialsProvider.create(credentials));
        return StaticCredentialsProvider.create(AwsSessionCredentials.create(assumedCredentials.accessKeyId(), assumedCredentials.secretAccessKey(), assumedCredentials.sessionToken()));
    }

    static Credentials assumeProfile(Profile basicProfile, AwsCredentialsProvider identityCredentialProvider) {
        var sts = StsClient.builder()
                .region(Region.of(basicProfile.property(ProfileProperty.REGION).get()))
                .credentialsProvider(identityCredentialProvider)
                .build();

        int durationSeconds = Integer.parseInt(basicProfile.property("duration").get());
        AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                .roleArn(basicProfile.property(ProfileProperty.ROLE_ARN).get())
                .roleSessionName(basicProfile.property(ProfileProperty.ROLE_SESSION_NAME).get())
                .durationSeconds(durationSeconds)
                .build();

        var roleResult = sts.assumeRole(roleRequest);
        return roleResult.credentials();
    }

    // VERSION 1 AWS CREDENTIALS DEPRECATE
//    private static AWSStaticCredentialsProvider createCredentialProvider(String baseProfile, String assumedProfile) {
//        var profilesConfigFile = new ProfilesConfigFile();
//        var credentialsProvider = new ProfileCredentialsProvider(profilesConfigFile, baseProfile);
//        var credentials = credentialsProvider.getCredentials();
//
//        Map<String, BasicProfile> basicProfiles = profilesConfigFile.getAllBasicProfiles();
//        assumedRoleProfile = basicProfiles.get(assumedProfile); // save role profile to create aws lambda function
//        if (assumedRoleProfile == null) {
//            throw new IllegalStateException("Could not find role profile '" + assumedProfile
//                    + "' in config file; options are: " + basicProfiles.keySet());
//        }
//        Credentials assumedCredentials = assumeProfile(assumedRoleProfile, new AWSStaticCredentialsProvider(credentials));
//        return new AWSStaticCredentialsProvider(new BasicSessionCredentials(assumedCredentials.getAccessKeyId(), assumedCredentials.getSecretAccessKey(), assumedCredentials.getSessionToken()));
//    }
//    static Credentials assumeProfile(BasicProfile basicProfile, AWSCredentialsProvider identityCredentialProvider) {
//        var sts = AWSSecurityTokenServiceClientBuilder.standard()
//                .withRegion(basicProfile.getRegion())
//                .withCredentials(identityCredentialProvider)
//                .build();
//
//        int durationSeconds = Integer.parseInt(basicProfile.getPropertyValue("duration"));
//        AssumeRoleRequest roleRequest = new AssumeRoleRequest() //
//                .withRoleArn(basicProfile.getRoleArn())
//                .withRoleSessionName(basicProfile.getRoleSessionName())
//                .withDurationSeconds(durationSeconds);
//
//        var roleResult = sts.assumeRole(roleRequest);
//        return roleResult.getCredentials();
//    }
    // END VERSION 1 AWS CREDENTIALS DEPRECATE

    private CreateFunctionResponse createLambdaFunction(LambdaClient lambdaClient) {
        CreateFunctionRequest request = CreateFunctionRequest.builder()
                .functionName("deep-consumer")
                .runtime("java11")
                // actual arn of the execution role you created
                .role(assumedRoleProfile.property(ProfileProperty.ROLE_ARN).get())
                // name of your source file and then name of function handler
                .handler("ProcessKinesisRecords.handleRecord")
                .build();
        return lambdaClient.createFunction(request);
    }

    // TODO(yolanda): configure kinesis stream to register consumer and subscribe to shard
    private void registerLambdaConsumer(KinesisAsyncClient kinesisClient, LambdaClient lambdaClient, CreateFunctionResponse lambdaFunctionResult) throws ExecutionException, InterruptedException {
        // Kinesis stream description
        DescribeStreamRequest kinesisStreamRequest = DescribeStreamRequest.builder()
                .streamName("deep-stream")
                .build();
        DescribeStreamResponse kinesisStream = kinesisClient.describeStream(kinesisStreamRequest).get();
        StreamDescription kinesisStreamDescription = kinesisStream.streamDescription();

        // Register stream consumer
        RegisterStreamConsumerRequest streamConsumerRequest = RegisterStreamConsumerRequest.builder()
                .consumerName("deep-consumer")
                .streamARN(lambdaFunctionResult.functionArn()) // Created Lambda's ARN
                .build();
        RegisterStreamConsumerResponse result = kinesisClient.registerStreamConsumer(streamConsumerRequest).get();

        // Find first shard to subscribe to
        ListShardsRequest shardsRequest = ListShardsRequest.builder()
                .streamName("deep-stream")
                .build();
        ListShardsResponse shardsResult = kinesisClient.listShards(shardsRequest).get();
        List<Shard> shardsList = shardsResult.shards(); // shardsResult.hasShards() test empty
        Shard firstShard = shardsList.get(0);

        // Create subscribe to shard request
        SubscribeToShardRequest subscribeShardRequest = SubscribeToShardRequest.builder()
                .consumerARN(result.consumer().consumerARN()) // Registered Lambda CONSUMER ARN
                .shardId(firstShard.shardId()) // First shard's id
                .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON))
                .build();

        // Create subscribe to shard response handler
        // TODO(yolanda): Figure out what this does
        SubscribeToShardResponseHandler subscribeShardResponseHandler = SubscribeToShardResponseHandler
                .builder()
                .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
                .build();
        kinesisClient.subscribeToShard(subscribeShardRequest, subscribeShardResponseHandler).get();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // 1. Get kinesis producer client and send data
        KinesisAsyncClient kinesisClient = getKinesisClient();
        App app = new App();
        app.populateRecordList();
        app.sendData(kinesisClient);

        // 5. Get lambda consumer client
        LambdaClient lambdaClient = getLambdaClient();

        // 6. Create lambda function
        CreateFunctionResponse lambdaFunctionResult = app.createLambdaFunction(lambdaClient);

        // 7. Subscribe lambda function to consume from kinesis stream
        app.registerLambdaConsumer(kinesisClient, lambdaClient, lambdaFunctionResult);

        System.out.println("Success: Main finished execution!");
    }

    private void sendData(KinesisAsyncClient kinesisClient) throws ExecutionException, InterruptedException {
        //2. PutRecordRequest
        PutRecordsRequest recordsRequest = PutRecordsRequest.builder()
                .streamName("deep-stream")
                .records(getRecordsRequestList())
                .build();

        //3. putRecord or putRecords - 500 records with single API call
        PutRecordsResponse results = kinesisClient.putRecords(recordsRequest).get();
        if (results.failedRecordCount() > 0) {
            System.out.println("Error occurred for records " + results.failedRecordCount());
        } else {
            System.out.println("Data sent successfully...");
        }

    }

    private List<PutRecordsRequestEntry> getRecordsRequestList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<PutRecordsRequestEntry> putRecordsRequestEntries = new ArrayList<>();
        for (Record record : getRecordList()) {
            PutRecordsRequestEntry requestEntry = PutRecordsRequestEntry.builder()
                    .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(gson.toJson(record).getBytes())))
                    .partitionKey(UUID.randomUUID().toString())
                    .build();
            putRecordsRequestEntries.add(requestEntry);
        }
        return putRecordsRequestEntries;
    }

    private List<Record> getRecordList(){
        List<Record> records = new ArrayList<>();
        for(int i=0;i<500;i++){
            Record record = new Record();
            record.setRecordId(random.nextInt());
            record.setHandler(recordList.get(random.nextInt(recordList.size())));
            record.setStatus("5xx");
            records.add(record);
        }
        return records;
    }

    /**
     * Sample of Handlers in canva.rpc.timer metric
     */
    private void populateRecordList(){
        recordList.add("documents");
        recordList.add("comments");
        recordList.add("media");
        recordList.add("product");
        recordList.add("vfolders");
        recordList.add("export");
        recordList.add("ripple");
    }
}
