package com.telefonica.weblogic_kafka_integration.weblogic.util;

public class Util {

    public static String createASampleEvent() {
        String jsonString = "{\n" +
                "    \"creation_issue\": \"2024-10-03\",\n" +
                "    \"event_id\": \"597e8f46-c021-4a66-884c-20e2ba1ec0c5\",\n" +
                "    \"type\": \"UPDATE\",\n" +
                "    \"subtype\": \"IDENTIFIER\",\n" +
                "    \"version\": \"0\",\n" +
                "    \"data\": {\n" +
                "        \"creation_date\": \"2024-10-03\",\n" +
                "        \"payload\": {\n" +
                "            \"new_identifier.id\": \"265946425\",\n" +
                "            \"new_identifier.type\": \"1122334455\",\n" +
                "            \"notification_event_id\": \"597e8f46-c021-4a66-884c-20e2ba1ec0c5\",\n" +
                "            \"old_identifier.id\": \"163143603\",\n" +
                "            \"old_identifier.type\": \"\"\n" +
                "        },\n" +
                "        \"user_id\": \"163143603\"\n" +
                "    },\n" +
                "    \"publisher\": \"ESB\"\n" +
                "}";

        return jsonString;
    }

}
