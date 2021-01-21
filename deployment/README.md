# bwHealthCloud Backend Manual


-----
## 1. Installation:

* Unzip application package

* Change into unpacked application directory __bwhc-backend__

* Run installation script with target directory as parameter

```
foo@bar: unzip bwhc-backend.zip
...
foo@bar: cd bwhc-backend/
foo@bar: ./install.sh /path/to/target/dir
```

The target directory is created if non-existent.

The installation script copies all necessary files to the target directory. In case that configuration files from a previous installation are already present, the previous files are __NOT__ overwritten.


--------
## 2. Configuration/Setup: 

--------
### 2.1 Backend Application Parameters: 

In Bash-script __config__, set the parameters marked with TODO

```bash

export BASE_DIR=$(pwd)            # Optional: Adapt BASE_DIR
                                  
export BWHC_PORT=9000             # Optional: Adapt HTTP Port

export BWHC_CONNECTOR_CONFIG=$BASE_DIR/bwhcConnectorConfig.xml
                                  
export ZPM_SITE=...               # TODO: Set local ZPM Site name

export BWHC_USER_DB_DIR=...       # TODO: Set absolute path to dir where User Service stores data

export BWHC_DATA_ENTRY_DIR=...    # TODO: Set absolute path to dir where Data Entry/Validation Service stores data

export BWHC_QUERY_DATA_DIR=...    # TODO: Set absolute path to dir where Query/Reporting Service stores data

```

-------
### 2.2 HTTP(S)/Communication Setup:

#### 2.2.1 Backend API Access:

Valid hosts for the Backend REST API can/must be configured in __production.conf__ :

```
  filters {
    ...
    hosts {
      allowed = ["localhost",...] # TODO: Add IP and/or domain name of host VM
    }
  }
```

See the [Play Framework Docs](https://www.playframework.com/documentation/2.8.x/AllowedHostsFilter#Allowed-hosts-filter) for details.


#### 2.2.2 bwHC Node Peer-to-Peer Communication:

URLs of other bwHC nodes are configured in __bwhcConnectorConfig.xml__:

```xml
<?xml version="1.0" encoding="utf-8"?>
<bwHC>
  <ZPM site="Freiburg"   baseURL="TODO"/>
  <ZPM site="Heidelberg" baseURL="TODO"/>
  <ZPM site="Tübingen"   baseURL="TODO"/>
  <ZPM site="Ulm"        baseURL="TODO"/>
</bwHC>
```
These URLs must point to the respective bwHC node's "System API" base URL, i.e.

https://HOST:PORT/bwhc/system/api


#### 2.2.3 Setting up HTTPS / Securing Backend API Access: 


##### 2.2.3.1 Set up NGINX as Reverse Proxy to handle SSL-Termination (HTTPS)

Here's a sample configuration to set up NGINX as reverse proxy to handle SSL-Termination for the bwHC Backend:

```nginx
http {
  # ...

  ssl_certificate      /path/to/server_cert.pem;
  ssl_certificate_key  /path/to/server_key.key;

  server {

    listen       443 ssl;
    server_name  xxxxxxx;  #TODO

    #IP-Filter
#    allow        127.0.0.1;   # Activate as required
#    deny         all;         # Activate as required

    # Forward requests for /bwhc/... to the backend service
    location /bwhc {
      proxy_pass http://BACKEND_HOST:PORT;    # Adapt Host/Port as required
    }

  }
  
}
```

See [NGINX Admin Guide](https://docs.nginx.com/nginx/admin-guide/) for detailed reference.


##### 2.2.3.2 Set up NGINX for Client Certificate Authentication (mutual SSL)

There are __2 possible ways__ to configure Nginx to secure the bwHC System API endpoints via mutual SSL.

##### Variant 1:

In config from 2.2.3.1, add a 'location' to perform client verification specifically for calls to System API URI:

```nginx
http {
  #... 
  server {

    # ...

    ssl_client_certificate  /path/to/ca-cert.pem;  # Path to trusted CA certificate
                                                   # from which client certificates originate

    ssl_verify_client       optional;              # Required only in location below
    ssl_verify_depth        2;

    # Require successful client certificate verification
    # for all calls to System API
    location =/bwhc/system/api/ {
      if ($ssl_client_verify != "SUCCESS"){
         return 403;
      }
      proxy_pass http://BACKEND_HOST:PORT;
    }

    # ... Rest from config from 2.2.3.1
    
  }
}
```

##### Variant 2:

Use a separate virtual server to handle client verification, in combination with re-direction of all calls to the System API to this reverse proxy:
 

```nginx
http {
  #...

  server {

    listen       443 ssl;
    server_name  yyyyyy;  #TODO

    # ...

    # Setting ssl_trusted_certificate seems more fitting than ssl_client_certificate below,
    # but throws an error
    #ssl_trusted_certificate  /path/to/ca-cert.pem;  # Path to trusted CA certificate
    ssl_client_certificate   /path/to/ca-cert.pem;   # Path to trusted CA certificate
    ssl_verify_client        on;
    ssl_verify_depth         2;

    location =/bwhc/system/api/ {
      proxy_pass http://BACKEND_HOST:PORT;
    }

  }

  server {

    server_name  ssl_proxy;

    # ...

    # Ensure that all calls to System API endpoints
    # are directed to mutual SSL reverse proxy above
    location /bwhc/system/api {
      return 308 https://localhost:443/bwhc/system/api;
    }

    # ...
  }
}
```


##### 2.2.3.3 Set up NGINX as Proxy for bwHC peers:

Here's a sample configuration to set up a virtual NGINX server to act as proxy "into the bwHC", i.e. for outgoing requests to other bwHC sites:

```nginx
http {

  #...

  server {
 
    listen 127.0.0.1:8080;   # Adapt as required

    #IP-Filter
    allow  127.0.0.1;        # This proxy should only accept requests from local bwHC backend
    deny   all;

    # Configuration of client certificate to use for mutual SSL:
    proxy_ssl_certificate        /path/to/client_cert.pem;
    proxy_ssl_certificate_key    /path/to/client.key;
    proxy_ssl_session_reuse      on;

    # Remote server certificate verification
    proxy_ssl_verify               off;   # Deactivated 
    # proxy_ssl_trusted_certificate  /path/to/ca_cert_chain.pem;
    # proxy_ssl_verify_depth         2;

    location /Freiburg {
      proxy_pass  https://HOST:PORT/bwhc/system/api;  # TODO: Adapt HOST/PORT
    }

    location /Heidelberg {
      # ... adapt above
    }

    # repeat...
  }
}
```

The corresponding URL settings in __bwhcConnectorConfig.xml__ would then be:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bwHC>
  <ZPM site="Freiburg"   baseURL="http://localhost:8080/Freiburg"/>
  <ZPM site="Heidelberg" baseURL="http://localhost:8080/Heidelberg"/>
  <!--
     TODO: Other sites...
   -->
</bwHC>
```

------
### 2.3 Logging Configuration (SLF4J):

In __logback.xml__, set property __LOG_DIR__ to the desired logging output directory.
Also uncomment the __FILE__ logging __appender__ and __appender-ref__. This activates logging to a daily changing log file.

```xml
<?xml version="1.0" encoding="utf-8"?>

<configuration scan="true">

  <property name="LOG_DIR"  value="/path/to/log/dir"/>  <!-- TODO!!! -->
  <property name="LOG_FILE" value="bwhealthcloud"/>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

<!--
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/${LOG_FILE}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/${LOG_FILE}-%d{yyyy-MM-dd}.log</fileNamePattern>
      <maxHistory>30</maxHistory>
      <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
      <pattern>%d [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
-->
  <root level="INFO">
    <appender-ref ref="STDOUT"/>
<!--
    <appender-ref ref="FILE"/>
-->
  </root>
</configuration>

```
Optionally also adjust the __logging level__: TRACE, DEBUG, INFO, WARN, ERROR

See [SLF4J/Logback](http://logback.qos.ch/manual/configuration.html) reference for details.


------
### 2.4 Random Data Generation Config:

For test purposes, the system can be configured to be filled with randomly generated MTBFiles.

__NOTE__: This setting only takes effect in case that __no persisted data__ is present upon startup.

In Bash-script __bwhc-backend-service__, uncomment variable __N_RANDOM_FILES__ and optionally adjust the pre-defined value.

__WARNING__: The random generated data will all be kept in memory, so avoid excessively large numbers!

Then uncomment the JVM-parameter setting __-Dbwhc.query.data.generate__ and include it in the application startup command, as shown below (indented command):

```bash
  ...
  
  N_RANDOM_FILES=50
  ...
  
  $BWHC_APP_DIR/bin/bwhc-rest-api-gateway \
    -Dconfig.file=$BASE_DIR/production.conf \
    -Dbwhc.zpm.site=$ZPM_SITE \
    -Dbwhc.data.entry.dir=$BWHC_DATA_ENTRY_DIR \
    -Dbwhc.query.data.dir=$BWHC_QUERY_DATA_DIR \
    -Dbwhc.user.data.dir=$BWHC_USER_DB_DIR \
    -Dbwhc.connector.configFile=$BWHC_CONNECTOR_CONFIG \
  -Dbwhc.query.data.generate=$N_RANDOM_FILES \        #This command is commented in default bwhc-backend-service
    -Dhttp.port=$BWHC_PORT &

  ...
```

-------
## 3. Operation:

Start/stop the backend service via Bash script __bwhc-backend-service__
```
foo@bar: ./bwhc-backend-service start
...
foo@bar: ./bwhc-backend-service stop
```

