# bwHealthCloud Backend REST API


## Preliminaries:

The new bwHC backend includes experimental usage of [Hypermedia](https://en.wikipedia.org/wiki/HATEOAS) for easier usability and "discoverability" of API functions.
[Cross-Platform Hypertext Language](https://github.com/mikestowe/CPHL) (CPHL) -- an extension of [HAL](https://en.wikipedia.org/wiki/Hypertext_Application_Language) -- was chosen as syntactical specification for hypermedia content.

Sub-APIs now have a "hypermedia base" endpoint which returns URIs and HTTP methods of possible API actions ("links"), together with -- where appropriate -- references to JSON Schemas of corresponding JSON request bodies. 

For example, retrieval of the Catalogs API Hypermedia base at

GET http://HOST:PORT/bwhc/catalogs/api/

returns Hypermedia "links" to actual resources from the API:

```javascript
{
  "_links": {
    "base": {                        
      "href": "/bwhc/catalogs/api/",                   // self-reference to API base
      "methods": [
          "GET"
      ]
    },
    "catalog-hgnc": {
      "href": "/bwhc/catalogs/api/Coding?system=hgnc", // URI of HGNC catalog
      "methods": [
          "GET"
      ]
    },
    // Further links...
  }
}
```

As another example: A call to the API endpoint to list Patients for which data curation is required

GET http://HOST:PORT/bwhc/mtb/api/data/qc/Patient

returns an entry set of Patient resources extended by hypermedia links to actions pertaining to the respective Patient:

```javascript
{
  "entries": [
    {
      // Patient attributes...
      "birthDate": "1980-12-02",
      "gender": "male",
      "id": "2c1fb8b3-3ac0-4dfd-a56f-714cf024934a",
      "managingZPM": "Tübingen",
      //Hypermedia links:
      "_links": {
        "get-data-quality-report": {   // Action: Get the Patient's DataQualityReport
          "formats": {
            "json": {
              "mimeType": "application/json",
              "schema": "/bwhc/mtb/api/data/schema/get-data-quality-report"  // Link to DataQualityReport JSON Schema
            }
          },
          "href": "/bwhc/mtb/api/data/DataQualityReport/2c1fb8b3-3ac0-4dfd-a56f-714cf024934a",
          "methods": [
            "GET"
          ]
        },
        "get-mtbfile": {   // Action: Get the Patient's MTBFile 
          "formats": {
            "json": {
              "mimeType": "application/json",
              "schema": "/bwhc/mtb/api/data/schema/get-mtbfile"   // Link to DataQualityReport JSON Schema
            }
          },
          "href": "/bwhc/mtb/api/data/MTBFile/2c1fb8b3-3ac0-4dfd-a56f-714cf024934a",
          "methods": [
            "GET"
          ]
        },
        "delete": {
          "href": "/bwhc/mtb/api/data/Patient/2c1fb8b3-3ac0-4dfd-a56f-714cf024934a",
          "methods": [
            "DELETE"
          ]
        }
      }
    }
  ],
  "total": 1
}
```

-------
## Synthetic Data Examples API

Request a random-generated MTBFile JSON example

__GET__ http://HOST:PORT/bwhc/fake/data/api/MTBFile



-------
## Data Upload/Management and Evidence Query API

IMPORTANT NOTICE:

* See the ["shared spreadsheet"](
https://docs.google.com/spreadsheets/d/1dwntOuyitgAuxwwU4i0kMBJZQQc31UrNdpG6AFW5ZMw/edit?usp=sharing
) of MTB File parameters and their currently assumed multiplicities



-------
### Data Upload/Validation/Management API

__GET__ http://HOST:PORT/bwhc/system/api/data/



-------
#### Upload MTBFile

Required header: "Content-Type: application/json"

__POST__ http://HOST:PORT/bwhc/system/api/data/upload

__Response__:

| Case | Status Code [and response payload] |
| ---- | ----- |
| Invalid JSON                  | __400 Bad Request__ with list of syntax errors | 
| Fatal data quality issues     | __422 Unprocessable Entity__ with DataQualityReport |
| Non-fatal Data quality issues | __201 Created__ with 'Location' of created DataQualityReport |
| No data quality issues        | __200 Ok__ with 'Location' of MTBFile entry |


-------
#### Get Patients with Data Quality Issue reports

__GET__ http://HOST:PORT/bwhc/mtb/api/data/Patient


-------
#### Get a Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/MTBFile/{PatientID}


-------
#### Get DataQualityReport for a given Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/DataQualityReport/{PatientID}


-------
#### Delete all of a Patient's data

__DELETE__ http://HOST:PORT/bwhc/mtb/api/data/Patient/{PatientID}



-------
## Reporting/Query API

-------
#### Get LocalQCReport (from local ZPM site)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/LocalQCReport


-------
#### Get GlobalQCReport (combined LocalQCReport of all ZPM sites)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/GlobalQCReport



-------
### Evidence Querying API


-------
#### Query Submission

__POST__ http://HOST:PORT/bwhc/mtb/api/query

```javascript
{
  "mode": "local",                         // Query Mode: {local,federated}
  "parameters": {
    "diagnoses": ["C22.0", "C25.6", ...],  // ICD-10-GM codes
    "medicationsWithUsage": [
      {
        "code": "L01XE15",                 // ATC Medication Code,
        "usage": "used"                    // Medication usage: {recommended, used}
      },                                  
      ...                                 
    ],                                    
    "responses": ["CR","PR", ...]          // Therapy Responses (RECIST codes)
  }
}
```

__Response__: Created Query Object

```javascript
{
  "id": "b65defa0-3bef-44e2-b1f7-3dfec07e8a24",
  "querier": "TODO",
  "submittedAt": "2020-05-12T15:35:50.528746",
  "mode": "local",
  "parameters": {
      "diagnoses": [
          "C25.6"
      ],
      "medicationsWithUsage": [
          {
              "code": "L01XE15",
              "usage": "used"
          }
      ],
      "responses": [
          "PR"
      ]
  },
  "filter": {
      "ageRange": {
          "end": 0,
          "start": 0
      },
      "genders": []
  },
  "lastUpdate": "2020-05-12T13:35:50.529974Z",
}
```

-------
#### Get Query Object by ID

__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}



-------
#### Access to contents from Query ResultSet

| Resource | URL |
|----------|--------|
| Patients |__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}/Patient   |

__TODO TODO TODO__






-------
## Catalogs and ValueSet API

-------
#### Coding Systems (Catalogs)

__GET__ http://HOST:PORT/bwhc/catalogs/api/Coding/SYSTEM[?pattern=CHARS][?version=VERSION]

__GET__ http://HOST:PORT/bwhc/catalogs/api/Coding?system=SYSTEM[&pattern=CHARS][?version=VERSION]

SYSTEM must be any of:

| System Name |
|-------------|
| ICD-10-GM |
| ICD-O-3-T |
| ICD-O-3-M |
| HGNC |
| ATC |

Optional Parameter "pattern" filters for codings whose display label contains CHARS 

Optional Parameter "version" indicated requested catalog version (only different ICD-10-GM versions a.t.m)


-------
#### ValueSets

__GET__ http://HOST:PORT/bwhc/catalogs/api/ValueSet


##### Fetch a given ValueSet NAME

__GET__ http://HOST:PORT/bwhc/catalogs/api/ValueSet/NAME

__GET__ http://HOST:PORT/bwhc/catalogs/api/ValueSet?name=NAME


| ValueSet Names |
|-------|
| Geschlecht |
| DiagnoseStadium |
| Verwandtschaftsgrad |
| TherapieLinie  |
| LeitlinienTherapie-Abbruchsgrund |
| RECIST |
| ECOGPerformanceStatus |
| WHOGradingOfCNSTumors |
| LevelOfEvidence-Graduierung |
| LevelOfEvidence-Zusatzverweise |
| Proben-Art |
| Proben-Lokalisierung |
| Proben-Entnahmemethode |
| Kostenübernahme-Status |
| Kostenübernahme-Ablehnungsgrund |
| MolekularTherapie-Status |
| MolekularTherapie-Nichtumsetzungsgrund |
| MolekularTherapie-Abbruchsgrund |



