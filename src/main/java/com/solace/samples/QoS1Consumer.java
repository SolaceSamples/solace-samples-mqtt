/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.solace.samples;

import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttCallback;

/**
 * A Mqtt QoS1 message consumer
 *
 */
public class QoS1Consumer {

    public void run(String... args) {
        System.out.println("QoS1Consumer initializing...");

        String host = args[0];
        String username = args[1];
        String password = args[2];

        if (!host.startsWith("tcp://")) {
            host = "tcp://" + host;
        }
        try {
            // Create an Mqtt client
            MqttAsyncClient mqttClient = new MqttAsyncClient(host, "HelloWorldQoS1Consumer");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            // Connect the client
            System.out.println("Connecting to Solace messaging at " + host);
            IMqttToken conToken = mqttClient.connect(connOpts);
            conToken.waitForCompletion(10000);
            if (!conToken.isComplete() || conToken.getException() != null) {
                System.out.println("Error connecting: " + conToken.getException());
                System.exit(-1);
            }
            System.out.println("Connected");

            // Latch used for synchronizing b/w threads
            final CountDownLatch latch = new CountDownLatch(1);

            // Callback - Anonymous inner-class for receiving messages
            mqttClient.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Called when a message arrives from the server that
                    // matches any subscription made by the client
                    String time = new Timestamp(System.currentTimeMillis()).toString();
                    System.out.println("\nReceived a Message!" +
                            "\n\tTime:    " + time + 
                            "\n\tTopic:   " + topic + 
                            "\n\tMessage: " + new String(message.getPayload()) + 
                            "\n\tQoS:     " + message.getQos() + "\n");
                    latch.countDown(); // unblock main thread
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to Solace messaging lost!" + cause.getMessage());
                    latch.countDown();
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                }

            });

            // Topic filter the client will subscribe to
            final String subTopic = "Q/tutorial";

            // Subscribe client to the topic filter with QoS level of 1
            System.out.println("Subscribing client to topic: " + subTopic);
            IMqttToken subToken = mqttClient.subscribe(subTopic, 1);
            subToken.waitForCompletion(10000);
            if (!subToken.isComplete() || subToken.getException() != null) {
                System.out.println("Error subscribing: " + subToken.getException());
                System.exit(-1);
            }
            if (subToken.getGrantedQos()[0] != 1) {
                System.out.println("Expected OoS level 1 but got OoS level: " + subToken.getGrantedQos()[0]);
                System.exit(-1);
            }
            System.out.println("Subscribed with OoS level 1 and waiting to receive msgs");

            // Wait for the message to be received
            try {
                latch.await(); // block here until message received, and latch will flip
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }

            // Disconnect the client
            mqttClient.disconnect();
            System.out.println("Exiting");

            System.exit(0);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length != 3) {
            System.out.println("Usage: QoS1Consumer <host:port> <client-username> <client-password>");
            System.out.println();
            System.exit(-1);
        }

        new QoS1Consumer().run(args);
    }
}
