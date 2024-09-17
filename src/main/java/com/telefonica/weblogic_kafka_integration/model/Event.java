package com.telefonica.weblogic_kafka_integration.model;

public class Event {

    String event_id;
    String creation_issue;
    Type type;
    SubType subtype;
    String version;
    String data;
    String publisher;

    public Event(String event_id, String creation_issue, 
            Type type, SubType subtype, String version, 
                String data, String publisher) {
        this.event_id = event_id;
        this.creation_issue = creation_issue;
        this.type = type;
        this.subtype = subtype;
        this.version = version;
        this.data = data;
        this.publisher = publisher;
    }
    
    public Event() {
    }

    public String getEvent_id() {
        return event_id;
    }
    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }

    public String getCreation_issue() {
        return creation_issue;
    }
    public void setCreation_issue(String creation_issue) {
        this.creation_issue = creation_issue;
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

    public String getData() {
        return data;
    }
    public void setData(String data) {
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
