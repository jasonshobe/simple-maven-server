# Simple Maven Server

This application is a very simple, lightweight Maven repository
server. It is suitable for smaller private repositories.

## Configuration

The server will run from the distribution without any additional
configuration. You can override the default settings by creating
a `maven.yaml` file in the application directory. This file may
set any of the properties shown here:

```yaml
maven:
  storage:
    s3:
      enabled: false
      bucket: my-maven-repo
    directory: repositories
    cache: cache
  port: 8080
  repositories:
    - releases
    - snapshots
  users:
    - name: admin
      password: admin
  proxies:
    - name: public
      repositories:
        - releases
        - snapshots
```

The configuration settings are loaded in the following order:

1. Java system properties
2. Environment variables
3. Configuration file
4. Defaults

So the settings in the configuration file can be overridden by
system properties set on the command line or with environment
variables.

### `maven.storage.s3.enabled`

Enables the use of an AWS S3 bucket for artifact storage. If
enabled, the bucket name must also be set. This property can also
be set using the `maven.storage.s3.enabled` system property or the
`MAVEN_STORAGE_S3_ENABLED` environment variable. The default value
of this property is `false`.

The AWS credentials are looked for in the following order:

1. Java system properties - `aws.accessKeyId` and
   `aws.secretAccessKey`
2. Environment variables - `AWS_ACCESS_KEY_ID` and
   `AWS_SECRET_ACCESS_KEY`
3. Credentials profile file at the default location
   (`~/.aws/credentials`)
4. Credentials delivered through the Amazon EC2 container service
   if the `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI` environment
   variable is set
5. Instance profile credentials delivered through the Amazon EC2
   metadata service

### `maven.storage.s3.bucket`

The name of the S3 bucket where the artifacts will be stored. This
property can also be set using the `maven.storage.s3.bucket` system
property or the `MAVEN_STORAGE_S3_BUCKET` environment variable. This
property is not set by default.

### `maven.storage.directory`

The path to the local directory where the artifacts will be stored
if AWS S3 is not being used. The directory can either be relative
to the application directory or an absolute path. This property can
also be set using the `maven.storage.directory` system property or
the `MAVEN_STORAGE_DIRECTORY` environment variable. The default
value of this property is `repositories`.

### `maven.storage.cache`

The path to the local directory where the repository index will be
cached. The directory can either be relative to the application
directory or an absolute path. This property can also be set using
the `maven.storage.cache` system property or the
`MAVEN_STORAGE_CACHE` environment variable. The default value of
this property is `cache`.

Note that the index cache is not persistent between server
restarts. It is built lazily as the server runs. When running in a
container, it is not necessary to mount this directory to a
persistent volume.

### `maven.port`

The port number on which the repository server will listen for HTTP
requests. This property can also be set using the `maven.port`
system property or the `MAVEN_PORT` environment variable. The
default value of this property is `8080`.

### `maven.repositories`

The names of the repositories provided by the server. This property
can also be set as a comma-separated list in the
`maven.repositories` system property or the `MAVEN_REPOSITORIES`
environment variable. The default value of this property is
`releases,snapshots`.

### `maven.users`

The credentials for the users that are allowed write access to the
repositories. This property can only be set in the configuration
file. The password may either be in clear text or hashed using
Bcrypt, in which case it should be prefixed with `bcrypt`. By
default, there is one user named `admin` with the password `admin`.

Additional users can be added to the configuration file with a
hashed password using the command:

```shell
simple-maven-server user [username]
```

### `maven.proxies`

Defines the proxy repositories provided by the server. A proxy
repository is a merged view of the proxied repositories. This
property can only be set in the configuration file. By default,
there is only one proxy repository named `public` that proxies
the `releases` and `snapshots` repositories.

## Running

The server can be run with the following command:

```shell
simple-maven-server start
```

Note that this application requires Java 11.

## Docker

A Docker image named
[`jasonshobe/simple-maven-server`](https://hub.docker.com/r/jasonshobe/simple-maven-server)
is available for this application in Docker Hub.

The only difference from the default configuration documented
earlier is that `maven.storage.directory` is configured to be
`/var/lib/maven/repositories` which can be mounted as a volume.

Other configuration can be set using environment variables or by
placing `maven.yaml` file in the `/app/` directory.

```shell
docker run -d --rm -p 8080:8080 jasonshobe/simple-maven-server
```

## License

Simple Maven Server is released under version 3 of the
[GNU Public License](https://www.gnu.org/licenses/gpl-3.0.txt).
