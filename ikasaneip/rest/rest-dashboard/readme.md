![Problem Domain](../../developer/docs/quickstart-images/Ikasan-title-transparent.png)
# Rest Dashboard

## Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [Data Harvesting Services](#data-harvesting-services)
  - [Error Harvesting Service](#error-harvesting-service)
  - [Exclusions Harvesting Service](#exclusions-harvesting-service)
  - [Metrics Harvesting Service](#metrics-harvesting-service)
  - [Replay Events Harvesting Service](#replay-events-harvesting-service)
  - [Wiretap Events Harvesting Service](#wiretap-events-harvesting-service)
- [Metadata Services](#metadata-services)
  - [Module Metadata Service](#module-metadata-service)
  - [Configuration Service](#configuration-service)
- [Data Sharing Services](#data-sharing-services)
  - [Query Wiretap Events](#query-wiretap-events)
  - [Query Error Occurrences](#query-error-occurrences)
  - [Query Exclusion Events](#query-exclusion-events)
  - [Query Replay Events](#query-replay-events)
  - [Query Module Metadata](#query-module-metadata)
  - [Query Configuration Metadata](#query-configuration-metadata)

## Overview

The Ikasan Dashboard exposes a comprehensive set of REST service endpoints that enable integration modules to:

1. **Push transient event data** - Runtime events from modules are aggregated and stored in Solr (text index) or MongoDB, depending on configuration
2. **Share module metadata** - Structural and configuration metadata describing the module topology and component details
3. **Query aggregated data** - Retrieve wiretap events, errors, exclusions, replays, and metadata using flexible query parameters

The dashboard acts as a central aggregation point for distributed Ikasan integration modules, providing both data ingestion (harvesting) and data retrieval (sharing) capabilities.

## Authentication

All Ikasan Dashboard REST service endpoints require an `Authorization` HTTP header containing a JWT bearer token:

```
Authorization: Bearer {JWT_TOKEN}
```

The JWT token can be obtained from the Authentication Endpoint by providing valid user credentials.

### Authorization Endpoint
Authentication and Authorization Service.

| Parameter | Value  |
|--- | --- |
| Request Method | POST |
| Service Context | {dashboard-root-context}/authenticate |
| Payload | A json serialised  [JwtRequest](src/main/java/org/ikasan/rest/dashboard/model/JwtRequest.java) |

<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
{
     "username":"testUsername",
     "password":"SecretPassword"
}
````

</p>



</details>

<details>
    <summary>Provided response.</summary>
<p>

````json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTU2NzI1NzYwOCwiaWF0IjoxNTY3MjM5NjA4fQ.9v2AolonpxP2E6jl-PJVNK-A3oHrHTR1YNM9MCQMkSTLtbreO9vAWlh6dsN3NeWgRipXWVcG3TZp1HO0gQnndw"
}
````

</p>


</details>

---

## Data Harvesting Services

Data harvesting services allow Ikasan integration modules to push runtime event data to the dashboard for aggregation and persistence.

### Error Harvesting Service
Aggregation service for errors produced by the Ikasan Hospital service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/harvest/errors |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Payload | A json serialised List of [ErrorOccurence](../../spec/service/error-reporting/src/main/java/org/ikasan/spec/error/reporting/ErrorOccurrence.java) |

<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[
  {
    "uri": "errorUri",
    "moduleName": "moduleName",
    "flowName": "flowName",
    "flowElementName": "componentName",
    "errorDetail": "errorDetail",
    "errorMessage": "failed error occurrence text",
    "exceptionClass": "exception.class",
    "eventLifeIdentifier": "lifeId",
    "eventRelatedIdentifier": "relatedLifeId",
    "action": "action",
    "event": "ZXZlbnQ=",
    "eventAsString": "event",
    "timestamp": 1000,
    "expiry": 0,
    "userAction": "userAction",
    "actionedBy": "actionedBy",
    "userActionTimestamp": 0
  },
  {
    "uri": "errorUri",
    "moduleName": "moduleName",
    "flowName": "flowName",
    "flowElementName": "componentName",
    "errorDetail": "errorDetail",
    "errorMessage": "failed error occurrence text",
    "exceptionClass": "exception.class",
    "eventLifeIdentifier": "lifeId",
    "eventRelatedIdentifier": "relatedLifeId",
    "action": "action",
    "event": "ZXZlbnQ=",
    "eventAsString": "event",
    "timestamp": 1000,
    "expiry": 0,
    "userAction": "userAction",
    "actionedBy": "actionedBy",
    "userActionTimestamp": 0
  },
  {
    "uri": "errorUri",
    "moduleName": "moduleName",
    "flowName": "flowName",
    "flowElementName": "componentName",
    "errorDetail": "errorDetail",
    "errorMessage": "failed error occurrence text",
    "exceptionClass": "exception.class",
    "eventLifeIdentifier": "lifeId",
    "eventRelatedIdentifier": "relatedLifeId",
    "action": "action",
    "event": "ZXZlbnQ=",
    "eventAsString": "event",
    "timestamp": 1000,
    "expiry": 0,
    "userAction": "userAction",
    "actionedBy": "actionedBy",
    "userActionTimestamp": 0
  }
]
````

</p>
</details>

### Exclusions Harvesting Service
Aggregation service for exclusions produced by the Ikasan Hospital service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/harvest/exclusions |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Payload | A json serialised List of [ExclusionEvent](../../spec/service/exclusion/src/main/java/org/ikasan/spec/exclusion/ExclusionEvent.java) |


<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[
  {
    "id": 1230,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "identifier": "identifier",
    "event": "ZXZlbnQ=",
    "timestamp": 1234,
    "errorUri": "errorUri",
    "harvested": false
  },
  {
    "id": 1230,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "identifier": "identifier",
    "event": "ZXZlbnQ=",
    "timestamp": 1234,
    "errorUri": "errorUri",
    "harvested": false
  },
  {
    "id": 1230,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "identifier": "identifier",
    "event": "ZXZlbnQ=",
    "timestamp": 1234,
    "errorUri": "errorUri",
    "harvested": false
  }
]
````

</p>
</details>

### Metrics Harvesting Service
Aggregation service for metrics produced by the Ikasan Metrics service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/harvest/metrics |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Payload | A json serialised List of [FlowInvocationMetric](../../spec/service/history/src/main/java/org/ikasan/spec/history/FlowInvocationMetric.java) |


<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[
  {
    "id": 1,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "invocationStartTime": 1564929295578,
    "invocationEndTime": 1564929296078,
    "finalAction": "ACTION",
    "componentInvocationMetricImpls": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295576,
        "endTimeMillis": 1564929296076,
        "id": 3,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 3,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295570,
        "endTimeMillis": 1564929296070,
        "id": 2,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 2,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295400,
        "endTimeMillis": 1564929295900,
        "id": 1,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 1,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295572,
        "endTimeMillis": 1564929296072,
        "id": 4,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 4,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295574,
        "endTimeMillis": 1564929296074,
        "id": 5,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 5,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ],
    "harvested": true,
    "expiry": 0,
    "errorUri": null,
    "harvestedDateTime": 1564929297534,
    "flowInvocationEvents": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295576,
        "endTimeMillis": 1564929296076,
        "id": 3,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 3,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295570,
        "endTimeMillis": 1564929296070,
        "id": 2,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 2,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295400,
        "endTimeMillis": 1564929295900,
        "id": 1,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 1,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295572,
        "endTimeMillis": 1564929296072,
        "id": 4,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 4,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId0",
        "beforeRelatedEventIdentifier": "relatedLifeId0",
        "afterEventIdentifier": "lifeId0",
        "afterRelatedEventIdentifier": "relatedLifeId0",
        "startTimeMillis": 1564929295574,
        "endTimeMillis": 1564929296074,
        "id": 5,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 5,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 5,
          "timestamp": 1564929296076,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId0",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ]
  },
  {
    "id": 2,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "invocationStartTime": 1564929295612,
    "invocationEndTime": 1564929296112,
    "finalAction": "ACTION",
    "componentInvocationMetricImpls": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295598,
        "endTimeMillis": 1564929296098,
        "id": 8,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 8,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295602,
        "endTimeMillis": 1564929296102,
        "id": 9,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 9,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295611,
        "endTimeMillis": 1564929296111,
        "id": 6,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 6,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295600,
        "endTimeMillis": 1564929296100,
        "id": 7,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 7,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295608,
        "endTimeMillis": 1564929296108,
        "id": 10,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 10,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ],
    "harvested": true,
    "expiry": 0,
    "errorUri": null,
    "harvestedDateTime": 1564929297546,
    "flowInvocationEvents": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295598,
        "endTimeMillis": 1564929296098,
        "id": 8,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 8,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295602,
        "endTimeMillis": 1564929296102,
        "id": 9,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 9,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295611,
        "endTimeMillis": 1564929296111,
        "id": 6,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 6,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295600,
        "endTimeMillis": 1564929296100,
        "id": 7,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 7,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId1",
        "beforeRelatedEventIdentifier": "relatedLifeId1",
        "afterEventIdentifier": "lifeId1",
        "afterRelatedEventIdentifier": "relatedLifeId1",
        "startTimeMillis": 1564929295608,
        "endTimeMillis": 1564929296108,
        "id": 10,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 10,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 10,
          "timestamp": 1564929296111,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId1",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ]
  },
  {
    "id": 3,
    "moduleName": "moduleName",
    "flowName": "flowName",
    "invocationStartTime": 1564929295628,
    "invocationEndTime": 1564929296128,
    "finalAction": "ACTION",
    "componentInvocationMetricImpls": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295621,
        "endTimeMillis": 1564929296121,
        "id": 11,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 11,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295625,
        "endTimeMillis": 1564929296125,
        "id": 13,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 13,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295623,
        "endTimeMillis": 1564929296123,
        "id": 15,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 15,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295619,
        "endTimeMillis": 1564929296119,
        "id": 14,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 14,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295627,
        "endTimeMillis": 1564929296127,
        "id": 12,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 12,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ],
    "harvested": true,
    "expiry": 0,
    "errorUri": null,
    "harvestedDateTime": 1564929297548,
    "flowInvocationEvents": [
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295621,
        "endTimeMillis": 1564929296121,
        "id": 11,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 11,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295625,
        "endTimeMillis": 1564929296125,
        "id": 13,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 13,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295623,
        "endTimeMillis": 1564929296123,
        "id": 15,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 15,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295619,
        "endTimeMillis": 1564929296119,
        "id": 14,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 14,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      },
      {
        "componentName": "componentName",
        "beforeEventIdentifier": "lifeId2",
        "beforeRelatedEventIdentifier": "relatedLifeId2",
        "afterEventIdentifier": "lifeId2",
        "afterRelatedEventIdentifier": "relatedLifeId2",
        "startTimeMillis": 1564929295627,
        "endTimeMillis": 1564929296127,
        "id": 12,
        "flowInvocation": null,
        "metrics": [
          {
            "id": 12,
            "componentInvocationMetricImpl": null,
            "name": "name",
            "value": "value"
          }
        ],
        "wiretapFlowEvent": {
          "identifier": 15,
          "timestamp": 1564929296127,
          "moduleName": "moduleName",
          "flowName": "flowName",
          "componentName": "componentName",
          "event": "payload",
          "expiry": 30,
          "nextByEventId": null,
          "previousByEventId": null,
          "eventId": "lifeId2",
          "relatedEventId": null,
          "eventTimestamp": 0
        }
      }
    ]
  }
]
````

</p>
</details>

### Replay Events Harvesting Service
Aggregation service for replay events produced by the Ikasan Replay service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/harvest/replay |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Payload | A json serialised List of [ReplayEvent](../../spec/service/replay/src/main/java/org/ikasan/spec/replay/ReplayEvent.java) |


<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[ {
  "id" : 1,
  "moduleName" : "moduleName",
  "flowName" : "flowName",
  "eventId" : "errorUri",
  "event" : "ZXZlbnQ=",
  "eventAsString" : "event",
  "timestamp" : 1564931198117,
  "expiry" : 1567523198117,
  "harvested" : false,
  "harvestedDateTime" : 0
}, {
  "id" : 2,
  "moduleName" : "moduleName",
  "flowName" : "flowName",
  "eventId" : "errorUri",
  "event" : "ZXZlbnQ=",
  "eventAsString" : "event",
  "timestamp" : 1564931198248,
  "expiry" : 1567523198248,
  "harvested" : false,
  "harvestedDateTime" : 0
}, {
  "id" : 3,
  "moduleName" : "moduleName",
  "flowName" : "flowName",
  "eventId" : "errorUri",
  "event" : "ZXZlbnQ=",
  "eventAsString" : "event",
  "timestamp" : 1564931198253,
  "expiry" : 1567523198253,
  "harvested" : false,
  "harvestedDateTime" : 0
}]
````

</p>
</details>

### Wiretap Events Harvesting Service
Aggregation service for wiretap events produced by the Ikasan Wiretap service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/harvest/wiretaps |
| Payload | A json serialised List of [WiretapEvent](../../spec/service/wiretap/src/main/java/org/ikasan/spec/wiretap/WiretapEvent.java) |

<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[
  {
    "moduleName": "My Module Name",
    "flowName": "My Flow Name",
    "event": "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>",
    "componentName": "My Component Name",
    "expiry": 1234567,
    "eventId": "event identifier",
    "identifier": 678910,
    "timestamp": 99999999999999
  },
  {
    "moduleName": "My Module Name",
    "flowName": "My Flow Name",
    "event": "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>",
    "componentName": "My Component Name",
    "expiry": 1234567,
    "eventId": "event identifier",
    "identifier": 678910,
    "timestamp": 99999999999999
  },
  {
    "moduleName": "My Module Name",
    "flowName": "My Flow Name",
    "event": "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>",
    "componentName": "My Component Name",
    "expiry": 1234567,
    "eventId": "event identifier",
    "identifier": 678910,
    "timestamp": 99999999999999
  }
]
````

</p>
</details>

---

## Metadata Services

Metadata services allow Ikasan integration modules to publish their structural and configuration metadata to the dashboard.

### Module Metadata Service
Aggregation service for module meta data produced by the Ikasan Topology service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/module/metadata |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Payload | A json serialised [ModuleMetaData](../../spec/metadata/src/main/java/org/ikasan/spec/metadata/ModuleMetaData.java) |


<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
{
  "name" : "module name",
  "description" : "module description",
  "version" : "module version",
  "flows" : [ {
    "name" : "Simple Flow 1",
    "consumer" : {
      "componentName" : "Test Consumer",
      "description" : "Test Consumer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Consumer",
      "implementingClass" : "org.ikasan.metadata.components.TestConsumer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    },
    "transitions" : [ {
      "from" : "Test Converter",
      "to" : "Test Producer",
      "name" : "default"
    }, {
      "from" : "Test Broker",
      "to" : "Test Converter",
      "name" : "default"
    }, {
      "from" : "Test Splitter",
      "to" : "Test Broker",
      "name" : "default"
    }, {
      "from" : "Test Filter",
      "to" : "Test Splitter",
      "name" : "default"
    }, {
      "from" : "Test Consumer",
      "to" : "Test Filter",
      "name" : "default"
    } ],
    "flowElements" : [ {
      "componentName" : "Test Producer",
      "description" : "Test Producer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Producer",
      "implementingClass" : "org.ikasan.metadata.components.TestProducer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Converter",
      "description" : "Test Converter Description",
      "componentType" : "org.ikasan.spec.component.transformation.Converter",
      "implementingClass" : "org.ikasan.metadata.components.TestConverter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Broker",
      "description" : "Test Broker Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Broker",
      "implementingClass" : "org.ikasan.metadata.components.TestBroker",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Splitter",
      "description" : "Test Splitter Description",
      "componentType" : "org.ikasan.spec.component.splitting.Splitter",
      "implementingClass" : "org.ikasan.metadata.components.TestSplitter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Filter",
      "description" : "Test Filter Description",
      "componentType" : "org.ikasan.spec.component.filter.Filter",
      "implementingClass" : "org.ikasan.metadata.components.TestFilter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Consumer",
      "description" : "Test Consumer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Consumer",
      "implementingClass" : "org.ikasan.metadata.components.TestConsumer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    } ],
    "configurationId" : "FLOW_CONFIGURATION_ID"
  }, {
    "name" : "Simple Flow 2",
    "consumer" : {
      "componentName" : "Test Consumer",
      "description" : "Test Consumer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Consumer",
      "implementingClass" : "org.ikasan.metadata.components.TestConsumer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    },
    "transitions" : [ {
      "from" : "Test Converter",
      "to" : "Test Producer",
      "name" : "default"
    }, {
      "from" : "Test Broker",
      "to" : "Test Converter",
      "name" : "default"
    }, {
      "from" : "Test Splitter",
      "to" : "Test Broker",
      "name" : "default"
    }, {
      "from" : "Test Filter",
      "to" : "Test Splitter",
      "name" : "default"
    }, {
      "from" : "Test Consumer",
      "to" : "Test Filter",
      "name" : "default"
    } ],
    "flowElements" : [ {
      "componentName" : "Test Producer",
      "description" : "Test Producer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Producer",
      "implementingClass" : "org.ikasan.metadata.components.TestProducer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Converter",
      "description" : "Test Converter Description",
      "componentType" : "org.ikasan.spec.component.transformation.Converter",
      "implementingClass" : "org.ikasan.metadata.components.TestConverter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Broker",
      "description" : "Test Broker Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Broker",
      "implementingClass" : "org.ikasan.metadata.components.TestBroker",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Splitter",
      "description" : "Test Splitter Description",
      "componentType" : "org.ikasan.spec.component.splitting.Splitter",
      "implementingClass" : "org.ikasan.metadata.components.TestSplitter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Filter",
      "description" : "Test Filter Description",
      "componentType" : "org.ikasan.spec.component.filter.Filter",
      "implementingClass" : "org.ikasan.metadata.components.TestFilter",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    }, {
      "componentName" : "Test Consumer",
      "description" : "Test Consumer Description",
      "componentType" : "org.ikasan.spec.component.endpoint.Consumer",
      "implementingClass" : "org.ikasan.metadata.components.TestConsumer",
      "configurationId" : null,
      "invokerConfigurationId" : "FLOW_INVOKER_CONFIGURATION_ID",
      "configurable" : false
    } ],
    "configurationId" : "FLOW_CONFIGURATION_ID"
  }
  ]
}
````

</p>
</details>

## Log File Download Services
Diagnostic services for downloading log files from the Dashboard, Modules, and Solr instances. These services provide convenient access to log files for troubleshooting and diagnostics.

### Dashboard Logs Download Service
Downloads all log files from the Ikasan Dashboard instance as a compressed zip file.

| Parameter | Value  |
|--- | --- |
| Request Method | GET |
| Service Context | {dashboard-root-context}/rest/logs/dashboard/zip |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Response Type | application/octet-stream |
| Response | A zip file containing all dashboard log files |

**Description:** This endpoint creates a zip archive of all log files from the dashboard's `logs` directory, including any subdirectories. The zip file is streamed directly to the client for download. The log directory location is determined by the `dashboard.log.dir` configuration property. If not specified, it defaults to `{user.dir}/logs`.

**Response Headers:**
- `Content-Type`: application/octet-stream
- `Content-Disposition`: attachment;filename=dashboardLogs{timestamp}.zip

**Error Responses:**
- `500 Internal Server Error`: If the logs directory does not exist or cannot be accessed
- `401 Unauthorized`: If the authorization token is invalid or missing

**Configuration:**
The dashboard log directory can be configured using the `dashboard.log.dir` property:
```properties
dashboard.log.dir=/path/to/dashboard/logs
```

If not configured, the service will default to using the `logs` directory under the current working directory (`{user.dir}/logs`).

### Module Logs Download Service
Downloads all log files from a specific Ikasan Module instance as a compressed zip file.

| Parameter | Value  |
|--- | --- |
| Request Method | GET |
| Service Context | {dashboard-root-context}/rest/logs/module/zip?moduleName={moduleName} |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Request Parameter | moduleName (required) - The name of the module |
| Response Type | application/octet-stream |
| Response | A zip file containing all module log files |

**Description:** This endpoint retrieves the module metadata from the dashboard's database, connects to the module's REST service endpoint, downloads all available log files, and packages them into a zip archive. This allows centralized log collection from distributed module instances.

**Request Example:**
```
GET /rest/logs/module/zip?moduleName=MyIntegrationModule
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response Headers:**
- `Content-Type`: application/octet-stream
- `Content-Disposition`: attachment;filename=dashboardLogs{timestamp}.zip

**Error Responses:**
- `500 Internal Server Error`: If the module metadata does not exist or the module logs cannot be retrieved
- `400 Bad Request`: If the moduleName parameter is missing
- `401 Unauthorized`: If the authorization token is invalid or missing

### Solr Logs Download Service
Downloads all log files from the Solr instance as a compressed zip file.

| Parameter | Value  |
|--- | --- |
| Request Method | GET |
| Service Context | {dashboard-root-context}/rest/logs/solr/zip |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Response Type | application/octet-stream |
| Response | A zip file containing all Solr log files |

**Description:** This endpoint creates a zip archive of all log files from the Solr server's logs directory. The location is determined by the `solr.install.dir` configuration property. If not specified, it defaults to `{user.dir}/solr/server/logs`.

**Response Headers:**
- `Content-Type`: application/octet-stream
- `Content-Disposition`: attachment;filename=solrLogs-{timestamp}.zip

**Error Responses:**
- `500 Internal Server Error`: If the Solr logs directory does not exist or cannot be accessed
- `401 Unauthorized`: If the authorization token is invalid or missing

**Configuration:**
The Solr installation directory can be configured using the `solr.install.dir` property:
```properties
solr.install.dir=/path/to/solr
```

## System Events Download Service
Diagnostic service for downloading system events from the Dashboard as a compressed zip file containing JSON files. This service provides convenient access to recent system events for troubleshooting, auditing, and diagnostics.

### System Events Last 24 Hours Download Service
Downloads all system events from the last 24 hours as a compressed zip file containing individual JSON files for each event.

| Parameter | Value  |
|--- | --- |
| Request Method | GET |
| Service Context | {dashboard-root-context}/rest/systemevents/last24hours/zip |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Response Type | application/octet-stream |
| Response | A zip file containing system event JSON files |

**Description:** This endpoint queries the system event search service for all events that occurred within the last 24 hours (from current time - 24 hours to current time). Each system event is serialized as a formatted JSON file and packaged into a zip archive. The JSON files are named sequentially as `systemEvent1.json`, `systemEvent2.json`, etc. This provides a convenient way to export system events for offline analysis, auditing, or archiving.

**System Event Data:**
Each JSON file in the zip contains a [SystemEvent](../../spec/service/system-event/src/main/java/org/ikasan/spec/systemevent/SystemEvent.java) object with the following fields:
- `id`: Unique identifier for the event
- `moduleName`: Name of the module that generated the event
- `action`: The action that was performed (e.g., START, STOP, PAUSE, RESUME)
- `actor`: The user or system that triggered the action
- `subject`: Description or details of the event
- `timestamp`: Date/time when the event occurred
- `expiry`: Date/time when the event will expire

**Request Example:**
```
GET /rest/systemevents/last24hours/zip
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response Headers:**
- `Content-Type`: application/octet-stream
- `Content-Disposition`: attachment;filename=systemEvents-{timestamp}.zip

**Sample System Event JSON:**
<details>
    <summary>Click to view sample system event JSON content</summary>
<p>

```json
{
  "id": 12345,
  "moduleName": "MyIntegrationModule",
  "action": "START",
  "actor": "admin@example.com",
  "subject": "Flow 'CustomerOrderFlow' started successfully",
  "timestamp": 1234567890000,
  "expiry": 1237246290000
}
```

</p>
</details>

**Error Responses:**
- `500 Internal Server Error`: If the system event search service encounters an error or if no events are found (empty result set)
  - When an error occurs, the response will include:
    - `Content-Disposition`: attachment;filename=error.txt
    - `Content-Type`: application/octet-stream
    - Body: Error message describing the failure
- `401 Unauthorized`: If the authorization token is invalid or missing

**Use Cases:**
- **Audit Trail**: Export system events for compliance and audit purposes
- **Troubleshooting**: Download recent events to analyze system behavior during incidents
- **Archiving**: Periodic export of events for long-term storage
- **Analysis**: Offline processing and analysis of system activity patterns
- **Reporting**: Generate reports from exported event data

**Notes:**
- The service uses pretty-printed JSON for better readability
- Events are queried using a 24-hour sliding window from the current time
- The zip file is streamed directly to the client for memory efficiency
- Empty or no results will return a 500 error with an error.txt file

## Security Service
Role-based access control service for managing module and job plan access permissions.

### Assign Module to Roles
Assigns a module to one or more roles, enabling fine-grained access control over integration modules.

| Parameter | Value  |
|--- | --- |
| Request Method | POST |
| Service Context | {dashboard-root-context}/rest/security/module/roles?moduleName={moduleName} |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Required Authorities | ALL, WebServiceAdmin |
| Request Parameter | moduleName (required) - The name of the module to assign |
| Payload | A JSON array of role names (strings) |

**Description:** This endpoint validates that the specified module exists and that all provided role names correspond to valid roles in the system. If validation passes, it creates role-module associations for each role, granting those roles access to the specified module.

**Validation:**
- Module name must not be null or empty
- Module must exist in the module metadata service
- Role names list must not be null or empty
- All role names must correspond to existing roles

**Request Example:**
```
POST /rest/security/module/roles?moduleName=CustomerIntegrationModule
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

["Admin", "IntegrationDeveloper", "SupportTeam"]
```

**Success Response:**
- `200 OK`: Module successfully assigned to all specified roles
- Response body: Empty

**Error Responses:**
- `400 Bad Request`:
  - Module name is null, empty, or whitespace only
  - Module does not exist in the system
  - Role names list is null or empty
  - One or more role names are invalid (null, empty, or do not exist)
  - Response body: `{"errorCode": null, "errorMessage": "Error description"}`
- `500 Internal Server Error`: Unexpected error during processing
- `401 Unauthorized`: Authorization token is invalid or missing
- `403 Forbidden`: User does not have required authority (ALL or WebServiceAdmin)

**Error Response Examples:**

<details>
    <summary>Module does not exist</summary>
<p>

```json
{
  "errorCode": null,
  "errorMessage": "Module [NonExistentModule] does not exist"
}
```

</p>
</details>

<details>
    <summary>Invalid role names</summary>
<p>

```json
{
  "errorCode": null,
  "errorMessage": "The following roles are not valid: InvalidRole1, InvalidRole2"
}
```

</p>
</details>

### Assign Job Plan to Roles
Assigns a job plan (scheduled context) to one or more roles, enabling fine-grained access control over scheduled job plans.

| Parameter | Value  |
|--- | --- |
| Request Method | POST |
| Service Context | {dashboard-root-context}/rest/security/jobplan/roles?jobPlanName={jobPlanName} |
| Requires 'Authorization' HTTP Header | Bearer {JWT TOKEN} |
| Required Authorities | ALL, WebServiceAdmin |
| Request Parameter | jobPlanName (required) - The name of the job plan to assign |
| Payload | A JSON array of role names (strings) |

**Description:** This endpoint validates that the specified job plan exists and that all provided role names correspond to valid roles in the system. If validation passes, it creates role-job plan associations for each role, granting those roles access to the specified job plan.

**Validation:**
- Job plan name must not be null or empty
- Job plan must exist in the scheduled context service
- Role names list must not be null or empty
- All role names must correspond to existing roles

**Request Example:**
```
POST /rest/security/jobplan/roles?jobPlanName=DailyDataSyncPlan
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

["Admin", "SchedulerOperator", "SupportTeam"]
```

**Success Response:**
- `200 OK`: Job plan successfully assigned to all specified roles
- Response body: Empty

**Error Responses:**
- `400 Bad Request`:
  - Job plan name is null, empty, or whitespace only
  - Job plan does not exist in the system
  - Role names list is null or empty
  - One or more role names are invalid (null, empty, or do not exist)
  - Response body: `{"errorCode": null, "errorMessage": "Error description"}`
- `500 Internal Server Error`: Unexpected error during processing
- `401 Unauthorized`: Authorization token is invalid or missing
- `403 Forbidden`: User does not have required authority (ALL or WebServiceAdmin)

**Error Response Examples:**

<details>
    <summary>Job plan does not exist</summary>
<p>

```json
{
  "errorCode": null,
  "errorMessage": "Job plan [NonExistentPlan] does not exist"
}
```

</p>
</details>

<details>
    <summary>Multiple validation errors</summary>
<p>

```json
{
  "errorCode": null,
  "errorMessage": "The following roles are not valid: null, \"\" (empty), NonExistentRole"
}
```

</p>
</details>

**Implementation Details:**
- Both endpoints use fail-fast validation: all validation checks must pass before any role assignments are made
- Role validation is centralized in a private `validateRoleNames()` method
- Module validation uses `ModuleMetaDataService.findById()`
- Job plan validation uses `ScheduledContextService.findByName()`
- All operations are logged with appropriate log levels (INFO for success, WARN for validation failures, ERROR for exceptions)

**Use Cases:**
- **Role-Based Access Control (RBAC)**: Implement fine-grained access control for modules and job plans
- **Security Auditing**: Track which roles have access to specific integration components
- **Multi-Tenant Deployments**: Segregate access to different integration modules by organization or team
- **Operational Segregation**: Control which operational teams can manage specific job plans
- **Compliance**: Enforce access control policies for sensitive integration processes

### Configuration Service
Aggregation service for module meta data produced by the Ikasan Topology service.

| Parameter | Value  |
|--- | --- |
| Request Method | PUT |
| Service Context | {dashboard-root-context}/rest/module/configuration |
| Payload | See [Configuration Service metadata](../../configuration-service/Readme.md) |


<details>
    <summary>Click to view the sample JSON payload expected by the service.</summary>
<p>

````json
[ {
  "configurationId" : "consumerConfiguredResourceId",
  "description" : "desc",
  "implementingClass" : "org.ikasan.configurationService.model.DefaultConfiguration",
  "parameters" : [ {
    "id" : null,
    "name" : "name",
    "value" : "value",
    "description" : "desc",
    "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterStringImpl"
  }, {
    "id" : null,
    "name" : "name",
    "value" : 10,
    "description" : "desc",
    "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterIntegerImpl"
  }, {
    "id" : null,
    "name" : "name",
    "value" : 10,
    "description" : "desc",
    "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterLongImpl"
  }, {
    "id" : null,
    "name" : "name",
    "value" : [ "one", "two", "three" ],
    "description" : "desc",
    "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterListImpl"
  }, {
    "id" : null,
    "name" : "name",
    "value" : {
      "one" : "1",
      "two" : "2",
      "three" : "3"
    },
    "description" : "desc",
    "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterMapImpl"
  } ]
},
  {
    "configurationId" : "producerConfiguredResourceId",
    "description" : "desc",
    "implementingClass" : "org.ikasan.configurationService.model.DefaultConfiguration",
    "parameters" : [ {
      "id" : null,
      "name" : "name",
      "value" : "value",
      "description" : "desc",
      "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterStringImpl"
    }, {
      "id" : null,
      "name" : "name",
      "value" : 10,
      "description" : "desc",
      "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterIntegerImpl"
    }, {
      "id" : null,
      "name" : "name",
      "value" : 10,
      "description" : "desc",
      "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterLongImpl"
    }, {
      "id" : null,
      "name" : "name",
      "value" : [ "one", "two", "three" ],
      "description" : "desc",
      "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterListImpl"
    }, {
      "id" : null,
      "name" : "name",
      "value" : {
        "one" : "1",
        "two" : "2",
        "three" : "3"
      },
      "description" : "desc",
      "implementingClass": "org.ikasan.configurationService.model.ConfigurationParameterMapImpl"
    } ]
  }]
````

</p>
</details>

---

## Data Sharing Services

Data Sharing Services (IKASAN-2793) provide query capabilities for retrieving aggregated event data and metadata from the dashboard. These services enable external systems or dashboards to query wiretap events, error occurrences, exclusions, replays, module metadata, and configuration metadata using flexible filtering, pagination, and sorting.

All Data Sharing Services endpoints:
- Require JWT authentication via `Authorization: Bearer {JWT_TOKEN}` header
- Support flexible query parameters for filtering
- Return paginated results with total count metadata
- Support custom sorting by field and order
- Use specialized converters to transform internal documents to REST DTOs

### Query Wiretap Events

Query service for retrieving wiretap events with flexible filtering and pagination.

**Endpoint:** `GET /rest/data-sharing/wiretap`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `fromTimestamp` | Long | **Yes** | - | Start timestamp (milliseconds) |
| `toTimestamp` | Long | **Yes** | - | End timestamp (milliseconds) |
| `moduleNames` | List&lt;String&gt; | No | - | Filter by module names |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Response:** JSON object containing:
- `data`: Array of [WiretapEvent](src/main/java/org/ikasan/rest/dashboard/model/wiretap/WiretapEventImpl.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/wiretap?fromTimestamp=1609459200000&toTimestamp=1612137600000&moduleNames=MyModule&offset=0&limit=50
Authorization: Bearer eyJhbGc...
```

**Count Endpoint:** `GET /rest/data-sharing/wiretap/count`

Uses same parameters (`fromTimestamp`, `toTimestamp`, `moduleNames`) and returns total count.

### Query Error Occurrences

Query service for retrieving error occurrences with flexible filtering and pagination.

**Endpoint:** `GET /rest/data-sharing/errors`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `fromTimestamp` | Long | **Yes** | - | Start timestamp (milliseconds) |
| `toTimestamp` | Long | **Yes** | - | End timestamp (milliseconds) |
| `moduleNames` | List&lt;String&gt; | No | - | Filter by module names |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Response:** JSON object containing:
- `data`: Array of [ErrorOccurrence](src/main/java/org/ikasan/rest/dashboard/model/error/ErrorOccurrenceImpl.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/errors?fromTimestamp=1609459200000&toTimestamp=1612137600000&moduleNames=MyModule&limit=25
Authorization: Bearer eyJhbGc...
```

**Count Endpoint:** `GET /rest/data-sharing/errors/count`

Uses same parameters (`fromTimestamp`, `toTimestamp`, `moduleNames`) and returns total count.

### Query Exclusion Events

Query service for retrieving exclusion events with flexible filtering and pagination.

**Endpoint:** `GET /rest/data-sharing/exclusions`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `fromTimestamp` | Long | **Yes** | - | Start timestamp (milliseconds) |
| `toTimestamp` | Long | **Yes** | - | End timestamp (milliseconds) |
| `moduleNames` | List&lt;String&gt; | No | - | Filter by module names |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Response:** JSON object containing:
- `data`: Array of [ExclusionEvent](src/main/java/org/ikasan/rest/dashboard/model/exclusion/ExclusionEventImpl.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/exclusions?fromTimestamp=1609459200000&toTimestamp=1612137600000&moduleNames=MyModule&offset=0&limit=100
Authorization: Bearer eyJhbGc...
```

**Count Endpoint:** `GET /rest/data-sharing/exclusions/count`

Uses same parameters (`fromTimestamp`, `toTimestamp`, `moduleNames`) and returns total count.

### Query Replay Events

Query service for retrieving replay events with flexible filtering and pagination.

**Endpoint:** `GET /rest/data-sharing/replays`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `fromTimestamp` | Long | **Yes** | - | Start timestamp (milliseconds) |
| `toTimestamp` | Long | **Yes** | - | End timestamp (milliseconds) |
| `moduleNames` | List&lt;String&gt; | No | - | Filter by module names |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Response:** JSON object containing:
- `data`: Array of [ReplayEvent](src/main/java/org/ikasan/rest/dashboard/model/replay/ReplayEventImpl.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/replays?fromTimestamp=1609459200000&toTimestamp=1612137600000&moduleNames=MyModule&offset=0&limit=50
Authorization: Bearer eyJhbGc...
```

**Count Endpoint:** `GET /rest/data-sharing/replays/count`

Uses same parameters (`fromTimestamp`, `toTimestamp`, `moduleNames`) and returns total count.

### Query Module Metadata

Query service for retrieving module metadata by module names.

**Endpoint:** `GET /rest/data-sharing/module-metadata`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `moduleNames` | List&lt;String&gt; | **Yes** | - | Module names to query (used as identifiers) |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Note:** For module metadata queries, the module name **is** the identifier. The service searches for module metadata documents by matching against module names.

**Response:** JSON object containing:
- `data`: Array of [ModuleMetaData](src/main/java/org/ikasan/rest/dashboard/model/metadata/module/ModuleMetaDataImpl.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/module-metadata?moduleNames=OrderModule,InventoryModule&limit=10
Authorization: Bearer eyJhbGc...
```

### Query Configuration Metadata

Query service for retrieving configuration metadata by configuration IDs.

**Endpoint:** `GET /rest/data-sharing/configuration`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `configurationIdentifiers` | List&lt;String&gt; | **Yes** | - | Configuration IDs to query (used as identifiers) |
| `offset` | Integer | No | 0 | Pagination offset |
| `limit` | Integer | No | 1000 | Result size limit |

**Note:** For configuration queries, the configuration ID **is** the identifier. The service searches for configuration documents by matching against configuration IDs.

**Response:** JSON object containing:
- `data`: Array of [ConfigurationMetaData](../../spec/metadata/src/main/java/org/ikasan/spec/metadata/ConfigurationMetaData.java)
- `totalCount`: Total number of matching results
- `hasMore`: Boolean indicating if more results are available

**Example Request:**
```
GET /rest/data-sharing/configuration?configurationIdentifiers=FLOW_CONFIG_1,CONSUMER_CONFIG_2&offset=0&limit=50
Authorization: Bearer eyJhbGc...
```

---

## Implementation Notes

### Document Converters

The Data Sharing Services use specialized converters to transform internal `IkasanESBDocument` instances to specific REST DTOs:

- [IkasanESBDocumentToWiretapEventConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToWiretapEventConverter.java) - Converts to WiretapEvent
- [IkasanESBDocumentToErrorOccurenceConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToErrorOccurenceConverter.java) - Converts to ErrorOccurrence
- [IkasanESBDocumentToExclusionEventConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToExclusionEventConverter.java) - Converts to ExclusionEvent
- [IkasanESBDocumentToReplayEventConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToReplayEventConverter.java) - Converts to ReplayEvent
- [IkasanESBDocumentToModuleMetaDataConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToModuleMetaDataConverter.java) - Converts to ModuleMetaData
- [IkasanESBDocumentToConfigurationMetaDataConverter](src/main/java/org/ikasan/rest/dashboard/component/converter/IkasanESBDocumentToConfigurationMetaDataConverter.java) - Converts to ConfigurationMetaData

### Search by Identifiers

The underlying search implementation ([DataSharingController](src/main/java/org/ikasan/rest/dashboard/DataSharingController.java)) uses the `ESBSearchService.search(Set<String> identifiers, ...)` method which performs efficient ID-based lookups:

- **Solr Implementation:** Uses OR query construction: `id:"identifier1" OR id:"identifier2" OR ...`
- **MongoDB Implementation:** Uses `$in` operator: `{ _id: { $in: ["identifier1", "identifier2", ...] } }`

Both implementations support offset-based pagination and flexible sorting with default fallback to timestamp descending.

### Error Handling

All Data Sharing Services return standard HTTP status codes:
- `200 OK` - Successful query
- `401 Unauthorized` - Missing or invalid JWT token
- `400 Bad Request` - Invalid query parameters
- `500 Internal Server Error` - Server-side processing error

### Security

All Data Sharing Services require valid JWT authentication. Ensure the JWT token is included in the `Authorization` header for all requests:

```
Authorization: Bearer {JWT_TOKEN}
```

Tokens can be obtained via the [Authentication Endpoint](#authorization-endpoint).
