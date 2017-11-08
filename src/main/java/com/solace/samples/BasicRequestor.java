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

import java.util.concurrent.Semaphore;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * A Mqtt basic requestor
 *
 */
public class BasicRequestor {
    
    // A unique Reply-To Topic for the client is obtained from Solace
    private String replyToTopic = "";
    
    private JSONParser parser = new JSONParser();
    
    public void run(String... args) {
        System.out.println("BasicRequestor initializing...");

        String host = args[0];
        String username = args[1];
        String password = args[2];

        if (!host.startsWith("tcp://")) {
            host = "tcp://" + host;
        }

        try {
            // Create an Mqtt client
            final MqttClient mqttClient = new MqttClient(host, "HelloWorldBasicRequestor");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            
            // Connect the client
            System.out.println("Connecting to Solace messaging at " + host);
            mqttClient.connect(connOpts);
            System.out.println("Connected");

            // Semaphore used for synchronizing b/w threads
            final Semaphore latch = new Semaphore(0);
            
            // Topic the client will use to send request messages
            final String requestTopic = "T/GettingStarted/request";
            
            // Callback - Anonymous inner-class for receiving the Reply-To topic from the Solace broker
            mqttClient.setCallback(new MqttCallback() {
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // If the topic is "$SYS/client/reply-to" then set our replyToTopic
                    // to with the contents of the message payload received
                    if (topic != null && topic.equals("$SYS/client/reply-to")) { 
                        replyToTopic = new String(message.getPayload());
                        System.out.println("\nReceived Reply-to topic from Solace for the MQTT client:" +
                            "\n\tReply-To: " + replyToTopic + "\n");
                    } else {
                        // Received a response to our request
                        try {
                            // Parse the response payload and convert to a JSONObject
                            Object obj = parser.parse(new String(message.getPayload()));
                            JSONObject jsonPayload = (JSONObject) obj;
                        
                            System.out.println("\nReceived a response!" +
                                    "\n\tCorrel. Id: " + (String) jsonPayload.get("correlationId") + 
                                    "\n\tMessage:    " + (String) jsonPayload.get("message") + "\n");
                        } catch (ParseException ex) {
                            System.out.println("Exception parsing response message!");
                            ex.printStackTrace();
                        }
                    }
                    
                    latch.release(); // unblock main thread
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to Solace messaging lost!" + cause.getMessage());
                    latch.release();
                }
                
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            
            // Subscribe client to the special Solace topic for requesting a unique
            // Reply-to destination for the MQTT client
            System.out.println("Requesting Reply-To topic from Solace...");
            mqttClient.subscribe("$SYS/client/reply-to", 0);
            
            // Wait for till we have received the reply to Topic
            try {
                latch.acquire();
            } catch (InterruptedException e) {
                System.out.println("I was awoken while waiting");
            }
            
            // Check if we have a Reply-To topic
            if(replyToTopic == null || replyToTopic.isEmpty())
            {
                System.out.println("Unable to request Reply-To from Solace. Exiting");
                System.exit(0);
            }
            
            // Subscribe client to the Solace provide Reply-To topic with a QoS level of 0
            System.out.println("Subscribing client to Solace provide Reply-To topic");
            mqttClient.subscribe(replyToTopic, 0);
            
            // Create the request payload in JSON format
            JSONObject obj = new JSONObject();
            obj.put("correlationId", UUID.randomUUID().toString());
            obj.put("replyTo", replyToTopic);
            obj.put("message", "Sample Request");
            String reqPayload = obj.toJSONString();
            
            // Create a request message and set the request payload
            MqttMessage reqMessage = new MqttMessage(reqPayload.getBytes());
            reqMessage.setQos(0);
    
            System.out.println("Sending request to: " + requestTopic);
    
            // Publish the request message
            mqttClient.publish(requestTopic, reqMessage);

            // Wait for till we have received a response
            try {
                latch.acquire(); // block here until message received
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
            System.out.println("Usage: basicRequestor <host:port> <client-username> <client-password>");
            System.out.println();
            System.exit(-1);
        }
        
        new BasicRequestor().run(args);
    }
}
