/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  https://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package amazonaws.sqs.example;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import javax.jms.*;
import java.util.concurrent.TimeUnit;

/**
 * An example class to demonstrate the behavior of CLIENT_ACKNOWLEDGE mode for received messages. This example
 * complements the example given in {@link SyncMessageReceiverUnorderedAcknowledge} for UNORDERED_ACKNOWLEDGE mode.
 *
 * First, a session, a message producer, and a message consumer are created. Then, two messages are sent. Next, two messages
 * are received but only the second one is acknowledged. After waiting for the visibility time out period, an attempt to
 * receive another message is made. It's shown that no message is returned for this attempt since in CLIENT_ACKNOWLEDGE mode,
 * as expected, all the messages prior to the acknowledged messages are also acknowledged.
 *
 * This ISN'T the behavior for UNORDERED_ACKNOWLEDGE mode. Please see {@link SyncMessageReceiverUnorderedAcknowledge}
 * for an example.
 */
public class SyncMessageReceiverClientAcknowledge {
 
    // Visibility time-out for the queue. It must match to the one set for the queue for this example to work.
    private static final long TIME_OUT_SECONDS = 1;
 
    public static void main(String args[]) throws JMSException, InterruptedException {
        // Create the configuration for the example
        ExampleConfiguration config = ExampleConfiguration.parseConfig("SyncMessageReceiverClientAcknowledge", args);
 
        // Setup logging for the example
        ExampleCommon.setupLogging();
 
        // Create the connection factory based on the config
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.standard()
                        .withRegion(config.getRegion().getName())
                        .withCredentials(config.getCredentialsProvider())
                );
 
        // Create the connection
        SQSConnection connection = connectionFactory.createConnection();
 
        // Create the queue if needed
        ExampleCommon.ensureQueueExists(connection, config.getQueueName());
 
        // Create the session  with client acknowledge mode
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
 
        // Create the producer and consume
        MessageProducer producer = session.createProducer(session.createQueue(config.getQueueName()));
        MessageConsumer consumer = session.createConsumer(session.createQueue(config.getQueueName()));
 
        // Open the connection
        connection.start();
 
        // Send two text messages
        sendMessage(producer, session, "Message 1");
        sendMessage(producer, session, "Message 2");
 
        // Receive a message and don't acknowledge it
        receiveMessage(consumer, false);
 
        // Receive another message and acknowledge it
        receiveMessage(consumer, true);
 
        // Wait for the visibility time out, so that unacknowledged messages reappear in the queue
        System.out.println("Waiting for visibility timeout...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_OUT_SECONDS));
 
        // Attempt to receive another message and acknowledge it. This results in receiving no messages since
        // we have acknowledged the second message. Although we didn't explicitly acknowledge the first message,
        // in the CLIENT_ACKNOWLEDGE mode, all the messages received prior to the explicitly acknowledged message
        // are also acknowledged. Therefore, we have implicitly acknowledged the first message.
        receiveMessage(consumer, true);
 
        // Close the connection. This closes the session automatically
        connection.close();
        System.out.println("Connection closed.");
    }
 
    /**
     * Sends a message through the producer.
     *
     * @param producer Message producer
     * @param session Session
     * @param messageText Text for the message to be sent
     * @throws JMSException
     */
    private static void sendMessage(MessageProducer producer, Session session, String messageText) throws JMSException {
        // Create a text message and send it
        producer.send(session.createTextMessage(messageText));
    }
 
    /**
     * Receives a message through the consumer synchronously with the default timeout (TIME_OUT_SECONDS).
     * If a message is received, the message is printed. If no message is received, "Queue is empty!" is
     * printed.
     *
     * @param consumer Message consumer
     * @param acknowledge If true and a message is received, the received message is acknowledged.
     * @throws JMSException
     */
    private static void receiveMessage(MessageConsumer consumer, boolean acknowledge) throws JMSException {
        // Receive a message
        Message message = consumer.receive(TimeUnit.SECONDS.toMillis(TIME_OUT_SECONDS));
 
        if (message == null) {
            System.out.println("Queue is empty!");
        } else {
            // Since this queue has only text messages, cast the message object and print the text
            System.out.println("Received: " + ((TextMessage) message).getText());
 
            // Acknowledge the message if asked
            if (acknowledge) message.acknowledge();
        }
    }
}
