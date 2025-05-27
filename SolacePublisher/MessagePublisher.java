import com.solacesystems.jcsmp.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessagePublisher {

    private static final Logger logger = LogManager.getLogger(MessagePublisher.class);

    private final XMLMessageProducer producer;
    private final Destination destination;
    private final Runnable shutdownCallback;
    private final int maxRetries = 3;
    private final long[] backoffDelays = {100, 1000, 5000}; // in milliseconds

    public MessagePublisher(JCSMPSession session, String destinationName, String destinationType, Runnable shutdownCallback) throws JCSMPException {
        this.producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void responseReceived(String messageID) {
                // Acknowledgement from Solace (optional)
            }

            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                logger.error("Error for message ID {}: {}", messageID, e.getMessage(), e);
            }
        });

        if ("queue".equalsIgnoreCase(destinationType)) {
            this.destination = JCSMPFactory.onlyInstance().createQueue(destinationName);
        } else {
            this.destination = JCSMPFactory.onlyInstance().createTopic(destinationName);
        }

        this.shutdownCallback = shutdownCallback;
    }

    public void publish(byte[] data) {
        BytesMessage message = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
        message.setData(data);

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                producer.send(message, destination);
                return; // success
            } catch (JCSMPException e) {
                logger.warn("Publish attempt {} failed: {}", attempt + 1, e.getMessage());

                if (attempt < backoffDelays.length) {
                    try {
                        Thread.sleep(backoffDelays[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("Failed to publish message after {} retries. Initiating shutdown.", maxRetries);
        shutdownCallback.run();
    }
}
