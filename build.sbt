import sbt.Keys._



name := "bwhc-rest-api-gateway"
organization in ThisBuild := "de.bwhc"
scalaVersion in ThisBuild := "2.13.1"
version in ThisBuild := "1.0-SNAPSHOT"


scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-unchecked",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-feature",
  "-deprecation"
)


libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play"              % "5.0.0" % "test",

  "de.bwhc"                %% "authentication-api"              % "1.0-SNAPSHOT",
//  "de.bwhc"                %% "fake-session-manager"            % "1.0-SNAPSHOT",
  "de.bwhc"                %% "session-manager-impl"            % "1.0-SNAPSHOT",

   // Fake Data dependencies
  "de.bwhc"                %% "mtb-dto-generators"              % "1.0-SNAPSHOT",
  "de.bwhc"                %% "fhir-mappings"                   % "1.0-SNAPSHOT",

  // User Service dependencies
  "de.bwhc"                %% "user-service-api"                % "1.0-SNAPSHOT",
  "de.bwhc"                %% "user-service-impl"               % "1.0-SNAPSHOT",
  "de.bwhc"                %% "user-service-fs-repos"           % "1.0-SNAPSHOT",

  // Data Entry/Validation Service dependencies
  "de.bwhc"                %% "data-entry-service-api"          % "1.0-SNAPSHOT",
  "de.bwhc"                %% "data-entry-service-impl"         % "1.0-SNAPSHOT",
  "de.bwhc"                %% "data-entry-service-dependencies" % "1.0-SNAPSHOT",

  // Query Service dependencies
  "de.bwhc"                %% "query-service-api"               % "1.0-SNAPSHOT",
  "de.bwhc"                %% "query-service-impl"              % "1.0-SNAPSHOT",
//  "de.bwhc"                %% "bwhc-broker-connector"           % "1.0-SNAPSHOT",
  "de.bwhc"                %% "bwhc-connector"                  % "1.0-SNAPSHOT",
  "de.bwhc"                %% "fs-mtbfile-db"                   % "1.0-SNAPSHOT",

  // Catalog dependencies
  "de.bwhc"                %% "hgnc-api"                        % "1.0-SNAPSHOT",
  "de.bwhc"                %% "hgnc-impl"                       % "1.0-SNAPSHOT",
  "de.bwhc"                %% "icd-catalogs-api"                % "1.0-SNAPSHOT",
  "de.bwhc"                %% "icd-catalogs-impl"               % "1.0-SNAPSHOT",
  "de.bwhc"                %% "medication-catalog-api"          % "1.0-SNAPSHOT",
  "de.bwhc"                %% "medication-catalog-impl"         % "1.0-SNAPSHOT",
)


lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
  )


