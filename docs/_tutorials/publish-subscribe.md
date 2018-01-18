---
layout: tutorials
title: Publish/Subscribe
summary: Learn how to set up pub/sub messaging on a Solace VMR.
icon: I_dev_P+S.svg
links:
    - label: TopicPublisher.java
      link: /blob/master/src/main/java/com/solace/samples/TopicPublisher.java
    - label: TopicSubscriber.java
      link: /blob/master/src/main/java/com/solace/samples/TopicSubscriber.java
---

This tutorial will introduce you to the fundamentals of connecting an MQTT client to Solace messaging by illustrating how to add a topic subscription and send a message matching this topic subscription. This forms the basis for any publish / subscribe message exchange.

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
*   You have access to Solace messaging with the following configuration details:
    *   Connectivity information for a Solace message-VPN
    *   Enabled client username and password
    *   Enabled MQTT services

One simple way to get access to Solace messaging quickly is to create a messaging service in Solace Cloud [as outlined here]({{ site.links-solaceCloud-setup}}){:target="_top"}. You can find other ways to get access to Solace messaging below.

## Goals

The goal of this tutorial is to demonstrate the MQTT messaging interaction using Solace. This tutorial will show you:

*   How to build and send a message on a topic
*   How to subscribe to a topic and receive a message

## MQ Telemetry Transport (MQTT) Introduction

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about MQTT. The following are good places to start

1.  [http://mqtt.org/](http://mqtt.org/){:target="_blank"}
2.  [https://www.eclipse.org/paho/](https://www.eclipse.org/paho/){:target="_blank"}

{% include solaceMessaging.md %}
{% include mqttApi.md %}

## Connecting to the Solace Messaging

In order to send or receive messages, a MQTT client must connect a session. The MQTT session is the basis for all client communication with Solace messaging.

In the Paho Java client library, MQTT sessions are created from the MqttClient class using a set of properties:

```java
MqttClient mqttClient = new MqttClient(host, "HelloWorldSub");
MqttConnectOptions connOpts = new MqttConnectOptions();
connOpts.setCleanSession(true);
connOpts.setUsername(username);
connOpts.setPassword(password.toCharArray())
mqttClient.connect(connOpts);
```

A MQTT client can maintain state information between sessions. The state information is used to ensure delivery and receipt of messages, and include subscriptions created by an MQTT client. This tutorial sets the "clean session" flag to true, via the `MqttConnectOptions` class, to clear the state information after each session disconnect.

At this point your client is connected to the Solace messaging. You can use SolAdmin to view the client connection and related details.

## Receive a message

This tutorial uses Quality of Service (QoS) level of 0 (equivalent to Solace "Direct" messages), which are at most once delivery messages. So first, let's express interest in the messages by subscribing to a topic filter. Then you can look at publishing a matching message and see it received.

With a session connected in the previous step, the next step is to use the MQTT client to subscribe to a topic filter to receive messages. A topic filter in MQTT differs from a Solace SMF topic subscription. Users can learn more about the differences between the two in the Topic Names and Topic Filters section of [MQTT Specification Conformance - Operational Behavior]({{ site.docs-ops-behavior }}){:target="_top"}.

Messages are received asynchronously through callbacks. These callbacks are defined in MQTT by the MqttCallback interface.

```java
mqttClient.setCallback(new MqttCallback() {

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String time = new Timestamp(System.currentTimeMillis()).toString();
        System.out.println("\nReceived a Message!" +
            "\n\tTime:    " + time +
            "\n\tTopic:   " + topic +
            "\n\tMessage: " + new String(message.getPayload()) +
            "\n\tQoS:     " + message.getQos() + "\n");
        latch.countDown(); // unblock main thread
    }

    public void connectionLost(Throwable cause) {
        System.out.println("Connection to Solace broker lost!" + cause.getMessage());
        latch.countDown();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }

});
```

The message consumer code uses a countdown latch in this hello world example to block the consumer thread until a single message has been received.

Then you must subscribe to a topic filter in order to express interest in receiving messages. This tutorial uses the topic "T/GettingStarted/pubsub" and QoS level of 0 for at most once delivery.

```java
mqttClient.subscribe("T/GettingStarted/pubsub", 0);
```

Then after the subscription is added, the consumer is started. At this point the consumer is ready to receive messages.

```java
try {
    latch.await(); // block here until message received, and latch will flip
} catch (InterruptedException e) {
    System.out.println("I was awoken while waiting");
}
```

## Sending a message

Now it is time to send a message to the waiting consumer.

![pub-sub-sending-message]({{ site.baseurl }}/images/pub-sub-sending-message-300x134.png){:target="_top"}

To send a message, you must create a message using the MqttMessage class and set the QoS level. This tutorial will send a message to topic "T/GettingStarted/pubsub" with contents "Hello world from MQTT!" and a QoS level of 0, which are at most once delivery messages. We then use the MQTT client created earlier to publish the message

```java
MqttMessage message = new MqttMessage("Hello world from MQTT!".getBytes());
message.setQos(0);
mqttClient.publish("T/GettingStarted/pubsub", message);
```

At this point the producer has sent a message to the Solace messaging and your waiting consumer will have received the message and printed its contents to the screen.

## Summarizing

The full source code for this example is available on [GitHub]({{ site.repository }}){:target="_blank"}. If you combine the example source code shown above results in the following source:

<ul>
{% for item in page.links %}
<li><a href="{{ site.repository }}{{ item.link }}" target="_blank">{{ item.label }}</a></li>
{% endfor %}
</ul>

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.repository | split: '/' | last }}
```

### Building

The project uses Gradle. To build, execute the following command.

```
./gradlew build
```

This builds all of the Java Samples with OS specific launch scripts. The files are staged in the `build/staged` directory.

### Running the Sample

If you start the `topicSubscriber` with arguments specifying your Solace messaging connection details, it will connect and wait for a message.

```
$ ./build/staged/bin/topicSubscriber <host:port> <client-username> <client-password>
TopicSubscriber initializing...
Connecting to Solace messaging at <host:port>
Connected
Subscribing client to topic: T/GettingStarted/pubsub
```

Then you can send a message using the `topicPublisher` with the same arguments. If successful, the output for the producer will look like the following:

```
$ ./build/staged/bin/topicPublisher <host:port> <client-username> <client-password>
opicPublisher initializing...
Connecting to Solace messaging at <host:port>
Connected
Publishing message: Hello world from MQTT!
Message published. Exiting
```

With the message delivered the subscriber output will look like the following:

```
Received a Message!
        Time:    2015-10-16 17:09:45.379
        Topic:   T/GettingStarted/pubsub
        Message: Hello world from MQTT!
QoS:     0

Exiting
```

The received message is printed to the screen. The message contents were "Hello world from MQTT!" as expected and the output contains extra information about the Solace message that was received.

If you have any issues sending and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} Q&A for answers to common issues seen.

You have now successfully connected a MQTT client, subscribed to a topic and exchanged messages using this topic.
