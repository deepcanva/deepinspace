package com.canva.deepinspace;

// TODO(yolanda): find and fix the dependencies
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import com.amazonaws.services.lambda.runtime.Context;

/**
 * Simple Lambda Function Handler
 */
public class ProcessKinesisRecords {
    // TODO(yolanda): add lambda client? resubscribe to the stream etc???

    public Void handleRecord(SubscribeToShardEvent event, Context context)
    {
        // TODO(yolanda): do some aggregations / processing
        for(Record rec : event.records()) {
            System.out.println(rec.data().toString());
        }
        return null;
    }
}