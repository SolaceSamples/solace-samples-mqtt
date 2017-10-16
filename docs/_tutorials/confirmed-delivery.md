---
layout: tutorials
title: Confirmed Delivery
summary: Learn how to confirm that your messages are received by a Solace message router.
icon: I_dev_confirm.svg
---

This tutorial builds on the basic concepts introduced in [Persistence with MQTT]({{ site.baseurl }}/persistence-with-queues) tutorial and will show you how to properly process publisher acknowledgements. When you receive an acknowledgement for a QoS level 1 message, you have confirmed your message have been properly accepted by the Solace message router and therefore can be guaranteed of no message loss.

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts]({{ site.docs-core-concepts }}){:target="_top"}.
*   You have access to a running Solace message router with the following configuration:
    *   Enabled message VPN configured for guaranteed messaging support.
    *   Enabled client username
    *   Client-profile enabled with guaranteed messaging permissions.
    *   Enabled MQTT service on port 1883

One simple way to get access to a Solace message router is to start a Solace VMR load as [outlined here]({{ site.docs-vmr-setup }}){:target="_top"}. By default the Solace VMR will run with the “default” message VPN configured and ready for messaging and the MQTT service enabled on port 1883\. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

Users can learn more details on enabling MQTT service on a Solace message router by referring to the [Solace Docs – Using MQTT]({{ site.docs-using-mqtt }}){:target="_top"}.

## Goals

The goal of this tutorial is to understand the following:

1.  How to properly handle QoS 1 message acknowledgments on message send

## Obtaining an MQTT Client Library

Although, you can use any MQTT Client library of your choice to connect to Solace, this tutorial uses the Paho Java Client library. Here are a few easy ways to get the Paho API. The instructions in the Building section assume you're using Gradle and pulling the jars from maven central. If your environment differs then adjust the build instructions appropriately.

### Get the API: Using Gradle

```
    compile("org.eclipse.paho:org.eclipse.paho.client.mqttv3:{{ site.paho_version }}")
```

### Get the API: Using Maven

```
<dependency>
  <groupId>org.eclipse.paho</groupId>
  <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
  <version>{{ site.paho_version }}</version>
</dependency>
```

## Connecting a session to the message router

This tutorial builds on the `QoS1Producer` introduced in Persistence with MQTT. So connect the `MqttClient` as outlined in the [Persistence with MQTT]({{ site.baseurl }}/persistence-with-queues) tutorial.

## Tracking the delivery of QoS 1 messages

Similar to the `QoS1Producer` we will publish a QoS 1 message and then wait for an acknowledgement from the broker confirming the message was received and stored.

In MQTT there are two approaches to track the delivery of messages:

1.  Setting an `MqttCallback` on the client. Once a message has been delivered and stored by the Solace message router, the `deliveryComplete(IMqttDeliveryToken)` method will be called with delivery token being passed as a parameter.
2.  Using an asynchronous MQTT client and the `MqttAsyncClient.publish` method, which returns a `IMqttToken` when the publish call returns. The producer can then use the `IMqttToken.waitForCompletion` method to block until the delivery has been completed and the broker has acknowledge the message.

For the purpose of this tutorial we choose the first approach and set an `MqttCallback` on the client. The `MqttCallback.deliveryComplete` method is implemented here to check if the QoS 1 message has been received and stored by the Solace message router.

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

*   [ConfirmedDeliveryProducer.java]({{ site.repository }}/blob/master/src/main/java/com/solace/samples/ConfirmedDeliveryProducer.java){:target="_blank"}

### Getting the Source

Clone the GitHub repository containing the Solace samples.

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
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
$ ./build/staged/bin/confirmedDeliveryProducer <HOST>
ConfirmedDeliveryProducer initializing...
Connecting to Solace broker: tcp://HOST
Connected
Publishing message: Hello world from MQTT!

Message was successfully delivered to Solace

Exiting
```

When the text “Message was successfully delivered to Solace” is printed on screen, you have confirmed that the published QoS 1 message has been delivered successfully to the broker.

If you have any issues sending and receiving a message, check the [Solace community]({{ site.links-community }}){:target="_top"} for answers to common issues.
