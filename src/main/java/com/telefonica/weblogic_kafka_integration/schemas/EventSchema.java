package com.telefonica.weblogic_kafka_integration.schemas;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;

public class EventSchema {

    @JsonAlias(value = "event_id")
    String eventId;
    @JsonAlias(value = "creation_issue")
    String creationIssue;
    Type type;
    SubType subtype;
    String version;
    JsonNode data;
    String publisher;

    public EventSchema() {
    }

    public String getEventId() {
        return eventId;
    }
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCreationIssue() {
        return creationIssue;
    }

    public void setCreationIssue(String creationIssue) {
        this.creationIssue = creationIssue;
    }   
   
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }

    public SubType getSubtype() {
        return subtype;
    }
    public void setSubtype(SubType subtype) {
        this.subtype = subtype;
    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public JsonNode getData() {
        return data;
    }
    public void setData(JsonNode data) {
        this.data = data;
    }

    public String getPublisher() {
        return publisher;
    }
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    
    public enum Type {
        ADD,
        REMOVE,
        UPDATE;
    }

    public enum SubType {
        USER,
        USER_IDENTIFIER,
        IDENTIFIER;
    }

}
