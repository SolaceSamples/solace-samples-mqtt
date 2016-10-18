---
layout: tutorials
title: Publish/Subscribe
summary: Learn how to set up pub/sub messaging on a Solace VMR.
icon: publish-subscribe.png
---


This tutorial will introduce you to the fundamentals of connecting an MQTT client to a Solace Message Router by illustrating how to add a topic subscription and send a message matching this topic subscription. This forms the basis for any publish / subscribe message exchange illustrated here:

![]({{ site.baseurl }}/images/request-reply.png)

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts](http://docs.solacesystems.com/Features/Core-Concepts.htm){:target="_top"}.
*   You have access to a running Solace message router with the following configuration:
    *   Enabled message VPN
    *   Enabled client username
    *   Enabled MQTT services on port 1883

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here](http://docs.solacesystems.com/Solace-VMR-Set-Up/Starting-VMRs-for-the-First-Time/Setting-Up-an-Eval-VMR-in-AWS.htm). By default the Solace VMR will run with the “default” message VPN configured and ready for messaging and the MQTT service enabled on port 1883\. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

Users can learn more details on enabling MQTT service on a Solace message router by referring to the [Solace Messaging Platform Feature Guide – Using MQTT](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Messaging_Platform_Feature_Guide/Using_MQTT.html).

## Goals

The goal of this tutorial is to demonstrate the MQTT messaging interaction using Solace. This tutorial will show you:

*   How to build and send a message on a topic
*   How to subscribe to a topic and receive a message

## MQ Telemetry Transport (MQTT) Introduction

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about MQTT. The following are good places to start

1.  [http://mqtt.org/](http://mqtt.org/)
2.  [https://www.eclipse.org/paho/](https://www.eclipse.org/paho/)

## Solace message router properties

In order to send or receive messages to a Solace message router, you need to know a few details of how to connect to the Solace message router. Specifically you need to know the following:

<table>

<tbody>

<tr>

<td width="133">**Resource**</td>

<td width="138">**Value**</td>

<td width="379">**Description**</td>

</tr>

<tr>

<td width="133">Host</td>

<td width="138">String of the form tcp://DNS_NAME or tcp://IP:PORT</td>

<td width="379">This is the address clients use when connecting to the Solace message router to send and receive messages.

For a Solace VMR this there is only a single interface so the IP is the same as the management IP address.

For Solace message router appliances this is the host address of the message-backbone.

</td>

</tr>

<tr>

<td width="133">Message VPN</td>

<td width="138">String</td>

<td width="379">The Solace message router Message VPN that this client should connect to. The simplest option is to use the “default” message-vpn which is present on all Solace message routers and fully enabled for message traffic on Solace VMRs.</td>

</tr>

<tr>

<td width="133">Client Username</td>

<td width="138">String</td>

<td width="379">The client username. For the Solace VMR default message VPN, authentication is disabled by default, so this can be any value.</td>

</tr>

<tr>

<td width="133">Client Password</td>

<td width="138">String</td>

<td width="379">The optional client password. For the Solace VMR default message VPN, authentication is disabled by default, so this can be any value or omitted.</td>

</tr>

</tbody>

</table>

The MQTT Port number is for MQTT clients to use when connecting to the Message VPN. The port number configured on a Solace VMR for the “default” message-vpn is 1883.

To see the port number configured on your Solace message router and Message VPN for the MQTT service, run the following command in the Solace message router CLI.

<pre class="brush: java; title: ; notranslate" title="">solace-vmr> show service</pre>

See the [VMR getting started](http://docs.solacesystems.com/Solace-VMR-Set-Up/Starting-VMRs-for-the-First-Time/Setting-Up-an-Eval-VMR-in-AWS.htm) tutorial for default credentials and accounts. Then paste the above command into the CLI.

For the purposes of this tutorial, you will connect to the default message VPN of a Solace VMR so the only required information to proceed is the Solace VMR host string which this tutorial accepts as an argument.

## Obtaining an MQTT Client Library

Although you can use any MQTT Client library of your choice to connect to Solace, this tutorial will be using the [Paho Java Client library](https://www.eclipse.org/paho/clients/java/). This tutorial will use Apache Maven to download and manage the MQTT dependencies.

The two sections below can be added to your pom.xml to configure it use the Paho Java library from the Eclipse Nexus repository.

<pre class="brush: java; title: ; notranslate" title=""><project ...>
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
</pre>

## Connecting to the Solace message router

In order to send or receive messages, a MQTT client must connect a session. The MQTT session is the basis for all client communication with the Solace message router.

In the Paho Java client library, MQTT sessions are created from the MqttClient class using a set of properties:

<pre class="brush: java; title: ; notranslate" title="">MqttClient mqttClient = new MqttClient("tcp://" + args[0], "HelloWorldSub");
MqttConnectOptions connOpts = new MqttConnectOptions();
connOpts.setCleanSession(true);
mqttClient.connect(connOpts);
</pre>

A MQTT client can maintain state information between sessions. The state information is used to ensure delivery and receipt of messages, and include subscriptions created by an MQTT client. This tutorial sets the “clean session” flag to true, via the `MqttConnectOptions` class, to clear the state information after each session disconnect.

At this point your client is connected to the Solace message router. You can use SolAdmin to view the client connection and related details.

## Receive a message

This tutorial uses Quality of Service (QoS) level of 0 (equivalent to Solace “Direct” messages), which are at most once delivery messages. So first, let’s express interest in the messages by subscribing to a topic filter. Then you can look at publishing a matching message and see it received.

With a session connected in the previous step, the next step is to use the MQTT client to subscribe to a topic filter to receive messages. A topic filter in MQTT differs from a Solace SMF topic subscription. Users can learn more about the differences between the two in the Topic Names and Topic Filters section of [MQTT Specification Conformance – Operational Behavior](https://sftp.solacesystems.com/Portal_Docs/#page/MQTT_Specification_Conformance/4_Operational_behavior.html#).

Messages are received asynchronously through callbacks. These callbacks are defined in MQTT by the MqttCallback interface.

<pre class="brush: java; title: ; notranslate" title="">mqttClient.setCallback(new MqttCallback() {

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
</pre>

The message consumer code uses a countdown latch in this hello world example to block the consumer thread until a single message has been received.

Then you must subscribe to a topic filter in order to express interest in receiving messages. This tutorial uses the topic “T/GettingStarted/pubsub” and QoS level of 0 for at most once delivery.

<pre class="brush: java; title: ; notranslate" title="">mqttClient.subscribe("T/GettingStarted/pubsub", 0);</pre>

Then after the subscription is added, the consumer is started. At this point the consumer is ready to receive messages.

<pre class="brush: java; title: ; notranslate" title="">try {
    latch.await(); // block here until message received, and latch will flip
} catch (InterruptedException e) {
    System.out.println("I was awoken while waiting");
}
</pre>

## Sending a message

Now it is time to send a message to the waiting consumer.

![pub-sub-sending-message](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/2015/08/pub-sub-sending-message-300x134.png)

To send a message, you must create a message using the MqttMessage class and set the QoS level. This tutorial will send a message to topic “T/GettingStarted/pubsub” with contents “Hello world from MQTT!” and a QoS level of 0, which are at most once delivery messages. We then use the MQTT client created earlier to publish the message

<pre class="brush: java; title: ; notranslate" title="">MqttMessage message = new MqttMessage("Hello world from MQTT!".getBytes());
message.setQos(0);
mqttClient.publish("T/GettingStarted/pubsub", message);
</pre>

At this point the producer has sent a message to the Solace message router and your waiting consumer will have received the message and printed its contents to the screen.

## Summarizing

Combining the example source code shown above results in the following source code archive files:

*   [TopicPublisher.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/TopicPublisher.zip)
*   [TopicSubscriber.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/TopicSubscriber.zip)

### Building

Building these examples is simple. The following provides an example using Maven to compile and execute the sample. These instructions assume you have Apache Maven installed in your environment. There are many suitable ways to build and execute these samples in Java. Adapt these instructions to suit your needs depending on your environment.

Extract both the archive files and run the below command in each directory to compile the samples:

<pre class="brush: java; title: ; notranslate" title="">cd TopicSubscriber
mvn clean compile
cd ..
cd TopicPublisher
mvn clean compile
</pre>

### <a name="_Toc432765876"></a>Sample Output

If you start the TopicSubscriber with a single argument for the Solace message router host address and MQTT TCP port configured on the router (default is 1883) it will connect and wait for a message. Replace HOST with the host address of your Solace VMR.

<pre class="brush: java; title: ; notranslate" title="">$ mvn exec:java -Dexec.args="HOST:1883"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building TopicSubscriber 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ TopicSubscriber ---
TopicSubscriber initializing...
Connecting to Solace broker: tcp://HOST:1883
Connected
Subscribing client to topic: T/GettingStarted/pubsub
</pre>

Then you can send a message using the TopicPublisher again using a single argument to specify the Solace message router host address. If successful, the output for the producer will look like the following:

<pre class="brush: java; title: ; notranslate" title="">$ mvn exec:java -Dexec.args="HOST:1883"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building TopicPublisher 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ TopicPublisher ---
TopicPublisher initializing...
Connecting to Solace broker: tcp://HOST:1883
Connected
Publishing message: Hello world from MQTT!
Message published. Exiting
</pre>

With the message delivered the subscriber output will look like the following:

<pre class="brush: java; title: ; notranslate" title="">Received a Message!
        Time:    2015-10-16 17:09:45.379
        Topic:   T/GettingStarted/pubsub
        Message: Hello world from MQTT!
QoS:     0

Exiting
</pre>

The received message is printed to the screen. The message contents were “Hello world from MQTT!” as expected and the output contains extra information about the Solace message that was received.

If you have any issues sending and receiving a message, check the Solace community Q&A for answers to common issues seen.

You have now successfully connected a MQTT client, subscribed to a topic and exchanged messages using this topic.
