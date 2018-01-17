---
layout: tutorials
title: Confirmed Delivery
summary: Learn how to confirm that your messages are received by Solace Messaging.
icon: I_dev_confirm.svg
links:
    - label: ConfirmedDeliveryProducer.java
      link: /blob/master/src/main/java/com/solace/samples/ConfirmedDeliveryProducer.java
---

This tutorial builds on the basic concepts introduced in [Persistence with MQTT]({{ site.baseurl }}/persistence-with-queues) tutorial and will show you how to properly process publisher acknowledgements. When you receive an acknowledgement for a QoS level 1 message, you have confirmed your message have been properly accepted by the Solace message router and therefore can be guaranteed of no message loss.

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
*   You have access to Solace messaging with the following configuration:
    *   Connectivity information for a Solace message-VPN configured for guaranteed messaging support
    *   Enabled client username and password
    *   Client-profile enabled with guaranteed messaging permissions.
    *   Enabled MQTT service

One simple way to get access to Solace messaging quickly is to create a messaging service in Solace Cloud [as outlined here]({{ site.links-solaceCloud-setup}}){:target="_top"}. You can find other ways to get access to Solace messaging below.

## Goals

The goal of this tutorial is to understand the following:

1.  How to properly handle QoS 1 message acknowledgments on message send

{% include solaceMessaging.md %}
{% include mqttApi.md %}

## Connecting a session to Solace messaging

This tutorial builds on the `QoS1Producer` introduced in Persistence with MQTT. So connect the `MqttClient` as outlined in the [Persistence with MQTT]({{ site.baseurl }}/persistence-with-queues) tutorial.

## Tracking the delivery of QoS 1 messages

Similar to the `QoS1Producer` we will publish a QoS 1 message and then wait for an acknowledgement from the broker confirming the message was received and stored.

In MQTT there are two approaches to track the delivery of messages:

1.  Setting an `MqttCallback` on the client. Once a message has been delivered and stored by Solace messaging, the `deliveryComplete(IMqttDeliveryToken)` method will be called with delivery token being passed as a parameter.
2.  Using an asynchronous MQTT client and the `MqttAsyncClient.publish` method, which returns a `IMqttToken` when the publish call returns. The producer can then use the `IMqttToken.waitForCompletion` method to block until the delivery has been completed and the broker has acknowledge the message.

For the purpose of this tutorial we choose the first approach and set an `MqttCallback` on the client. The `MqttCallback.deliveryComplete` method is implemented here to check if the QoS 1 message has been received and stored by Solace messaging.

```java
// Callback - Anonymous inner-class for receiving msg delivery complete token
mqttClient.setCallback(new MqttCallback() {
    public void messageArrived(String topic, MqttMessage message) throws Exception {
    }

    public void connectionLost(Throwable cause) {
        System.out.println("Connection to Solace broker lost!" + cause.getMessage());
        latch.countDown();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("\nMessage was successfully delivered to Solace\n");
        latch.countDown(); // unblock main thread
    }
});
```

The producer code uses a countdown latch to block the main thread until the `MqttCallback.deliveryComplete` method has been called.

```java
try {
    latch.await(); // block here until message delivery is completed, and latch will flip
} catch (InterruptedException e) {
    System.out.println("I was awoken while waiting");
}
```

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

Run the example from the command line as follows.

```
$ ./build/staged/bin/confirmedDeliveryProducer <host:port> <client-username> <client-password>
ConfirmedDeliveryProducer initializing...
Connecting to Solace messaging at <host:port>
Connected
Publishing message: Hello world from MQTT!

Message was successfully delivered to Solace

Exiting
```

When the text “Message was successfully delivered to Solace” is printed on screen, you have confirmed that the published QoS 1 message has been delivered successfully to the broker.

If you have any issues sending and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues.
