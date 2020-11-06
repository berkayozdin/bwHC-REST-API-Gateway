# bwHealthCloud Backend Manual


-----
## Installation:

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

The installation script copies all necessary files to the target directory. In case that an installation is already present, the previous configuration files are __NOT__ overwritten.


--------
## Configuration/Setup: 

--------
### Backend Application Parameters: 

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
### HTTP(S)/Communication Setup:

#### Backend API Access:

Hosts allowed to access the Backend REST API can/must be configured in __production.conf__ :

```
  filters {
    ...
    hosts {
      allowed = ["localhost"]     # TODO
    }
  }
```

See the [Play Framework Docs](https://www.playframework.com/documentation/2.8.x/AllowedHostsFilter#Allowed-hosts-filter) for details.


#### bwHC Node Peer-to-Peer Communication:

URLs of other bwHC Nodes are configured in __bwhcConnectorConfig.xml__:

```xml
<?xml version="1.0" encoding="utf-8"?>
<bwHC>
  <ZPM site="Freiburg"   baseURL="TODO"/>
  <ZPM site="Heidelberg" baseURL="TODO"/>
  <ZPM site="Tübingen"   baseURL="TODO"/>
  <ZPM site="Ulm"        baseURL="TODO"/>
</bwHC>
```

#### Setting up HTTPS / Securing Backend API Access: 


##### Set up NGINX as Reverse Proxy to handle SSL-Termination (HTTPS)

Here's a sample configuration to set up NGINX as reverse proxy to handle SSL-Termination for the bwHC Backend:

```nginx
http {
  ...

  ssl_certificate      /path/to/server_cert.pem;
  ssl_certificate_key  /path/to/server_key.key;

  server {

    listen                   443 ssl;
    server_name              xxxxx;          # TODO

    #IP-Filter
#    allow                    127.0.0.1;   # Activate as required
#    deny                     all;         # Activate as required

    # This proxies all requests to the backend service
    location /bwhc {
      proxy_pass http://localhost:9000;    # Adapt Host/Port as required
    }
  }
  
}
```

__IMPORTANT NOTE/SUGGESTION__: In this set-up, the Reverse Proxy should be set as sole "allowed host" for the Backend API in production.conf.

See [NGINX Admin Guide](https://docs.nginx.com/nginx/admin-guide/) for detailed reference.


##### Set up NGINX as Proxy for bwHC peers:

Here's a sample configuration to set up a virtual NGINX server to act as proxy "into the bwHC", i.e. for outgoing requests to other bwHC sites:

```nginx
http {
  ...

  server {
 
    listen 127.0.0.1:8080;   # Adapt as required

    #IP-Filter
    allow  127.0.0.1;        # This proxy should only accept requests from local bwHC backend
    deny   all;

#    proxy_ssl_certificate        /path/to/client.pem;
#    proxy_ssl_certificate_key    /path/to/client.key;
#    proxy_ssl_session_reuse      on;

    # TO CHECK: add trusted CA root certificate?
    # proxy_ssl_trusted_certificate  /path/to/trusted_ca_cert.pem;
    # proxy_ssl_verify               on;
    # proxy_ssl_verify_depth         2;

    location /Freiburg {
      proxy_pass  http://HOST:PORT/bwhc/system/api;  # TODO: Adapt Host/PORT
    }

    location /Heidelberg {
      # ... adapt above
    }

    ...
  }
}
```

The corresponding settings in __bwhcConnectorConfig.xml__ would then be:

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
### Logging (SLF4J):

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

See SLF4J/Logback reference for details.


------
### Random Data Generation Config:

For test purposes, the system can be configured to be filled with randomly generated MTBFiles,
in case that no real data is present upon startup.

In Bash-script __bwhc-backend-service__, uncomment variable __N_RANDOM_FILES__ and optionally adjust the pre-defined value.
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
## Operation:

Start/stop the backend service via Bash-script __bwhc-backend-service__
```
foo@bar: ./bwhc-backend-service start
...
foo@bar: ./bwhc-backend-service stop
```

