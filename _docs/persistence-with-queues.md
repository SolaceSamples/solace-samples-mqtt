---
layout: tutorials
title: Persistence with Queues
summary: Learn how to set up persistence for guaranteed delivery.
icon: I_dev_Persistent.svg
links:
    - label: QoS1Producer.java
      link: /blob/master/src/main/java/com/solace/samples/QoS1Producer.java
    - label: QoS1Consumer.java
      link: /blob/master/src/main/java/com/solace/samples/QoS1Consumer.java

---

This tutorial builds on the basic concepts introduced in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe), and will show you how to send
and receive QoS 1 messages using Solace messaging.

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
*   You have access to Solace messaging with the following configuration details:
    *   Connectivity information for a Solace message-VPN configured for guaranteed messaging support
    *   Enabled client username and password
    *   Client-profile enabled with guaranteed messaging permissions.
    *   Enabled MQTT service

One simple way to get access to Solace messaging quickly is to create a messaging service in Solace Cloud [as outlined here]({{ site.links-solaceCloud-setup}}){:target="_top"}. You can find other ways to get access to Solace messaging below.

## Goals

The goal of this tutorial is to understand the following:

1.  How to send a QoS 1 message to Solace messaging.
2.  How to receive a QoS 1 message from Solace messaging.

## MQ Telemetry Transport (MQTT) Introduction

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to informatoin provided on the Solace developer portal, you may also look at some external sources for more details about MQTT. The following are good places to start

1. [http://mqtt.org/](http://mqtt.org/){:target="_blank"}
2. [https://www.eclipse.org/paho/](https://www.eclipse.org/paho/){:target="_blank"}

{% include_relative assets/solaceMessaging.md %}
{% include_relative assets/mqttApi.md %}

## Connecting a session to Solace messaging

The simplest way to connect to a Solace messaging in MQTT is to use an 'MqttClient', as done with other tutorials. So connect the 'MqttClient' as outlined in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe).

NOTE: If you use the default 'MqttConnectOptions', or set 'MqttConnectOptions.cleanSession' to 'true', as done in the publish/subscribe tutorial, then a Non-Durable (a.k.a. Temporary Endpoint) queue will automatically be created on Solace messaging when the client adds a QoS 1 subscription. Queues are used to store QoS 1 messages providing persistence for the 'MqttClient'. A Non-Durable queue is removed when the 'MqttClient' disconnects, which mean Solace messaging will not retain any messages for the client after it disconnects. Setting the 'MqttConnectOptions.cleanSession' to 'false' will create a Durable queue which will retain messages even after the client disconnects. You can learn more about Solace queue durability from the Endpoint Durability section of [Solace Features – Working with Guaranteed Messages]({{ site.docs-gm-feature }}){:target="_top"}.

For the purpose of this tutorial and to clean up resources and state 'MqttConnectOptions.cleanSession' is set to 'true'.


## Receiving a QoS 1 message

First connect and subscribe to receive the messages sent to a QoS 1 subscription.

<div style="float: right">
  <img src="{{ site.baseurl }}/assets/images/receiving-message-from-queue-300x160.png" alt="Receiving MQTT QoS 1 Message" />
</div>

This tutorial uses Quality of Service (QoS) level of 1 (equivalent to Solace “Guaranteed” or “Persistent” messages), which are at least once delivery messages. So first, let’s express interest in the messages by subscribing to a topic filter.

A topic filter in MQTT differs from a Solace SMF topic subscription. Users can learn more about the differences between the two in the Topic Names and Filters section of [MQTT Specification Conformance – Operational Behavior]({{ site.docs-ops-behavior }}){:target="_top"}.

As with other tutorials, this tutorial receives messages asynchronously through callbacks. So define a callback using the 'MqttCallback' interface as outlined in the [publish/subscribe tutorial]({{ site.baseurl }}/publish-subscribe).

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
  <img src="{{ site.baseurl }}/assets/images/sending-message-to-queue-300x160.png" alt="Sending MQTT QoS 1 Message" />
</div>

You must first connect an 'MqttClient' as outlined above in the “Connecting a session to Solace messaging” section. To send a message, you must create a message using the 'MqttMessage' class and set the QoS level. This tutorial will send a message to topic '“Q/tutorial”' with contents “Hello world from MQTT!” and a QoS level of 1, which are at least once delivery messages or Persistent messages in Solace. With a QoS level to 1 set on the message the client will receive acknowledgments from Solace messaging when it has successfully stored the message.

We then use the MQTT client created earlier to publish the message

~~~java
MqttMessage message = new MqttMessage("Hello world from MQTT!".getBytes());
message.setQos(1);
mqttClient.publish("Q/tutorial", message);
~~~

At this point the producer has sent a message to Solace messaging which gets in the Solace messaging spool and your waiting consumer will have received the message and printed its contents to the screen.

## Summarizing

The full source code for this example is available on [GitHub]({{ site.repository }}){:target="_blank"}. Combining the example source code show above results in the following source files:

<ul>
{% for item in page.links %}
<li><a href="{{ site.repository }}{{ item.link }}" target="_blank">{{ item.label }}</a></li>
{% endfor %}
</ul>

## Getting the Source

Clone the GitHub repository containing the Solace samples.

~~~shell
git clone {{ site.repository }}
cd {{ site.repository | split: '/' | last }}
~~~

## Building

The project uses Gradle. To build, execute the following command.

~~~shell
./gradlew build
~~~

This builds all of the Java Samples with OS specific launch scripts. The files are staged in the `build/staged` directory.

## Sample Output

If you start the 'QoS1Consumer' with arguments specifying your Solace messaging connection details, it will connect and wait for a message.

~~~shell
$ ./build/staged/bin/QoS1Consumer <host:port> <client-username> <client-password>
QoS1Consumer initializing...
Connecting to Solace messaging at <host:port>
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

Then you can send a message using the 'QoS1Producer' with the same arguments. If successful, the output for the producer will look like the following:

~~~shell
$ ./build/staged/bin/QoS1Producer <host:port> <client-username> <client-password>
QoS1Producer initializing...
Connecting to Solace messaging at <host:port>
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

If you have any issues sending and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues seen.

You have now successfully sent and received QoS 1 MQTT messages which are equivalent to Solace guaranteed messages.
