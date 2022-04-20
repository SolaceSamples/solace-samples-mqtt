/*
 * Copyright 2016-2022 Solace Corporation. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.solace.samples;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A Mqtt topic subscriber
 *
 */
public class TopicSubscriber {

    static boolean isShutdown = false;

    public void run(String... args) throws IOException {
        System.out.println("TopicSubscriber initializing...");

        String host = args[0];
        String username = args[1];
        String password = "";
        if (args.length > 2) password = args[2];

        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient(host, "HelloWorldSub_" + UUID.randomUUID().toString().substring(0,8));
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            if (args.length > 2) connOpts.setPassword(password.toCharArray());
            
            // Connect the client
            System.out.println("Connecting to Solace messaging at "+host);
            mqttClient.connect(connOpts);
            System.out.println("Connected");

            // Topic filter the client will subscribe to
            final String subTopic = "solace/samples/+/direct/#";
            
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
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to Solace messaging lost!" + cause.getMessage());
                    isShutdown = true;
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                }

            });
            
            // Subscribe client to the topic filter and a QoS level of 0
            System.out.println("Subscribing client to topic: " + subTopic);
            mqttClient.subscribe(subTopic, 0);
            System.out.println("Subscribed. Press [ENTER] to quit.");

            // Wait for the message to be received
            
            try {
                while (System.in.available() == 0 && !isShutdown) {
                    Thread.sleep(1000);  // wait 1 second
                }
            } catch (InterruptedException e) {
                // Thread.sleep() interrupted... probably getting shut down
            }
            
            // Disconnect the client
            mqttClient.disconnect();
            System.out.println("Exiting");

            System.exit(0);
        } catch (MqttException me) {
            System.out.println("Exception:   " + me);
            System.out.println("Reason Code: " + me.getReasonCode());
            System.out.println("Message:     " + me.getMessage());
            if (me.getCause() != null) System.out.println("Cause:       " + me.getCause());
            me.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Check command line arguments
        if (args.length < 2) {
            System.out.println("Usage: topicSubscriber tcp://<host:port> <client-username> [client-password]");
            System.out.println();
            System.exit(-1);
        }
        new TopicSubscriber().run(args);
    }
}
