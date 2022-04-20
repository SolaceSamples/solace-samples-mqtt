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
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A Mqtt topic publisher 
 *
 */
public class TopicPublisher {
	
	static boolean isShutdown = false;
    
    public void run(String... args) throws IOException {
        System.out.println("TopicPublisher initializing...");

        String host = args[0];
        String username = args[1];
        String password = "";
        if (args.length > 2) password = args[2];

        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient(host, "HelloWorldPub_" + UUID.randomUUID().toString().substring(0,8));
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            if (args.length > 2) connOpts.setPassword(password.toCharArray());
            
            // Connect the client
            System.out.println("Connecting to Solace messaging at " + host);
            mqttClient.connect(connOpts);
            System.out.println("Connected.  Press [ENTER] to quit.");

            for (int i=0; i<100; i++) {
	            // Create a Mqtt message
	            String content = "Hello world from MQTT!";
	            MqttMessage message = new MqttMessage(content.getBytes());
	            // Set the QoS on the Messages - 
	            // Here we are using QoS of 0 (equivalent to Direct Messaging in Solace)
	            message.setQos(0);
	            
	            System.out.println("Publishing message: " + content);
	            
	            // Publish the message
	            mqttClient.publish("solace/samples/mqtt/direct/pub", message);
	            try {
	            	Thread.sleep(1000);
	            } catch (InterruptedException e) {
	            	isShutdown = true;
	            }
	            if (System.in.available() != 0 || isShutdown) break;
            }
            // Disconnect the client
            mqttClient.disconnect();
            
            System.out.println("Messages published. Exiting");

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
            System.out.println("Usage: topicPublisher tcp://<host:port> <client-username> [client-password]");
            System.out.println();
            System.exit(-1);
        }
        new TopicPublisher().run(args);
    }
}
