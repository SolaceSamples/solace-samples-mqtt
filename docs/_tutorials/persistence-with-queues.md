---
layout: tutorials
title: Persistence with Queues
summary: Learn how to set up persistence for guaranteed delivery.
icon: persistence-tutorial.png
---

This tutorial builds on the basic concepts introduced in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe), and will show you how to send
and receive QoS 1 messages using a Solace router.

![]({{ site.baseurl }}/images/persistence-tutorial.png)

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts](http://dev.solacesystems.com/docs/core-concepts/){:target="_top"}.
*   You have access to a running Solace message router with the following configuration:
    *   Enabled message VPN
    *   Enabled client username
    *   Client-profile enabled with guaranteed messaging permissions.
    *   Enabled MQTT service on port 1883

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here](http://dev.solacesystems.com/docs/get-started/setting-up-solace-vmr_vmware/){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging and the MQTT service enabled on port 1883. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

You can learn more details on enabling MQTT service on a Solace message router by referring to the [Solace Messaging Platform Feature Guide - Using MQTT](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Messaging_Platform_Feature_Guide/Using_MQTT.html).

## Goals

The goal of this tutorial is to understand the following:

1.  How to send a QoS 1 message to a Solace message router.
2.  How to receive a QoS 1 message from a Soalce message rotuer.

## MQ Telemetry Transport (MQTT) Introduction

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to informatoin provided on the Solace developer portal, you may also look at some external sources for more details about MQTT. The following are good places to start

1. [http://mqtt.org/](http://mqtt.org/)
2. [https://www.eclipse.org/paho/](https://www.eclipse.org/paho/)

## Solace message router properties

As with other tutorials, this tutorial will connect to the default message VPN of a Solace VMR which has authentication disabled. So the only required information to proceed is the Solace VMR host string which this tutorial accepts as an argument.

## Obtaining an MQTT Client Library

Although, you can use any MQTT Client library of your choice to connect to Solace, this tutorial uses the Paho Java Client library. It will use Apache Maven to download and manage the MQTT dependencies.

The two sections below can be added to your pom.xml to configure it use the Paho Java library from the Eclipse Nexus repository.

~~~java
<project ...>
  <repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>https://repo.eclipse.org/content/repositories/paho-releases/</url>
    </repository>
  </repositories>
  ...
  <dependencies>
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>1.0.2</version>
    </dependency>
  </dependencies>
</project>
~~~

## Connecting a session to the message router

The simplest way to connect to a Solace message router in MQTT is to use an 'MqttClient', as done with other tutorials. So connect the 'MqttClient' as outlined in the [publish/subscribe tutorial](http://dev.solacesystems.com/get-started/mqtt-tutorials/publish-subscribe_mqtt/).

NOTE: If you use the default 'MqttConnectOptions', or set 'MqttConnectOptions.cleanSession' to 'true', as done in the publish/subscribe tutorial, then a Non-Durable (a.k.a. Temporary Endpoint) queue will automatically be created on the Solace message router when the client adds a QoS 1 subscription. Queues are used to store QoS 1 messages providing persistence for the 'MqttClient'. A Non-Durable queue is removed when the 'MqttClient' disconnects, which mean the Solace message router will not retain any messages for the client after it disconnects. Setting the 'MqttConnectOptions.cleanSession' to 'false' will create a Durable queue which will retain messages even after the client disconnects. You can learn more about Solace queue durability from the Endpoint Durability section of [Solace Messaging Platform Feature Guide – Working with Guaranteed Messages](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Messaging_Platform_Feature_Guide/Working_with_Guaranteed_Messages.html#).

For the purpose of this tutorial and to clean up resources and state 'MqttConnectOptions.cleanSession' is set to 'true'.


## Receiving a QoS 1 message

First connect and subscribe to receive the messages sent to a QoS 1 subscription.

<div style="float: right">
  <img src="{{ site.baseurl }}/images/receiving-message-from-queue-300x160.png"/>
</div>

This tutorial uses Quality of Service (QoS) level of 1 (equivalent to Solace “Guaranteed” or “Persistent” messages), which are at least once delivery messages. So first, let’s express interest in the messages by subscribing to a topic filter.

A topic filter in MQTT differs from a Solace SMF topic subscription. Users can learn more about the differences between the two in the Topic Names and Filters section of [MQTT Specification Conformance – Operational Behavior](https://sftp.solacesystems.com/Portal_Docs/#page/MQTT_Specification_Conformance/4_Operational_behavior.html).

As with other tutorials, this tutorial receives messages asynchronously through callbacks. So define a callback using the 'MqttCallback' interface as outlined in the [publish/subscribe tutorial](http://dev.solacesystems.com/get-started/mqtt-tutorials/publish-subscribe_mqtt/).

Then you must subscribe to a topic filter with a QoS level of 1 in order to express interest in receiving QoS 1 messages. This tutorial uses the topic '“Q/tutorial”'.

~~~java
mqttClient.subscribe("Q/tutorial", 1);
~~~

The above demonstrates the simplest way to add an OoS 1 subscription with an 'MqttClient'. However, the client is not informed of which QoS is actually granted. This tutorial will confirm if the broker has actually granted the client with OoS 1 subscription. In order do so, we can modify our tutorial to use an 'MqttAsyncClient' instead of an 'MqttClient'. The 'MqttAsyncClient' provides the granted QoS in the response from the subscribe method. You create a client as follows:

~~~java
MqttAsyncClient mqttClient = new MqttAsyncClient("tcp://" + args[0], "HelloWorldQoS1Subscriber");
~~~

We use the 'MqttAsyncClient.subscribe' method, which returns an 'IMqttToken', to track and wait for the subscribe call to complete. Then it is possible to confirm if the client was been granted the OoS 1 level for the topic subscribed.

~~~java
IMqttToken subToken = mqttClient.subscribe("Q/tutorial", 1);
subToken.waitForCompletion(10000);
if (!subToken.isComplete() || subToken.getException() != null) {
    System.out.println("Error subscribing: " + subToken.getException());
    System.exit(-1);
}
if (subToken.getGrantedQos()[0] != 1) {
    System.out.println("Expected OoS level 1 but got OoS level: " + subToken.getGrantedQos()[0]);
    System.exit(-1);
}
~~~

## Sending a QoS 1 message

Now it is time to send a QoS 1 message to the subscriber.

<div style="float: right">
  <img src="{{ site.baseurl }}/images/sending-message-to-queue-300x160.png"/>
</div>

You must first connect an 'MqttClient' as outlined above in the “Connecting a session to the message router” section. To send a message, you must create a message using the 'MqttMessage' class and set the QoS level. This tutorial will send a message to topic '“Q/tutorial”' with contents “Hello world from MQTT!” and a QoS level of 1, which are at least once delivery messages or Persistent messages in Solace. With a QoS level to 1 set on the message the client will receive acknowledgments from the Solace message router when it has successfully stored the message.

We then use the MQTT client created earlier to publish the message

~~~java
MqttMessage message = new MqttMessage("Hello world from MQTT!".getBytes());
message.setQos(1);
mqttClient.publish("Q/tutorial", message);
~~~

At this point the producer has sent a message to the Solace message router which gets in the Solace message router spool and your waiting consumer will have received the message and printed its contents to the screen.

## Summarizing

Combining the example source code show above results in the following source code archive files:

* [QoS1Producer.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/QoS1Producer.zip)
* [QoS1Consumer.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/QoS1Consumer.zip)

## Building

Building these examples is simple. The following provides an example using Maven to compile and execute the sample. These instructions assume you have Apache Maven installed in your environment. There are many suitable ways to build and execute these samples in Java. Adapt these instructions to suit your needs depending on your environment.

Extract both the archive files and run the below command in each directory to compile the samples:

~~~shell
cd QoS1Consumer
mvn clean compile
cd ..
cd QoS1Producer
mvn clean compile
~~~

## Sample Output

If you start the 'QoS1Consumer' with a single argument for the Solace message router host address it will connect and wait for a message. Replace HOST with the host address of your Solace VMR.

~~~shell
$ mvn exec:java -Dexec.args="HOST"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building QoS1Consumer 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ QoS1Consumer ---
QoS1Consumer initializing...
Connecting to Solace broker: tcp://HOST
Connected
Subscribing client to topic: Q/tutorial
Subscribed with OoS level 1 and waiting to receive msgs

Received a Message!
        Time:     2015-10-26 13:50:56.091
        Topic:    Q/tutorial
        Message:  Hello world from MQTT!
        QoS:      1

Exiting
~~~

Then you can send a message using the 'QoS1Producer' again using a single argument to specify the Solace message router HOST address. If successful, the output for the producer will look like the following:

~~~shell
$ mvn exec:java -Dexec.args="HOST"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building QoS1Producer 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ QoS1Producer ---
QoS1Producer initializing...
Connecting to Solace broker: tcp://HOST
Connected
Publishing message: Hello world from MQTT!
Message published. Exiting
~~~

With the message delivered the subscriber output will look like the following:

~~~shell
Received a Message!
    Time:     2015-10-19 11:10:49.929
    Topic:    Q/tutorial
    Message:  Hello world from MQTT!
    QoS:      1

Exiting
~~~

The received message is printed to the screen. The message contents were “Hello world from MQTT!” as expected with a QoS level of 1 and the output contains extra information about the Solace message that was received.

If you have any issues sending and receiving a message, check the [Solace community](http://dev.solacesystems.com/community) for answers to common issues seen.

You have now successfully sent and received QoS 1 MQTT messages which are equivalent to Solace guaranteed messages.

## Up Next:

* [Learn how to enable effective message acknowlegdements](http://dev.solacesystems.com/get-started/mqtt-tutorials/confirmed-delivery_mqtt/).

