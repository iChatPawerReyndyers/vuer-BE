package com.vuer.email.service;

import com.vuer.device.entity.Device;
import com.vuer.device.repository.DeviceRepository;
import com.vuer.email.entity.LinkedEmailAccount;
import com.vuer.email.repository.LinkedEmailAccountRepository;
import com.vuer.message.dto.MessageIngestRequest;
import com.vuer.message.entity.ChannelType;
import com.vuer.message.service.MessageService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Background worker that polls all linked IMAP email inboxes every 30 seconds.
 *
 * For each active LinkedEmailAccount:
 *  - Connects to the IMAP server with stored credentials
 *  - Searches for UNSEEN (unread) messages
 *  - Ingests each email as a Vuer message with channelType=EMAIL
 *  - Marks the email as SEEN to prevent duplicate ingestion
 *
 * FR-2.3: Real-Time Network Email Polling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPollingService {

    private final LinkedEmailAccountRepository linkedEmailAccountRepository;
    private final DeviceRepository deviceRepository;
    private final MessageService messageService;

    @Scheduled(fixedDelay = 30_000)
    public void pollAllInboxes() {
        List<LinkedEmailAccount> accounts = linkedEmailAccountRepository.findAllByIsActiveTrue();
        if (accounts.isEmpty()) {
            log.debug("Email polling: no active linked accounts to poll.");
            return;
        }
        log.info("Email polling: checking {} linked inbox(es)...", accounts.size());
        for (LinkedEmailAccount account : accounts) {
            try {
                pollInbox(account);
            } catch (Exception e) {
                log.error("Email polling failed for account [{}] ({}): {}",
                        account.getDisplayLabel(), account.getUsername(), e.getMessage(), e);
            }
        }
    }

    private void pollInbox(LinkedEmailAccount account) throws MessagingException, IOException {
        Properties props = buildImapProperties(account);
        Session session = Session.getInstance(props);

        try (Store store = session.getStore("imaps")) {
            store.connect(account.getImapHost(), account.getImapPort(),
                    account.getUsername(), account.getEncryptedPassword());

            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);

                // Find only UNSEEN (unread) messages
                Message[] unseenMessages = inbox.search(
                        new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                if (unseenMessages.length == 0) {
                    log.debug("No new emails for [{}]", account.getDisplayLabel());
                    return;
                }

                log.info("Found {} new email(s) for [{}]", unseenMessages.length, account.getDisplayLabel());

                // Find the first active device for this user to associate with the message
                List<Device> devices = deviceRepository.findAllByUserId(account.getUserId());
                Device device = devices.stream()
                        .filter(Device::isActive)
                        .findFirst()
                        .orElse(null);

                if (device == null) {
                    log.warn("No active device found for user {} — skipping email ingestion.", account.getUserId());
                    return;
                }

                for (Message message : unseenMessages) {
                    try {
                        ingestEmail(device.getDeviceToken(), message);
                        // Mark as SEEN to prevent re-ingestion on next poll
                        message.setFlag(Flags.Flag.SEEN, true);
                    } catch (Exception e) {
                        log.error("Failed to ingest email (subject: {}): {}",
                                message.getSubject(), e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void ingestEmail(String deviceToken, Message message) throws MessagingException, IOException {
        String from = message.getFrom() != null && message.getFrom().length > 0
                ? message.getFrom()[0].toString()
                : "Unknown Sender";

        String subject = message.getSubject() != null ? message.getSubject() : "";
        String body = extractTextBody(message);
        LocalDateTime sentAt = toLocalDateTime(message.getSentDate());

        MessageIngestRequest request = new MessageIngestRequest(
                deviceToken,
                from,
                body,
                ChannelType.EMAIL,
                subject,
                sentAt
        );

        messageService.ingestMessage(request);
        log.debug("Ingested email from '{}' subject '{}'", from, subject);
    }

    /**
     * Extracts the plain-text body from a message.
     * Handles simple text/plain, text/html (stripped), and multipart.
     */
    private String extractTextBody(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("text/html")) {
            // Strip HTML tags for a clean plain-text preview
            String html = (String) part.getContent();
            return html.replaceAll("<[^>]*>", "").trim();
        }
        if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                String partText = extractTextBody(multipart.getBodyPart(i));
                if (!partText.isBlank()) {
                    sb.append(partText);
                    break; // Take the first non-empty part
                }
            }
            return sb.toString();
        }
        return "";
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Properties buildImapProperties(LinkedEmailAccount account) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", account.getImapHost());
        props.put("mail.imaps.port", String.valueOf(account.getImapPort()));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "10000");
        return props;
    }
}
