package com.vuer.email.controller;

import com.vuer.email.dto.LinkEmailRequest;
import com.vuer.email.dto.LinkedEmailResponse;
import com.vuer.email.entity.LinkedEmailAccount;
import com.vuer.email.repository.LinkedEmailAccountRepository;
import com.vuer.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST endpoints for managing linked IMAP email accounts per user.
 *
 * POST   /api/email-accounts        - Link a new inbox
 * GET    /api/email-accounts        - List linked inboxes (no passwords returned)
 * DELETE /api/email-accounts/{id}   - Unlink (soft-delete) an inbox
 */
@RestController
@RequestMapping("/api/email-accounts")
@RequiredArgsConstructor
public class LinkedEmailController {

    private final LinkedEmailAccountRepository linkedEmailAccountRepository;

    @PostMapping
    public ResponseEntity<LinkedEmailResponse> linkEmailAccount(
            @AuthenticationPrincipal User user,
            @RequestBody LinkEmailRequest request) {

        LinkedEmailAccount account = LinkedEmailAccount.builder()
                .userId(user.getId())
                .imapHost(request.imapHost())
                .imapPort(request.imapPort() > 0 ? request.imapPort() : 993)
                .username(request.username())
                .encryptedPassword(request.password()) // AesEncryptor converts on persist
                .displayLabel(request.displayLabel() != null ? request.displayLabel() : request.username())
                .isActive(true)
                .build();

        LinkedEmailAccount saved = linkedEmailAccountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<LinkedEmailResponse>> getLinkedAccounts(
            @AuthenticationPrincipal User user) {
        List<LinkedEmailResponse> accounts = linkedEmailAccountRepository
                .findAllByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unlinkEmailAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        linkedEmailAccountRepository.findById(id).ifPresent(account -> {
            if (account.getUserId().equals(user.getId())) {
                account.setActive(false); // Soft delete
                linkedEmailAccountRepository.save(account);
            }
        });
        return ResponseEntity.noContent().build();
    }

    private LinkedEmailResponse toResponse(LinkedEmailAccount account) {
        return new LinkedEmailResponse(
                account.getId(),
                account.getDisplayLabel(),
                account.getUsername(),
                account.getImapHost(),
                account.getImapPort(),
                account.isActive()
        );
    }
}
