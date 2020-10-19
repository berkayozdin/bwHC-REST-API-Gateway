# bwHealthCloud Backend REST API


-------
## Data Upload/Management and Evidence Query API


IMPORTANT NOTICE:


* See the ["shared spreadsheet"](
https://docs.google.com/spreadsheets/d/1dwntOuyitgAuxwwU4i0kMBJZQQc31UrNdpG6AFW5ZMw/edit?usp=sharing
) of MTB File parameters and their currently assumed multiplicities




-------
#### Random Data Examples API

Request a random-generated MTBFile JSON example


__GET__ http://HOST:PORT/bwhc/fake/data/api/MTBFile



-------
### Data Upload/Validation/Management API

-------
#### Upload MTBFile

Required header: "Content-Type: application/json"

__POST__ http://HOST:PORT/bwhc/system/api/data/upload

__Response__:

| Case | Status Code [and response payload] |
| ---- | ----- |
| Invalid JSON | __400 Bad Request__ with list of syntax errors | 
| Fatal data quality issues | __422 Unprocessable Entity__ with DataQualityReport |
| Non-fatal Data quality issues | __201 Created__ with 'Location' of created DataQualityReport |
| No data quality issues | __200 Ok__ with 'Location' of MTBFile entry |




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
| Patients                      |__GET__ http://HOST:PORT/bwhc/mtb/api/query/{QueryID}/Patient   |

__TODO TODO TODO__






-------
## Catalogs and ValueSet API

-------
#### Coding Systems (Catalogs)

__GET__ http://HOST:PORT/bwhc/catalogs/api/Coding/SYSTEM[?pattern=CHARS]

__GET__ http://HOST:PORT/bwhc/catalogs/api/Coding?system=SYSTEM[&pattern=CHARS]

SYSTEM must be any of:

| System Name |
|-------------|
| ICD-10-GM |
| ICD-O-3-T |
| ICD-O-3-M |
| HGNC |
| ATC |

Parameter "pattern" filters for codings whose display label contains CHARS 


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



