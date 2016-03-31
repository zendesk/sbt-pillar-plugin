# sbt-pillar - manage Cassandra migrations from sbt

[![Build Status](https://travis-ci.org/inoio/sbt-pillar-plugin.png?branch=master)](https://travis-ci.org/inoio/sbt-pillar-plugin)

Based off the work done by https://github.com/inoio/sbt-pillar-plugin but differs by:
* Allow use of Authentication credentials
* Allow passing in hosts as a comma-separated string
* Will allow use of NetworkTopologyStrategy for creating keyspaces.

This sbt plugin allows to run Cassandra schema/data migrations from sbt (using [pillar](https://github.com/comeara/pillar)).
 For details on migration files check out the [pillar documentation](https://github.com/comeara/pillar#migration-files).
 The cassandra connection configuration is not based on pillar but we're using our own format (see [Configuration](#configuration)).

The plugin is built for sbt 0.13.6+.

## Installation

To install the plugin you have to add it to `project/plugins.sbt`:
```
addSbtPlugin("com.42dragons" %% "sbt-pillar" % "0.1.0")
```

## Configuration

Add appropriate configuration to `build.sbt` like this:
```
pillarConfigFile in ThisBuild := file("db/pillar.conf")
pillarMigrationsDir in ThisBuild := file("db/migrations")
```

The shown configuration assumes that the settings for your cassandra are configured in `db/pillar.conf` and that pillar migration files are kept in `db/migrations` (regarding the format of migration files
check out the [pillar documentation](https://github.com/comeara/pillar#migration-files)).

An example configuration file (based on typesafe-config) is:
```
development {
  cassandra {
    keyspace = "pigeon"
    hosts = "localhost"
    port = 9042
    replicationFactor = 1
    defaultConsistencyLevel = 1
    replicationStrategy = "SimpleStrategy"
  }
}

test = ${development}
test {
  cassandra {
    keyspace = "pigeon_test"
  }
}

master {
  cassandra {
    hosts = ${?CASSANDRA_HOSTS}
    port = 9042
    keyspace = ${?CASSANDRA_KEYSPACE}
    username = ${?CASSANDRA_USERNAME}
    password = ${?CASSANDRA_PASSWORD}
    replicationFactor = ${?CASSANDRA_REPLICATION_FACTOR}
    defaultConsistencyLevel = ${?CASSANDRA_CONSISTENCY_LEVEL}
    replicationStrategy = "SimpleStrategy"
  }
}

staging = ${master}
production = ${master}
```

## Usage

The sbt pillar plugin provides the following tasks:

<dl>
<dt>createKeyspace</dt><dd>Creates the keyspace (and creates pillar's <code>applied_migrations</code> table)</dd>
<dt>dropKeyspace</dt><dd>Drops the keyspace</dd>
<dt>migrate</dt><dd>Runs pillar migrations (assumes <code>createKeyspace</code> was run before)</dd>
<dt>cleanMigrate</dt><dd>Recreates the keyspace (drops if exists && creates) and runs pillar migrations (useful for continuous integration scenarios)</dd>
</dl>

## Todo

Currently only the replication strategy 'SimpleStrategy' works. This will be resolved in next version.

## License

The license is Apache 2.0, see LICENSE.txt.
