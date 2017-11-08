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

import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;

/**
 * A Mqtt QoS1 message producer with delivery confirmation
 *
 */
public class ConfirmedDeliveryProducer {
    
    public void run(String... args) {
        System.out.println("ConfirmedDeliveryProducer initializing...");
        String host = args[0];
        String username = args[1];
        String password = args[2];

        if (!host.startsWith("tcp://")) {
            host = "tcp://" + host;
        }
        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient(host, "ConfirmedDeliveryProducer");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            
            // Connect the client
            System.out.println("Connecting to Solace messaging at " + args[0]);
            mqttClient.connect(connOpts);
            System.out.println("Connected");
            
            // Latch used for synchronizing b/w threads
            final CountDownLatch latch = new CountDownLatch(1);
            
            // Callback - Anonymous inner-class for receiving msg delivery complete notifications
            mqttClient.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to Solace messaging lost!" + cause.getMessage());
                    latch.countDown();
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("\nMessage was successfully delivered to Solace\n");
                    latch.countDown(); // unblock main thread
                }
            });

            // Create a Mqtt message
            String content = "Hello world from MQTT!";
            MqttMessage message = new MqttMessage(content.getBytes());
            // Set the QoS on the Messages - 
            // Here we are using QoS of 1 (equivalent to Persistent Messages in Solace)
            message.setQos(1);
            
            System.out.println("Publishing message: " + content);
            
            // Publish the message
            mqttClient.publish("Q/tutorial", message);
            
            // Wait for the delivery complete notification
            try {
                latch.await(); // block here until message delivery is completed, and latch will flip
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
            System.out.println("Usage: confirmedPublish <host:port> <client-username> <client-password>");
            System.out.println();
            System.exit(-1);
        }
        ConfirmedDeliveryProducer app = new ConfirmedDeliveryProducer();
        app.run(args);
    }
}
