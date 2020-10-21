# bwHealthCloud User Management and Authentication REST API

-------

-------
## User API

-------
#### User Creation (Admin only)

__POST__ http://HOST:PORT/bwhc/user/api/user

```javascript
{
  "username" : "test_user",
  "password" : "top-secret",
  "humanName" : {
    "given" : "Ute",
    "family" : "Musterfrau"
  },
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
  "humanName" : {
    "given" : "Ute",
    "family" : "Musterfrau"
  },
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

__GET__ http://HOST:PORT/bwhc/user/api/user


-------
#### Get User by ID (User her-/himself and Admin)

__GET__ http://HOST:PORT/bwhc/user/api/user/{UserID}


-------
#### Update User Data (User her-/himself and Admin)

__PUT__ http://HOST:PORT/bwhc/user/api/user/{UserID}

```javascript
{
  "id" : "fb53eb3b-27a5-4045-acdc-f31ff514bf9a",
  "username" : "test_user",            // Optional
  "password" : "new-top-secret",       // Optional
  "humanName" : {                      // Optional
    "given" : "Ute",
    "family" : "Musterfrau"
  }
}
```


-------
#### Update a User's Roles (Admin only)

__PUT__ http://HOST:PORT/bwhc/user/api/user/{UserID}/roles

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
## Authentication API


#### Login

__POST__ http://HOST:PORT/bwhc/user/api/login

with Credentials either as Form payload (x-www-url-formencoded):

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

TODO: Session token



#### Logout

__POST__ http://HOST:PORT/bwhc/user/api/logout








