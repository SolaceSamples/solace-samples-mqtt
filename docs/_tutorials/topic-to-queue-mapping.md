---
layout: tutorials
title: Topic to Queue Mapping
summary: Learn how to map existing topics to Solace queues.
icon: topic-to-queue-mapping.png
---

The goal of this tutorial is to show you how to make use of one of Solace’s advanced queueing features called “Topic to Queue Mapping.”

![topic-to-queue-mapping]({{ site.baseurl }}/images/topic-to-queue-mapping.png)

In addition to spooling messages published directly to the queue, it is possible to add one or more topic subscriptions to a durable queue so that messages published to those topics are also delivered to and spooled by the queue. This is a powerful feature that enables queues to participate equally in point to point and publish / subscribe messaging models. More details about the [“Topic to Queue Mapping” feature here](http://docs.solacesystems.com/Features/Core-Concepts.htm#topic-queue-mapping){:target="_top"}.

The following diagram illustrates this feature.  
<img src="{{ site.baseurl }}/images/topic-to-queue-mapping-detail.png" width="500" height="206" />

If you have a durable queue named “Q”, it will receive messages published directly to the queue destination named “Q”. However, it is also possible to add subscriptions to this queue in the form of topics. This example adds topics “A” and “B”. Once these subscriptions are added, the queue will start receiving messages published to the topic destinations “A” and “B”. When you combine this with the wildcard support provided by Solace topics this opens up a number of interesting use cases.

## Topic to Queue Mapping and MQTT

MQTT is a standard wireline protocol with a design goal of being a light weight publish/subscribe protocol. MQTT inherently supports this feature via QoS 1 subscriptions. There is nothing further applications need to do to directly take advantage of the Solace Topic to Queue Mapping feature. However, the Solace message router support for QoS 1 subscriptions inherently takes advantage of this feature to provide the QoS 1 MQTT service. Simply follow steps outlined in the [Persistence Tutorial]({{ site.baseurl }}/persistence-with-queues) for QoS 1 messaging and MQTT clients are using this feature under the covers.

## Summarizing

So in summary MQTT applications take advantage of this feature by using QoS 1 subscriptions. There is nothing further that they must do.

If you have any issues or questions check the [Solace community](http://dev.solacesystems.com/community/){:target="_top"} for answers and discussions.