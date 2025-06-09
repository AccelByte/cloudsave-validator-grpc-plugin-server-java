# cloudsave-validator-grpc-plugin-server-java

```mermaid
flowchart LR
   subgraph AccelByte Gaming Services
   CL[gRPC Client]
   end
   subgraph Extend Override App
   SV["gRPC Server"]
   end
   CL --- SV
```

`AccelByte Gaming Services` (AGS) features can be customized using 
`Extend Override` apps. An `Extend Override` app is basically a `gRPC server` which 
contains one or more custom functions which can be called by AGS instead of the 
default functions.

## Overview

This repository provides a project template to create an `Extend Override` 
app for `cloudsave validator` written in `Java`. It includes an example of how the
custom functions can be implemented. It also includes the essential 
`gRPC server` authentication and authorization to ensure security. Additionally, 
it comes with built-in instrumentation for observability, ensuring that metrics, 
traces, and logs are available upon deployment.

You can clone this repository to begin developing your own `Extend Override` 
app for `cloudsave validator`. Simply modify this project by implementing
your own logic for the custom functions.


## Prerequisites

1. Windows 11 WSL2 or Linux Ubuntu 22.04 or macOS 14+ with the following tools installed:

   a. Bash

      - On Windows WSL2 or Linux Ubuntu:

         ```
         bash --version

         GNU bash, version 5.1.16(1)-release (x86_64-pc-linux-gnu)
         ...
         ```

      - On macOS:

         ```
         bash --version

         GNU bash, version 3.2.57(1)-release (arm64-apple-darwin23)
         ...
         ```

   b. Make

      - On Windows WSL2 or Linux Ubuntu:

         To install from the Ubuntu repository, run `sudo apt update && sudo apt install make`.

         ```
         make --version

         GNU Make 4.3
         ...
         ```

      - On macOS:

         ```
         make --version

         GNU Make 3.81
         ...
         ```

   c. Docker (Docker Desktop 4.30+/Docker Engine v23.0+)
   
      - On Linux Ubuntu:

         1. To install from the Ubuntu repository, run `sudo apt update && sudo apt install docker.io docker-buildx docker-compose-v2`.
         2. Add your user to the `docker` group: `sudo usermod -aG docker $USER`.
         3. Log out and log back in to allow the changes to take effect.

      - On Windows or macOS:

         Follow Docker's documentation on installing the Docker Desktop on [Windows](https://docs.docker.com/desktop/install/windows-install/) or [macOS](https://docs.docker.com/desktop/install/mac-install/).

         ```
         docker version

         ...
         Server: Docker Desktop
            Engine:
            Version:          24.0.5
         ...
         ```

   d. JDK 17

      - On Linux Ubuntu:

         To install from the Ubuntu repository, run: `sudo apt update && sudo apt install openjdk-17-jdk`.

      - On Windows or macOS:

         Follow Microsoft's documentation for [installing the Microsoft Build for OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/install).

         ```
         java --version

         openjdk 17.0.10 2024-01-16
         ...
         ```

   e. Curl

      - On Windows WSL2 or Linux Ubuntu:

         To install from the Ubuntu repository, run `sudo apt update && sudo apt install curl`.

         ```
         curl --version

         curl 7.81.0 (x86_64-pc-linux-gnu)
         ...
         ```

      - On macOS:

         ```
         curl --version

         curl 8.4.0 (x86_64-apple-darwin23.0) ...
         ...
         ```

   f. Jq

      - On Windows WSL2 or Linux Ubuntu:

         To install from the Ubuntu repository, run `sudo apt update && sudo apt install jq`.

         ```
         jq --version

         jq-1.6
         ...
         ```

      - On macOS:

         To install using Homebrew, run `brew install jq`.

         ```
         jq --version

         jq-1.7.1
         ```

   g. [Postman](https://www.postman.com/)

      - Use the available binary from [Postman](https://www.postman.com/downloads/).

   h. [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli)

      - Use the available binary from [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli/releases).

   i. Local tunnel service that has TCP forwarding capability, such as:

      - [Ngrok](https://ngrok.com/)
         
         Need registration for free tier. Please refer to [ngrok documentation](https://ngrok.com/docs/getting-started/) for a quick start.

      - [Pinggy](https://pinggy.io/)

         Free to try without registration. Please refer to [pinggy documentation](https://pinggy.io/docs/) for a quick start.

   > :exclamation: In macOS, you may use [Homebrew](https://brew.sh/) to easily install some of the tools above.

2. Access to AGS environment.

   a. Base URL
   
      - For `Shared Cloud` tier e.g.  https://spaceshooter.prod.gamingservices.accelbyte.io
      - For `Private Cloud` tier e.g.  https://dev.accelbyte.io
      
   b. [Create a Game Namespace](https://docs.accelbyte.io/gaming-services/services/access/reference/namespaces/manage-your-namespaces/) if you don't have one yet. Keep the `Namespace ID`.

   c. [Create an OAuth Client](https://docs.accelbyte.io/gaming-services/services/access/authorization/manage-access-control-for-applications/#create-an-iam-client) with confidential client type. Keep the `Client ID` and `Client Secret`.

## Setup

To be able to run this app, you will need to follow these setup steps.

1. Create a docker compose `.env` file by copying the content of [.env.template](.env.template) file.

   > :warning: **The host OS environment variables have higher precedence compared to `.env` file variables**: If the variables in `.env` file do not seem to take effect properly, check if there are host OS environment variables with the same name. 
   See documentation about [docker compose environment variables precedence](https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/) for more details.

2. Fill in the required environment variables in `.env` file as shown below.

   ```
   AB_BASE_URL=https://test.accelbyte.io     # Base URL of AccelByte Gaming Services environment
   AB_CLIENT_ID='xxxxxxxxxx'                 # Client ID from the Prerequisites section
   AB_CLIENT_SECRET='xxxxxxxxxx'             # Client Secret from the Prerequisites section
   AB_NAMESPACE='xxxxxxxxxx'                 # Namespace ID from the Prerequisites section
   PLUGIN_GRPC_SERVER_AUTH_ENABLED=true     # Enable or disable access token validation
   ```

   > :exclamation: **In this app, PLUGIN_GRPC_SERVER_AUTH_ENABLED is `true` by default**: If it is set to `false`, the `gRPC server` can be invoked without an AGS access 
   token. This option is provided for development purpose only. It is 
   recommended to enable `gRPC server` access token validation in production 
   environment.

## Building

To build this app, use the following command.

```shell
make build
```

## Running

To (build and) run this app in a container, use the following command.

```shell
docker compose up --build
```

## Testing

### Test in Local Development Environment

> :warning: **To perform the following, make sure PLUGIN_GRPC_SERVER_AUTH_ENABLED is set to `false`**: Otherwise,
the gRPC request will be rejected by the `gRPC server`.

This app can be tested locally using [postman](https://www.postman.com/).

1. Run this app by using the command below.

   ```shell
   docker compose up --build
   ```

2. Open `postman`, create a new `gRPC request`, and enter `localhost:6565` as server URL.

   > :warning: **If you are running [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this project as mentioned in [Test Observability](#test-observability)**: Use `localhost:10000` instead of `localhost:6565`. This way, the `gRPC server` will be called via `Envoy` service within `grpc-plugin-dependencies` stack instead of directly.

3. Continue by selecting `CloudsaveValidatorService/BeforeWritePlayerRecord` method and invoke it with the sample message below.

   a. With a VALID `payload`

      ```json
      {
          "createdAt": {
              "nanos": 10,
              "seconds": "1693468029"
          },
          "isPublic": true,
          "key": "favourite_weapon",
          "namespace": "mynamespace",
          "payload": "eyJ1c2VySWQiOiAiMWUwNzZiY2VlNmQxNGM4NDlmZmIxMjFjMGUwMTM1YmUiLCAiZmF2b3VyaXRlV2VhcG9uVHlwZSI6ICJTV09SRCIsICJmYXZvdXJpdGVXZWFwb24iOiAiZXhjYWxpYnVyIn0=",  // {"userId": "1e076bcee6d14c849ffb121c0e0135be", "favouriteWeaponType": "SWORD", "favouriteWeapon": "excalibur"} encoded in base64
          "setBy": "SERVER",
          "updatedAt": {
              "nanos": 10,
              "seconds": "1693468275"
          },
          "userId": "1e076bcee6d14c849ffb121c0e0135be"
      }
      ```

      The response will contain `isSuccess: true`

      ```json
      {
          "isSuccess": true,
          "key": "favourite_weapon",
          "userId": "1e076bcee6d14c849ffb121c0e0135be"
      }
      ```

   b. With an INVALID `payload`
   
      ```json
      {
          "createdAt": {
              "nanos": 10,
              "seconds": "1693468029"
          },
          "isPublic": true,
          "key": "favourite_weapon",
          "namespace": "mynamespace",
          "payload": "eyJmb28iOiJiYXIifQ==",  // {"foo":"bar"} encoded in base64
          "setBy": "SERVER",
          "updatedAt": {
              "nanos": 10,
              "seconds": "1693468275"
          },
          "userId": "1e076bcee6d14c849ffb121c0e0135be"
      }
      ```

      The response will contain `isSuccess: false`

      ```json
      {
          "isSuccess": false,
          "key": "favourite_weapon",
          "userId": "1e076bcee6d14c849ffb121c0e0135be",
          "error": {
              "errorCode": 1,
              "errorMessage": "favourite weapon cannot be empty;favourite weapon type cannot be empty;user ID cannot be empty"
          }
      }
      ```

### Test with AccelByte Gaming Services

To test the app, which runs locally with AGS, the `gRPC server` needs to be connected to the internet. To do this without requiring public IP, you can use local tunnel service.

1. Run this app by using command below.

   ```shell
   docker compose up --build
   ```

2. Expose `gRPC server` TCP port 6565 in local development environment to the internet. Simplest way to do this is by using local tunnel service provider.
   - Sign in to [ngrok](https://ngrok.com/) and get your `authtoken` from the ngrok dashboard and set it up in your local environment.
      And, to expose `gRPC server` use following command:
      ```bash
      ngrok tcp 6565
      ```

   - **Or** alternatively, you can use [pinggy](https://pinggy.io/) and use only `ssh` command line to setup simple tunnel.
      Then to expose `gRPC server` use following command:
      ```bash
      ssh -p 443 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -R0:127.0.0.1:6565 tcp@a.pinggy.io
      ```

   Please take note of the tunnel forwarding URL, e.g., `http://0.tcp.ap.ngrok.io:xxxxx` or `tcp://xxxxx-xxx-xxx-xxx-xxx.a.free.pinggy.link:xxxxx`.

   > :exclamation: You may also use other local tunnel service and different method to expose the gRPC server port (TCP) to the internet.

   > :warning: **If you are running [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this app as mentioned in [Test Observability](#test-observability)**: Run the above 
   command in `grpc-plugin-dependencies` directory instead of this app directory and change tunnel local port from 6565 to 10000.
   This way, the `gRPC server` will be called via `Envoy` service within `grpc-plugin-dependencies` stack instead of directly.

3. [Create an OAuth Client](https://docs.accelbyte.io/gaming-services/services/access/authorization/manage-access-control-for-applications/#create-an-iam-client) with `confidential` client type with the following permissions. Keep the `Client ID` and `Client Secret`.
   
   - For AGS Private Cloud customers:
      - `ADMIN:NAMESPACE:{namespace}:CLOUDSAVE:PLUGINS [CREATE,READ,UPDATE,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:USER:*:CLOUDSAVE:RECORD [CREATE,READ,UPDATE,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:CLOUDSAVE:RECORD [CREATE,READ,UPDATE,DELETE]`
      - `NAMESPACE:{namespace}:CLOUDSAVE:RECORD [CREATE,READ,UPDATE,DELETE]`
      - `ADMIN:NAMESPACE:{namespace}:INFORMATION:USER:* [DELETE]`
   - For AGS Shared Cloud customers:
      - Cloud Save -> Custom Configuration (Read, Create, Update, Delete)
      - Cloud Save -> Game Records (Read, Create, Update, Delete)
      - Cloud Save -> Player Records (Read, Create, Update, Delete)
      - IAM -> Users (Delete)

   > :warning: **Oauth Client created in this step is different from the one from Prerequisites section:** It is required by the Postman collection in the next step to register the `gRPC Server` URL and also to create and delete test users.

4. Import the [Postman collection](demo/cloudsave-validator-demo.postman_collection.json) in order to simulate the extend app flow. Pay attention to this app console log when extend app flow is running. At least one of the `gRPC Server` methods should get called when you run all the requests in the collection.

   > :warning: Please don't forget to set the required environment variables in the Postman Collection Overview (and/or the Global Environment) including the `Client ID` and `Client Secret` created in the previous step.
   > You'll also need to set the environment variable `GRPC_SERVER_URL` (ex: `9.tcp.ap.ngrok.io:99999` or `xxxxx-xxx-xxx-xxx-xxx.a.free.pinggy.link:xxxxx`) with tunnnel forwarding URL to expose your locally hosted Extend App; or `EXTEND_APP_NAME` if you deployed your Extend App to AccelByte Gaming Services.

### Test Observability

To be able to see the how the observability works in this app locally, there are few things that need be setup before performing tests.

1. Uncomment loki logging driver in [docker-compose.yaml](docker-compose.yaml)

   ```
    # logging:
    #   driver: loki
    #   options:
    #     loki-url: http://host.docker.internal:3100/loki/api/v1/push
    #     mode: non-blocking
    #     max-buffer-size: 4m
    #     loki-retries: "3"
   ```

   > :warning: **Make sure to install docker loki plugin beforehand**: Otherwise,
   this project will not be able to run. This is required so that container logs
   can flow to the `loki` service within `grpc-plugin-dependencies` stack. 
   Use this command to install docker loki plugin: `docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions`.

2. Clone and run [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) stack alongside this project. After this, Grafana 
will be accessible at http://localhost:3000.

   ```
   git clone https://github.com/AccelByte/grpc-plugin-dependencies.git
   cd grpc-plugin-dependencies
   docker-compose up
   ```

   > :exclamation: More information about [grpc-plugin-dependencies](https://github.com/AccelByte/grpc-plugin-dependencies) is available [here](https://github.com/AccelByte/grpc-plugin-dependencies/blob/main/README.md).

3. Perform testing. For example, by following [Test in Local Development Environment](#test-in-local-development-environment) or [Test with AccelByte Gaming Services](#test-with-accelbyte-gaming-services).

## Deploying

After completing testing, the next step is to deploy your app to `AccelByte Gaming Services`.

1. **Create an Extend Override app**

   If you do not already have one, create a new [Extend Override App](https://docs.accelbyte.io/gaming-services/services/extend/override/cloud-save-validator/get-started-cloudsave-validator/#create-the-extend-app).

   On the **App Detail** page, take note of the following values.
   - `Namespace`
   - `App Name`

2. **Build and Push the Container Image**

   Use [extend-helper-cli](https://github.com/AccelByte/extend-helper-cli) to build and upload the container image.

   ```
   extend-helper-cli image-upload --login --namespace <namespace> --app <app-name> --image-tag v0.0.1
   ```

   > :warning: Run this command from your project directory. If you are in a different directory, add the `--work-dir <project-dir>` option to specify the correct path.

3. **Deploy the Image**
   
   On the **App Detail** page:
   - Click **Image Version History**
   - Select the image you just pushed
   - Click **Deploy Image**

## Next Step

Proceed by modifying this `Extend Override` app template to implement your own custom logic. For more details, see [here](https://docs.accelbyte.io/gaming-services/services/extend/override/cloud-save-validator/customize-cloudsave-validator/).
