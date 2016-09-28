/**
 *  Copyright 2015 Solace Systems, Inc. All rights reserved.
 * 
 *  http://www.solacesystems.com
 * 
 *  This source is distributed under the terms and conditions of
 *  any contract or license agreement between Solace Systems, Inc.
 *  ("Solace") and you or your company. If there are no licenses or
 *  contracts in place use of this source is not authorized. This 
 *  source is provided as is and is not supported by Solace unless
 *  such support is provided for under an agreement signed between 
 *  you and Solace.
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

        try {
            // Create an Mqtt client
            MqttClient mqttClient = new MqttClient("tcp://" + args[0], "ConfirmedDeliveryProducer");
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            
            // Connect the client
            System.out.println("Connecting to Solace broker: tcp://" + args[0]);
            mqttClient.connect(connOpts);
            System.out.println("Connected");
            
            // Latch used for synchronizing b/w threads
            final CountDownLatch latch = new CountDownLatch(1);
            
            // Callback - Anonymous inner-class for receiving msg delivery complete notifications
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
        if (args.length < 1) {
            System.out.println("Usage: ConfirmedDeliveryProducer <msg_backbone_ip:port>");
            System.exit(-1);
        }

		ConfirmedDeliveryProducer app = new ConfirmedDeliveryProducer();
		app.run(args);
    }
}
