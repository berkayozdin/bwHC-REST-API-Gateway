# bwHealthCloud User Management and Authentication REST API

-------


-------
## Authentication API

#### Login

__POST__ http://HOST:PORT/bwhc/user/api/login

with Credentials either as Form payload (x-www-form-urlencoded):

```javascript
username=...
password=...
```

OR

Json payload (application/json)

```javascript
{
  "username" : "...",
  "password" : "..."
}
```

__Response__: OAuth 2.0 Bearer token

```javascript
{
  "access_token": "9e7f6d00-4e1a-4f7f-8bb0-188fb1a23818",
  "created_at": 1608031839297,
  "expires_in": 3600,
  "scope": "bwhc",
  "token_type": "Bearer"
}
```

#### Who am I?

__GET__ http://HOST:PORT/bwhc/user/api/whoami

with Header: 'Authorization: Bearer _access_token_'


__Response__: User in session

```javascript
{
  "id" : "fb53eb3b-27a5-4045-acdc-f31ff514bf9a",
  "username" : "test_user",
  ...,
  "lastUpdate" : "2020-10-21T07:43:24.213325Z"
}
```

#### What functions do I have access to?

__GET__ http://HOST:PORT/bwhc

with Header: 'Authorization: Bearer _access_token_'


__Response__: URLs to accessible links/actions

```javascript
{
    "_actions": {
        "logout": {
            "href": "/bwhc/user/api/logout",
            "method": "POST"
        },
        "whoami": {
            "href": "/bwhc/user/api/whoami",
            "method": "GET"
        }
    },
    "_links": {
        ...
        "data-quality-api": {
            "href": "/bwhc/mtb/api/data-quality/"
        },
        "etl-api": {
            "href": "/bwhc/etl/api/"
        },
        "query-api": {
            "href": "/bwhc/mtb/api/query/"
        },
        "reporting-api": {
            "href": "/bwhc/mtb/api/reporting/"
        },
        ...
    }
}
```

#### Logout

__POST__ http://HOST:PORT/bwhc/user/api/logout

with Header: 'Authorization: Bearer _access_token_'



-------
## User API

-------
#### User Creation (Admin only)

__POST__ http://HOST:PORT/bwhc/user/api/users

```javascript
{
  "username" : "test_user",
  "password" : "top-secret",
  "givenName" : "Ute",
  "familyName" : "Musterfrau",
  "roles" : [
     "Admin",
     "Documentarist",
     "LocalZPMCoordinator",
     "GlobalZPMCoordinator",
     "MTBCoordinator",
     "Researcher" 
  ]
}
```

__Response__: Created User (w/o password)

```javascript
{
  "id" : "fb53eb3b-27a5-4045-acdc-f31ff514bf9a",
  "username" : "test_user",
  "givenName" : "Ute",
  "familyName" : "Musterfrau",
  "status" : "Active",
  "roles" : [
     "Admin",
     "Documentarist",
     "LocalZPMCoordinator",
     "GlobalZPMCoordinator",
     "MTBCoordinator",
     "Researcher"
  ],
  "registeredOn" : "2020-10-21",
  "lastUpdate" : "2020-10-21T07:43:24.213325Z"
}
```

#### Assignable User Roles:
-------

| Role | Code |
| ---- | ---- |
| Administrator            | Admin |
| Documentarist            | Documentarist |
| ZPM-Coordinator (local)  | LocalZPMCoordinator |
| ZPM-Coordinator (global) | GlobalZPMCoordinator |
| MTB-Coordinator (local)  | MTBCoordinator |
| Researcher               | Researcher |


-------
#### Get List of all Users (Admin only)

__GET__ http://HOST:PORT/bwhc/user/api/users


-------
#### Get User by ID (User her-/himself and Admin)

__GET__ http://HOST:PORT/bwhc/user/api/users/{UserID}


-------
#### Update User Data (User her-/himself and Admin)

__PUT__ http://HOST:PORT/bwhc/user/api/users/{UserID}

```javascript
{
  "id" : "fb53eb3b-27a5-4045-acdc-f31ff514bf9a",
  "username" : "test_user",            // Optional
  "password" : "new-top-secret",       // Optional
  "givenName" : "Ute",                 // Optional
  "familyName" : "Musterfrau"          // Optional
}
```


-------
#### Update a User's Roles (Admin only)

__PUT__ http://HOST:PORT/bwhc/user/api/users/{UserID}/roles

```javascript
{
  "id" : "fb53eb3b-27a5-4045-acdc-f31ff514bf9a",
  "roles" : [
     "Documentarist",
     "LocalZPMCoordinator",
     "MTBCoordinator",
     "Researcher"
  ]
}
```

-------
#### Delete a User (Admin only)

__DELETE__ http://HOST:PORT/bwhc/user/api/users/{UserID}

