---
layout: tutorials
title: Confirmed Delivery
summary: Learn how to confirm that your messages are received by a Solace message router.
icon: confirmed-delivery.png
---

This tutorial builds on the basic concepts introduced in “[Persistence with MQTT](http://dev.solacesystems.com/get-started/mqtt-tutorials/persistence-with-queues_mqtt/)” tutorial and will show you how to properly process publisher acknowledgements. When you receive an acknowledgement for a QoS level 1 message, you have confirmed your message have been properly accepted by the Solace message router and therefore can be guaranteed of no message loss.

![confirmed-delivery]({{ site.baseurl }}/images/confirmed-delivery.png)

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts](http://docs.solacesystems.com/Features/Core-Concepts.htm).
*   You have access to a running Solace message router with the following configuration:
    *   Enabled message VPN configured for guaranteed messaging support.
    *   Enabled client username
    *   Client-profile enabled with guaranteed messaging permissions.
    *   Enabled MQTT service on port 1883

One simple way to get access to a Solace message router is to start a Solace VMR load as [outlined here](http://docs.solacesystems.com/Solace-VMR-Set-Up/Starting-VMRs-for-the-First-Time/Setting-Up-an-Eval-VMR-in-AWS.htm). By default the Solace VMR will run with the “default” message VPN configured and ready for messaging and the MQTT service enabled on port 1883\. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

Users can learn more details on enabling MQTT service on a Solace message router by referring to the [Solace Messaging Platform Feature Guide – Using MQTT](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Messaging_Platform_Feature_Guide/Using_MQTT.html).

## Goals

The goal of this tutorial is to understand the following:

1.  How to properly handle QoS 1 message acknowledgments on message send

## Obtaining an MQTT Client Library

Although, you can use any MQTT Client library of your choice to connect to Solace, this tutorial uses the Paho Java Client library. It will use Apache Maven to download and manage the MQTT dependencies.

The two sections below can be added to your pom.xml to configure it use the Paho Java library from the Eclipse Nexus repository.

<pre class="brush: java; title: ; notranslate" title=""><project ...>
  <repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>https://repo.eclipse.org/content/repositories/paho
releases/</url>
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
</pre>

## Connecting a session to the message router

This tutorial builds on the `QoS1Producer` introduced in Persistence with MQTT. So connect the `MqttClient` as outlined in the [Persistence with MQTT](http://dev.solacesystems.com/get-started/mqtt-tutorials/persistence-with-queues_mqtt/) tutorial.

## Tracking the delivery of QoS 1 messages

Similar to the `QoS1Producer` we will publish a QoS 1 message and then wait for an acknowledgement from the broker confirming the message was received and stored.

In MQTT there are two approaches to track the delivery of messages:

1.  Setting an `MqttCallback` on the client. Once a message has been delivered and stored by the Solace message router, the `deliveryComplete(IMqttDeliveryToken)` method will be called with delivery token being passed as a parameter.
2.  Using an asynchronous MQTT client and the `MqttAsyncClient.publish` method, which returns a `IMqttToken` when the publish call returns. The producer can then use the `IMqttToken.waitForCompletion` method to block until the delivery has been completed and the broker has acknowledge the message.

For the purpose of this tutorial we choose the first approach and set an `MqttCallback` on the client. The `MqttCallback.deliveryComplete` method is implemented here to check if the QoS 1 message has been received and stored by the Solace message router.

<pre class="brush: java; title: ; notranslate" title="">// Callback - Anonymous inner-class for receiving msg delivery complete token
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
</pre>

The producer code uses a countdown latch to block the main thread until the `MqttCallback.deliveryComplete` method has been called.

<pre class="brush: java; title: ; notranslate" title="">try {
    latch.await(); // block here until message delivery is completed, and latch will flip
} catch (InterruptedException e) {
    System.out.println("I was awoken while waiting");
}
</pre>

## Summarizing

Combining the example source code shown above results in the following source code archive files:

*   [ConfirmedDeliveryProducer.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/ConfirmedDeliveryProducer.zip)

### Building

Building the example is simple. The following provides an example using Maven to compile and execute the sample. These instructions assume you have Apache Maven installed in your environment. There are many suitable ways to build and execute these samples in Java. Adapt these instructions to suit your needs depending on your environment.

Extract the archive file and run the below command to compile the sample:

<pre class="brush: java; title: ; notranslate" title="">cd ConfirmedDeliveryProducer
mvn clean compile
</pre>

### Sample Output

If you start the ConfirmedDeliveryProducer with a single argument for the Solace message router host address it will connect and wait for a message. Replace HOST with the host address of your Solace VMR.

<pre class="brush: java; title: ; notranslate" title="">$ mvn exec:java -Dexec.args="HOST"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building ConfirmedDeliveryProducer 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ ConfirmedDeliveryProducer ---
ConfirmedDeliveryProducer initializing...
Connecting to Solace broker: tcp://HOST
Connected
Publishing message: Hello world from MQTT!

Message was successfully delivered to Solace

Exiting
</pre>

With the text “Message was successfully delivered to Solace” is printed on screen, you have confirmed that the published QoS 1 message has been delivered successfully to the broker.

If you have any issues sending and receiving a message, check the Solace community Q&A for answers to common issues seen.

You have now successfully sent QoS 1 MQTT message and confirmed it’s delivery to the Solace message router.
