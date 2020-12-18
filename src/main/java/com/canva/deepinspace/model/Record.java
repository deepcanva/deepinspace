package com.canva.deepinspace.model;

/**
 * Alert Record
 */
public class Record {
    int recordId;
    String handler;
    String status;

    public int getRecordId() {
        return recordId;
    }

    public String getHandler() {
        return handler;
    }

    public String getStatus() {
        return status;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}