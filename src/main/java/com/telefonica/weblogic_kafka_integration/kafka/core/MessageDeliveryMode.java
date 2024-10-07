package com.telefonica.weblogic_kafka_integration.kafka.core;

public enum MessageDeliveryMode {
   
    NONE(0), SYNC(1), ASYNC(2);

    private final int number;

    private MessageDeliveryMode(int number) {
        this.number =  number;    
    }

    public int toInt(MessageDeliveryMode mode) {
       return mode.modeAsNumber();
    }

    public MessageDeliveryMode fromInt(int modeAsNumber) {
        try {
            return MessageDeliveryMode.values()[modeAsNumber];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid mode number: " + modeAsNumber);
        }
    }

    public int modeAsNumber() {
        return number;
    }

}
