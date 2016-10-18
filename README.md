# Getting Started Examples
## MQ Telemetry Transport (MQTT)

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to information provided on the Solace [developer portal](http://dev.solacesystems.com/tech/mqtt/), you may also look at some external sources for more details about MQTT. The following are good places to start

- http://mqtt.org/
- https://www.eclipse.org/paho/

These tutorials will get you up to speed and sending messages with Solace technology as quickly as possible. There are two ways you can get a Solace Message Router:

- If your company has Solace message routers deployed, contact your middleware team to obtain the host name or IP address of a Solace message router to test against, a username and password to access it, and a VPN in which you can produce and consume messages.
- If you do not have access to a Solace message router, you will need to go through the “[Set up a VMR](http://dev.solacesystems.com/get-started/vmr-setup-tutorials/setting-up-solace-vmr/)” tutorial to download and install the software.

## Contents

This repository contains code and matching tutorial walk throughs for different basic scenarios. It is best to view the associated [tutorials home page](https://solacesamples.github.io/solace-samples-mqtt/).

## Prerequisites

There are no prerequisites. 

## Build the Samples

    ./gradlew build

## Running the Samples

To try individual samples, build the project from source and then run samples like the following:

    ./build/staged/bin/topicPublisher <HOST>

See the individual tutorials linked from the [tutorials home page](https://solacesamples.github.io/solace-samples-mqtt/) for full details which can walk you through the samples, what they do, and how to correctly run them to explore MQTT.

## Exploring the Samples

### Setting up your preferred IDE

Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow the steps below for your preferred IDE.

#### Using Eclipse

To generate Eclipse metadata (.classpath and .project files), do the following:

    ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 *File -> Import -> Existing projects into workspace*

Browse to the *'solace-samples-java'* root directory. All projects should import
free of errors.

#### Using IntelliJ IDEA

To generate IDEA metadata (.iml and .ipr files), do the following:

    ./gradlew idea

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/SolaceSamples/solace-samples-mqtt/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:

- The Solace Developer Portal website at: [http://dev.solacesystems.com](http://dev.solacesystems.com/)
- Get a better understanding of [Solace technology.](http://dev.solacesystems.com/tech/)
- Check out the [Solace blog](http://dev.solacesystems.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solacesystems.com/community/)
