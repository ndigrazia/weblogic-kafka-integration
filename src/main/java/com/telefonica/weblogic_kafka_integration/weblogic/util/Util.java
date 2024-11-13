package com.telefonica.weblogic_kafka_integration.weblogic.util;

public class Util {

    public static String createASampleEvent() {
        String jsonString = "{\n" +
            "    \"specversion\": \"1.0\",\n" +
            "    \"id\": \"597e8f46-c021-4a66-884c-20e2ba1ec0c5\",\n" +
            "    \"source\": \"urn:telefonica:at3osbc:jms:ob-eventsqueue\",\n" +
            "    \"type\": \"telefonica.event.identifier.updated.v1\",\n" +
            "    \"datacontenttype\": \"application/json\",\n" +
            "    \"time\": \"2024-10-03T00:00:00Z\",\n" +
            "    \"data\": {\n" +
            "        \"newIdentifier\": {\n" +
            "            \"id\": \"265946425\",\n" +
            "            \"type\": \"1122334455\"\n" +
            "        },\n" +
            "        \"oldIdentifier\": {\n" +
            "            \"id\": \"163143603\",\n" +
            "            \"type\": \"1122334455\"\n" +
            "        },\n" +
            "        \"userId\": \"163143603\"\n" +
            "    }\n" +
            "}";

        return jsonString;
    }

}
