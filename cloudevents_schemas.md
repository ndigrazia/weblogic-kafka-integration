### CloudEvents 

### CloudEvents 1.0 Specification

https://github.com/cloudevents/spec 

```json
{
   "specversion":"1.0", //The version of the CloudEvents specification being used.
   "id":"<uuid>", //A unique identifier for the event, which ensures that each event is distinct.
   "source":"<source>", // The source of the event. (e.g., urn:telefonica:at3osbc:jms:ob-eventsqueue).
   "type":"<type>", //The type of the event, indicating the nature of the event.
   "datacontenttype":"application/json",
   "time":"<time>", //The timestamp when the event was generated, usually in ISO 8601 format.
   "data":{ 
      //The payload content depends on event types. See event types below.
   	}
}
```

## Event types:

### Publish new user event

1. Type: telefonica.event.user.created.v1
2. Data:

```json
{
	"userId": "string"
}
```

### Publish user removal event

1. Type: telefonica.event.user.removed.v1
2. Data:

```json
{
	"userId": "string"
}
```

### Publish new user identifier event

1. Type: telefonica.event.user-identifier.created.v1
2. Data:

```json
{
	"identifier": {
		"id": "string",
		"type": "phonenumber"
	},
	"userId": "string"
}
```

### Publish user identifier removal event

1. Type: telefonica.event.user-identifier.removed.v1
2. Data:

```json
{
	"identifier": {
		"id": "string",
		"type": "phonenumber"
	},
	"userId": "string"
}
```

### Publish identifier user change event

1. Type: telefonica.event.user-identifier.updated.v1
2. Data:

```json
{
	"identifier": {
		"id": "string",
		"type": "phonenumber"
	},
	"userId": "string"
}
```

### Publish user identifier change event

1. Type: telefonica.event.identifier.updated.v1
2. Data:

```json
{
	"newIdentifier": {
		"id": "string",
		"type": "phonenumber"
	},
	"oldIdentifier": {
		"id": "string",
		"type": "phonenumber"
	},
	"userId": "string"
}
```

