### Event

```json
{
	"creation_issue": "<TIMESTAMP>",// The time when the event was created or occurred.
	"event_id": "<UUID>", //It's the value in the notification_event_id field.
	"type": "{ADD|REMOVE|UPDATE}", // 
	"subtype": "{USER|USER-IDENTIFIER|IDENTIFIER}",
	"version": "0"
	"data": {
		//The payload content depends on event types. See event types below.
	},
	"publisher": "ESB"
}
```

## Event types:

### Publish new user event

1. Type: ADD, Subtype: USER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"notification_event_id": "${event_id}"
	},
	"user_id": "string"
}
```

### Publish user removal event

1. Type: REMOVE, Subtype: USER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"notification_event_id": "${event_id}"
	},
	"user_id": "<string>"
}
```

### Publish new user identifier event

1. Type: ADD, Subtype: USER-IDENTIFIER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"identifier.id": "<string>",
		"identifier.type": "<phone_number>",
		"notification_event_id": "${event_id}"
	},
	"user_id": "<string>"
}
```

### Publish user identifier removal event

1. Type: REMOVE, Subtype: USER-IDENTIFIER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"identifier.id": "<string>",
		"identifier.type": "<uid>",
		"notification_event_id": "${event_id}"
	},
	"user_id": "<string>"
}
```

### Publish identifier user change event

1. Type: UPDATE, Subtype: USER-IDENTIFIER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"identifier.id": "<string>",
		"identifier.type": "<phone_number>",
		"notification_event_id": "${event_id}"
	},
	"user_id": "<string>"
}
```

### Publish user identifier change event

1. Type: UPDATE, Subtype: IDENTIFIER
2. Payload:

```json
{
	"creation_date": "${creation_issue}",
	"payload": {
		"new_identifier.id": "<string>",
		"new_identifier.type": "<phone_number>",
		"notification_event_id": "${event_id}",
		"old_identifier.id": "<string>",
		"old_identifier.type": "<phone_number>"
	},
	"user_id": "<string>"
}
```

