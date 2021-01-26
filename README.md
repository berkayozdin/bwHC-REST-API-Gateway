# bwHealthCloud Backend REST API


## 1. Preliminaries:


### 1.1 MTBFile Model:

See the ["shared spreadsheet"](
https://docs.google.com/spreadsheets/d/1dwntOuyitgAuxwwU4i0kMBJZQQc31UrNdpG6AFW5ZMw/edit?usp=sharing
) of MTB File parameters and their currently defined multiplicities and JSON representation.


### 1.2 Hypermedia

The new bwHC backend includes experimental usage of [Hypermedia](https://en.wikipedia.org/wiki/HATEOAS) to be truly RESTful, i.e. to allow easier discoverability and usage of API functions.
The used representation for hypermedia content is essentially [Hypertext Application Language](https://en.wikipedia.org/wiki/Hypertext_Application_Language) (HAL) with
custom extensions inspired from [SIREN](https://github.com/kevinswiber/siren) and [CPHL](https://github.com/mikestowe/CPHL) to allow specifying _actions_ on resources in addition to just links/relations.

The backend now has a "__hypermedia API entry point__":

__GET__ http://HOST:PORT/bwhc

from where hypermedia links to the various accessible sub-APIs can be followed:

```javascript
{
  "_actions": {                            // Possible Actions:
    "logout": {                            // Logout
        "method": "POST",                  // HTTP Method
        "href": "/bwhc/user/api/logout"    // URI (relative)
    },
    ...
  },
  "_links": {
    "catalogs-api": {                     
        "href": "/bwhc/catalogs/api/"      // URI (relative) to Catalogs API
    },
    ...
    "etl-api": {
        "href": "/bwhc/etl/api/"           // URI (relative) to ETL API
    }
  }
}
```
For instance, following the link to the "__etl-api__"

__GET__ http://HOST:PORT/bwhc/etl/api/

returns a description of Links/Actions available for this sub-API:

```javascript
{
  "_actions": {
    "upload-mtbfile": {                                    // Action: Upload MTBFile
      "method": "POST"                                     // HTTP Method
      "href": "/bwhc/etl/api/MTBFile",                     // URI (relative)

      "formats": {                                         // Format specifications of request payloads for the Action
        "application/json": {
          "href": "/bwhc/etl/api/schema/upload-mtbfile"    // Link to JSON Schema specification for Content-Type 'application/json'
        }
      },
    },
    ...
    "delete-patient": {                                    // Action: Delete a Patient's data
      "method": "DELETE",
      "href": "/bwhc/etl/api/Patient/{id}"
    },
  },
  "_links": {
    ...
  }
}
```


-------
## 2. Synthetic Data Examples API

Request a random-generated MTBFile JSON example

__GET__ http://HOST:PORT/bwhc/fake/data/api/MTBFile



-------
## 3. Data Upload/Management and Evidence Query API

-------
### 3.1 Data Upload/Deletion API for external Systems

__IMPORTANT__: This sub-API is not secured via a user login mechanism. 
Access to this URL path SHOULD be restricted in the reverse proxy.


-------
#### 3.1.1 Upload MTBFile

Required header: "Content-Type: application/json"

__POST__ http://HOST:PORT/bwhc/etl/api/MTBFile
__POST__ http://HOST:PORT/bwhc/etl/api/data/upload

__Response__:

| Case | Status Code [and response payload] |
| ---- | ----- |
| Invalid JSON                  | __400 Bad Request__ with list of syntax errors | 
| Fatal data quality issues     | __422 Unprocessable Entity__ with DataQualityReport |
| Non-fatal Data quality issues | __201 Created__ |
| No data quality issues        | __200 Ok__ |


-------
#### 3.1.2 Delete a Patient's data

__DELETE__ http://HOST:PORT/bwhc/etl/api/Patient/{PatientID}

__DELETE__ http://HOST:PORT/bwhc/etl/api/MTBFile/{PatientID}




-------
### 3.2 Data Quality Feedback API

-------
#### 3.2.1 Get Patients with Data Quality Issue reports

__GET__ http://HOST:PORT/bwhc/mtb/api/data/Patient


-------
#### 3.2.2 Get a Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/MTBFile/{PatientID}


-------
#### 3.2.3 Get DataQualityReport for a given Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/DataQualityReport/{PatientID}


-------
#### 3.2.4 Delete a Patient's data

__DELETE__ http://HOST:PORT/bwhc/mtb/api/data/Patient/{PatientID}




-------
## 4. ZPM-QC-Reporting / Query API

### 4.1 Reporting 
-------
#### 4.1.1 Get LocalQCReport (from local ZPM site)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/QCReport?scope=local

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/LocalQCReport


-------
#### 4.1.2 Get GlobalQCReport (combined LocalQCReport of all ZPM sites)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/QCReport?scope=global

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/GlobalQCReport



-------
### 4.2 Evidence Querying API


-------
#### 4.2.1 Query Submission

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
#### 4.2.2 Get Query Object by ID

__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}



-------
#### 4.2.3 Access to contents from Query ResultSet

| Resource | URL |
|----------|--------|
| Patients |__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}/Patient   |

__TODO TODO TODO__




-------
## 5 Catalogs and ValueSet API

-------
### 5.1 Coding Systems (Catalogs)

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
### 5.2 ValueSets

__GET__ http://HOST:PORT/bwhc/catalogs/api/ValueSet

#### 5.2.1 Fetch a given ValueSet NAME

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



