![Problem Domain](../../developer/docs/quickstart-images/Ikasan-title-transparent.png)
# Rest Dashboard
The Ikasan Dashboard exposes a number of REST service endpoints that allow for integration modules
to push both transient data to the dashboard as well as data that describes the runtime details of the 
module along with the runtime state. The dashboard acts as an aggregator for the transient data and pushes
the transient data to a Solr text index or a database depending on the manner in which Ikasan is configured. 
The runtime metadata is also pushed to a data store and is used to build visual representations of the underlying topology,
while the runtime state can be used for monitoring and control purposes. 

All Ikasan Dashboard REST service endpoints require Authorisation HTTP header to be send along with data payload. 
Authorisation Header has a form of "Bearer {JWT TOKEN}". The {JWT TOKEN} can be obtained from Authorisation Endpoint by
providing user credentials.

## Authorization Endpoint
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

## Error Harvesting Service
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

## Exclusions Harvesting Service
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

## Metrics Harvesting Service
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

## Replay Events Harvesting Service
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

## Wiretap Events Harvesting Service
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

## Metadata Service
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

## Configuration Service
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