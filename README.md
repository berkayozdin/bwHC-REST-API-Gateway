# bwHealthCloud Backend REST API


## 1. Preliminaries:


### 1.1 MTBFile Model:

See the ["shared spreadsheet"](
https://docs.google.com/spreadsheets/d/1dwntOuyitgAuxwwU4i0kMBJZQQc31UrNdpG6AFW5ZMw/edit?usp=sharing
) of MTB File parameters and their currently defined multiplicities and JSON representation.


### 1.2 Hypermedia

The bwHC backend includes experimental usage of [Hypermedia](https://en.wikipedia.org/wiki/HATEOAS) to be truly RESTful, i.e. to allow easier discoverability and usage of API functions.
The used representation for hypermedia content is essentially [Hypertext Application Language](https://en.wikipedia.org/wiki/Hypertext_Application_Language) (HAL) with
custom extensions inspired from [SIREN](https://github.com/kevinswiber/siren) and [CPHL](https://github.com/mikestowe/CPHL) to allow specifying _actions_ on resources in addition to just links/relations.

The backend has a "__hypermedia API entry point__":

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
## 3 ETL API (Data upload and deletion)

__IMPORTANT__: This sub-API is not secured via a user login mechanism. 
Access to this URL path SHOULD be restricted in the reverse proxy.


-------
### 3.1 Upload MTBFile

__POST__ http://HOST:PORT/bwhc/etl/api/MTBFile

__POST__ http://HOST:PORT/bwhc/etl/api/data/upload

Supported formats (HTTP Header "Content-type"):
- application/json
- application/fhir+json (experimental)

__Response__:

| Case | Status Code [and response payload] |
| ---- | ----- |
| Invalid JSON                  | __400 Bad Request__ with list of syntax errors | 
| Fatal data quality issues     | __422 Unprocessable Entity__ with DataQualityReport |
| Non-fatal Data quality issues | __201 Created__ |
| No data quality issues        | __200 Ok__ |


-------
### 3.2 Delete a Patient's data

__DELETE__ http://HOST:PORT/bwhc/etl/api/Patient/{PatientID}

__DELETE__ http://HOST:PORT/bwhc/etl/api/MTBFile/{PatientID}




-------
## 4. Data Quality Feedback API

-------
### 4.1 Get Patients with Data Quality Issue reports

__GET__ http://HOST:PORT/bwhc/mtb/api/data/Patient


-------
### 4.2 Get a Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/MTBFile/{PatientID}


-------
### 4.3 Get DataQualityReport for a given Patient's MTBFile

__GET__ http://HOST:PORT/bwhc/mtb/api/data/DataQualityReport/{PatientID}


-------
### 4.4 Delete a Patient's data

__DELETE__ http://HOST:PORT/bwhc/mtb/api/data/Patient/{PatientID}




-------
## 5. ZPM-QC-Reporting / Query API

### 5.1 Reporting 
-------
#### 5.1.1 Get LocalQCReport (from local ZPM site)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/QCReport?scope=local

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/LocalQCReport


-------
#### 5.1.2 Get GlobalQCReport (combined LocalQCReport of all ZPM sites)

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/QCReport?scope=global

__GET__ http://HOST:PORT/bwhc/mtb/api/reporting/GlobalQCReport



-------
### 5.2 Evidence Querying API


-------
#### 5.2.1 Query Submission

__POST__ http://HOST:PORT/bwhc/mtb/api/query

```javascript
{
  "mode": {
      "code": "local",                  // Values: "local", "federated"
  },
  "parameters": {
      "diagnoses": [
          {
              "code": "C25.1",
          }
      ],
      "medicationsWithUsage": [
          {
              "medication": {
                  "code": "L01XX59",   // ATC code
              },
              "usage": {
                  "code": "used",      // Values: "used", "recommended"
              }
          }
      ],
      "mutatedGenes": [
          {
              "code": "HGNC:22",       // HGNC ID
          }
      ],
      "responses": [
          {
              "code": "SD",            // RECIST code 
          }
      ]
  }
}
```

__Response__: Created Query Object

```javascript
{
    "id": "6033bc57-1ec5-4c32-a1e5-cf97355ee950",
    "querier": "082e1d9e-9d94-43a5-8a9f-1aed7118f6e8",
    "submittedAt": "2021-12-14T08:28:31.695372Z",
    "lastUpdate": "2021-12-14T08:28:31.695389Z",
    "mode": {
        "code": "local",
        "display": "Lokal",
        "system": "Query-Mode"
    },
    "parameters": {
        "diagnoses": [
            {
                "code": "C25.1",
                "display": "Bösartige Neubildung: Pankreaskörper",
                "system": "ICD-10-GM",
                "version": "2022"
            }
        ],
        "medicationsWithUsage": [
            {
                "medication": {
                    "code": "L01XX59",
                    "display": "Enasidenib",
                    "system": "ATC",
                    "version": "2021"
                },
                "usage": {
                    "code": "used",
                    "system": "Drug-Usage"
                }
            }
        ],
        "mutatedGenes": [
            {
                "code": "HGNC:22",
                "display": "adeno-associated virus integration site 1",
                "system": "HGNC"
            }
        ],
        "responses": [
            {
                "code": "SD",
                "display": "Stable Disease",
                "system": "RECIST"
            }
        ]
    },
    "filter": {
        "ageRange": {
            "l": 0,
            "r": 0
        },
        "genders": [],
        "vitalStatus": []
    },
    "zpms": []
    "_links": {
        "base": {
            "href": "/bwhc/mtb/api/query/"
        },
        "molecular-therapies": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/molecular-therapies"
        },
        "ngs-summaries": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/ngs-summaries"
        },
        "patients": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/patients"
        },
        "result-summary": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/result-summary"
        },
        "self": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950"
        },
        "therapy-recommendations": {
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/therapy-recommendations"
        }
    },
    "_actions": {
        "apply-filter": {
            "formats": {
                "application/json": {
                    "href": "/bwhc/mtb/api/query/schema/apply-filter"
                }
            },
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950/filter",
            "method": "POST"
        },
        "update": {
            "formats": {
                "application/json": {
                    "href": "/bwhc/mtb/api/query/schema/update"
                }
            },
            "href": "/bwhc/mtb/api/query/6033bc57-1ec5-4c32-a1e5-cf97355ee950",
            "method": "POST"
        }
    },
}

```

-------
#### 5.2.2 Get Query Object by ID

__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}



-------
#### 5.2.3 Access to contents from Query ResultSet

See hypermedia links in example above


-------
## 6 Catalogs and ValueSet API

-------
### 6.1 Coding Systems (Catalogs)

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
### 6.2 ValueSets

__GET__ http://HOST:PORT/bwhc/catalogs/api/ValueSet

#### 6.2.1 Fetch a given ValueSet NAME

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



