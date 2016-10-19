---
layout: tutorials
title: Request/Reply
summary: This is a short description for the landing page.
icon: request-reply.png
---

This tutorial outlines both roles in the request-response message exchange pattern. It will show you how to act as the client by creating a request, sending it and waiting for the response. It will also show you how to act as the server by receiving incoming requests, creating a reply and sending it back to the client. It builds on the basic concepts introduced in [publish/subscribe tutorial](http://dev.solacesystems.com/get-started/mqtt-tutorials/publish-subscribe_mqtt/).

![](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/2015/08/request-reply.png)

## Assumptions

This tutorial assumes the following:

*   You are familiar with Solace [core concepts](http://docs.solacesystems.com/Features/Core-Concepts.htm).
*   You have access to a running Solace message router with the following configuration:
    *   Enabled message VPN
    *   Enabled client username
    *   Enabled MQTT services on port 1883

One simple way to get access to a Solace message router is to start a Solace VMR load [as outlined here](http://docs.solacesystems.com/Solace-VMR-Set-Up/Starting-VMRs-for-the-First-Time/Setting-Up-an-Eval-VMR-in-AWS.htm). By default the Solace VMR will run with the “default” message VPN configured and ready for messaging and the MQTT service enabled on port 1883\. Going forward, this tutorial assumes that you are using the Solace VMR. If you are using a different Solace message router configuration, adapt the instructions to match your configuration.

Users can learn more details on enabling MQTT service on a Solace message router by referring to the [Solace Messaging Platform Feature Guide – Using MQTT](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Router_Feature_Provisioning/15_Configuring_MQTT_Messaging.html).

## Goals

The goal of this tutorial is to understand the following:

*   On the requestor side:
    1.  How to create a request
    2.  How to retrieve a unique Reply-To topic
    3.  How to receive a response
    4.  How to correlate the request and response
*   On the replier side:
    1.  How to detect a request expecting a reply
    2.  How to generate a reply message

## MQ Telemetry Transport (MQTT) Introduction

MQTT is a standard lightweight protocol for sending and receiving messages. As such, in addition to information provided on the Solace developer portal, you may also look at some external sources for more details about MQTT. The following are good places to start

1.  [http://mqtt.org/](http://mqtt.org/)
2.  [https://www.eclipse.org/paho/](https://www.eclipse.org/paho/)

## Overview

MQTT does not explicitly support the request-response message exchange pattern. However, this tutorial will implement the pattern by defining a topic structure to send requests and by obtaining a unique Reply-To topic from the Solace message router on which the response should be sent back to the requestor. Obtaining the Reply-To topic from a MQTT session is a Solace extension to MQTT and is achieved by adding a topic subscription to the designated special topic `“$SYS/client/reply-to”`. You can learn more details on requesting MQTT session information by referring to the [Solace Feature Provisioning – Managing MQTT Messaging](https://sftp.solacesystems.com/Portal_Docs/#page/Solace_Router_Feature_Provisioning/Managing_MQTT_Messaging.html).

This tutorial will be using the MQTT Quality of Service (QoS) level 0 to send and receive request and response messages, but it is possible to use any of the QoS level 0, 1, or 2 for the request response scenarios.

### Message Correlation

For request-response messaging to be successful it must be possible for the requestor to correlate the request with the subsequent response. In this tutorial two fields are added to the request message to enable request-reply correlation. The reply-to field can be used by the requestor to indicate a topic where the reply should be sent. A natural choice for this is to use a unique topic per client by requesting the Reply-To topic from Solace Message Routers, as described above in the Overview section.

The second requirement is to be able to detect the reply message from the stream of incoming messages. This is accomplished adding a correlation-id field. Repliers can include the same correlation-id in a reply message to allow the requestor to detect the corresponding reply. The figure below outlines this exchange.

![](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/2015/07/Request-Reply_diagram-1.png)

In this tutorial the payload of both the request and reply messages are formatted to JSON in order to add the reply-to field, the correlation-id field, and the message contents. You can use any payload format which both the requestor and replier understand, but for the purpose of this tutorial we choose JSON to structure the payload of the message and keep the tutorial simple. This tutorial will use the JSON-Simple Java library to both construct and parse the payload in JSON. The section below can be added to your pom.xml to configure the JSON-Simple dependency.

<pre class="brush: plain; title: ; notranslate" title=""><project ...>
  ...
  <dependencies>
    <dependency>
     <groupId>com.googlecode.json-simple</groupId>
      <artifactId>json-simple</artifactId>
      <version>1.1.1</version>
    </dependency>
  </dependencies>
</project>
</pre>

## Obtaining an MQTT Client Library

Although, you can use any MQTT Client library of your choice to connect to Solace, for the purposes of this tutorial, this tutorial will be using the [Paho Java Client library](https://www.eclipse.org/paho/clients/java/). This tutorial will use Apache Maven to download and manage the MQTT dependencies.

The two sections below can be added to your pom.xml to configure it use the Paho Java library from the Eclipse Nexus repository.

<pre class="brush: plain; title: ; notranslate" title=""><project ...>
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

## Connecting a session to the message router

This tutorial builds on the `TopicPublisher` introduced in Publish-Subscribe with MQTT. So connect the `MqttClient` as outlined in the [Publish-Subscribe with MQTT](http://dev.solacesystems.com/get-started/mqtt-tutorials/publish-subscribe_mqtt/) tutorial.

## Making a request

First let’s look at the requestor. This is the application that will send the initial request message and wait for the reply.

![](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/2015/07/Request-Reply_diagram-2.png)

The requestor must obtain the unique reply-to topic. Using Solace Message Routers, this can be accomplished by adding a subscription to the designated special topic `“$SYS/client/reply-to”`. The reply-to topic is received asynchronously through callbacks. These callbacks are defined in MQTT by the `MqttCallback` interface. The same callback is also used to receive the actual reply message. In order to distinguish between the two messages we inspect the topic string provided in the `MqttCallback.messageArrived` method.

<pre class="brush: plain; title: ; notranslate" title="">mqttClient.setCallback(new MqttCallback() {
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // If the topic is "$SYS/client/reply-to" then set our replyToTopic
        // to with the contents of the message payload received
        if (topic != null && topic.equals("$SYS/client/reply-to")) {
            replyToTopic = new String(message.getPayload());
        } else {
            try {
                // Received a response to our request
                Object obj = parser.parse(new String(message.getPayload()));
                JSONObject jsonPayload = (JSONObject) obj;
            } catch (ParseException ex) {
                System.out.println("Exception parsing response message!");
                ex.printStackTrace();
            }
        }

        latch.release(); // unblock main thread
    }

    public void connectionLost(Throwable cause) {
        System.out.println("Connection to Solace broker lost!" + cause.getMessage());
        latch.release();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }
});
</pre>

Now the requestor can add the subscription to the special designated topic:

<pre class="brush: plain; title: ; notranslate" title="">mqttClient.subscribe("$SYS/client/reply-to", 0);
</pre>

The requestor uses a semaphore to block the requestor thread until the reply-to message has been received. Once the reply-to message has been received, the topic is obtained from the payload of the message as shown above in the callback. You must then subscribe to the obtained reply-to topic in order to express interest in receiving responses. This tutorial uses a QoS level of 0 for at most once delivery for our response messages.

<pre class="brush: plain; title: ; notranslate" title="">mqttClient.subscribe(replyToTopic, 0);
</pre>

At this point the requestor is ready to send request messages and receive responses.

<pre class="brush: plain; title: ; notranslate" title="">// Create the request payload in JSON format
JSONObject obj = new JSONObject();
obj.put("correlationId", UUID.randomUUID().toString());
obj.put("replyTo", replyToTopic);
obj.put("message", "Sample Request");
String reqPayload = obj.toJSONString();

// Create a request message and set the request payload
MqttMessage reqMessage = new MqttMessage(reqPayload.getBytes());
reqMessage.setQos(0);

// Publish the request message
mqttClient.publish(requestTopic, reqMessage);
</pre>

The requestor uses a semaphore to block the requestor thread until the response message has been received.

<pre class="brush: plain; title: ; notranslate" title="">try {
    latch.acquire(); // block here until message received
} catch (InterruptedException e) {
    System.out.println("I was awoken while waiting");
}
</pre>

## Replying to a request

Now it is time to receive the request and generate an appropriate reply.  
![Request-Reply_diagram-3](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/2015/07/Request-Reply_diagram-3.png)

Similar to the requestor an `MqttClient` is created and connected to the Solace message router. Request messages are received asynchronously through callback defined by the `MqttCallback` interface. When a request message is received, the replier parses the payload of the message to a JSON object, constructs a reply message and adds the correlation-id field retrieved from the request payload. The reply message is published to the reply-to topic found in the body of the request message.

<pre class="brush: plain; title: ; notranslate" title="">mqttClient.setCallback(new MqttCallback() {

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            // Parse the received JSON request message
            Object payloadObj = parser.parse(new String(message.getPayload()));
            JSONObject jsonPayload = (JSONObject) payloadObj;

            String correlationId = (String) jsonPayload.get("correlationId");
            String replyTo = (String) jsonPayload.get("replyTo");
            String messageContent = (String) jsonPayload.get("message");

            // Create the response payload in JSON format
            JSONObject obj = new JSONObject();
            obj.put("correlationId", correlationId);
            obj.put("message", "Sample Response");
            String respPayload = obj.toJSONString();

            // Create a response message and set the response payload
            MqttMessage respMessage = new MqttMessage(respPayload.getBytes());
            respMessage.setQos(0);

            // Publish the response message using the MqttTopic class
            MqttTopic mqttTopic = mqttClient.getTopic(replyTo);
            mqttTopic.publish(respMessage);

            latch.countDown(); // unblock main thread
        } catch (ParseException ex) {
            System.out.println("Exception parsing request message!");
            ex.printStackTrace();
        }
    }

    public void connectionLost(Throwable cause) {
        System.out.println("Connection to Solace broker lost!" + cause.getMessage());
        latch.countDown();
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
    }

});
</pre>

Now the replier can add the topic subscription with a QoS level of 0, to express interest in receiving request messages.

<pre class="brush: plain; title: ; notranslate" title="">mqttClient.subscribe(requestTopic, 0);
</pre>

Then after the subscription is added, the replier is started. At this point the replier is ready to receive messages and send responses to your waiting requestor.

## Summarizing

Combining the example source code show above results in the following source code files:

*   [BasicRequestor.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/BasicRequestor.zip)
*   [BasicReplier.zip](http://2vs7bv4aq50r1hyri14a8xkf.wpengine.netdna-cdn.com/wp-content/uploads/mqtt/BasicReplier.zip)

### Building

Building these examples is simple. The following provides an example using Maven to compile and execute the sample. These instructions assume you have Apache Maven installed in your environment. There are many suitable ways to build and execute these samples in Java. Adapt these instructions to suit your needs depending on your environment.

Extract both the archive files and run the below command in each directory to compile the samples:

<pre class="brush: plain; title: ; notranslate" title="">cd BasicRequestor
mvn clean compile
cd ..
cd BasicReplier
mvn clean compile
</pre>

### Sample Output

If you start the `BasicReplier` with a single argument for the Solace message router host address it will connect and wait for a message. Replace HOST with the host address of your Solace VMR.

<pre class="brush: plain; title: ; notranslate" title="">$ mvn exec:java -Dexec.args="HOST"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building BasicReplier 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ BasicReplier ---
BasicReplier initializing...
Connecting to Solace broker: tcp://HOST
Connected
Subscribing client to request topic: T/GettingStarted/request
Waiting for request message...
</pre>

Then you can send a request message using the `BasicRequestor` again using a single argument to specify the Solace message router host address. If successful, the output for the requestor will look like the following:

<pre class="brush: plain; title: ; notranslate" title="">$ mvn exec:java -Dexec.args="HOST"
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building BasicRequestor 0.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ BasicRequestor ---
BasicRequestor initializing...
Connecting to Solace broker: tcp://HOST
Connected
Requesting Reply-To topic from Solace...

Received Reply-to topic from Solace for the MQTT client:
    Reply-To: _P2P/v:solace-vmr/_mqtt/HelloWorldBasicRequestor/169

Subscribing client to Solace provide Reply-To topic
Sending request to: T/GettingStarted/request

Received a response!
    Correl. Id: c6edf881-f211-4b3d-815f-87c3aeb95f22
    Message:    Sample Response

Exiting
</pre>

With the message delivered the replier output will look like the following:

<pre class="brush: plain; title: ; notranslate" title="">Received a request message!
    Correl. Id: c6edf881-f211-4b3d-815f-87c3aeb95f22
    Reply To:   _P2P/v:solace-vmr/_mqtt/HelloWorldBasicRequestor/169
    Message:    Sample Request

Sending response to: _P2P/v:solace-vmr/_mqtt/HelloWorldBasicRequestor/169
Exiting
</pre>

With that you now know how to successfully implement the request-reply message exchange pattern using MQTT.

If you have any issues sending and receiving a message, check the [Solace community Q&A](/community) for answers to common issues seen.