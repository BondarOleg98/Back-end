package com.netcraker.services.impl;

import com.netcraker.model.AuthorizationLinks;
import com.netcraker.model.User;
import com.netcraker.repositories.AuthorizationRepository;
import com.netcraker.repositories.impl.AuthorizationRepositoryImpl;
import com.netcraker.security.filter.JwtAuthenticationFilter;
import com.netcraker.services.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class RecoveryServiceImp implements RecoveryService {
    private final AuthEmailSenderService emailSender;
    private final AuthorizationRepository authRepo;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final PasswordGenerator passGenerator;
    private static final Logger logger = LoggerFactory.getLogger(RecoveryServiceImp.class);
    @Override
    public boolean sendRecoveryCode(String email) {
        final User fromDb = userService.findByEmail(email);
        if (fromDb == null) return false;
        final AuthorizationLinks recoveryLink = authRepo.createRecoveryLink(fromDb);
        emailSender.sendRecoveryLink(fromDb, recoveryLink);
        return true;
    }

    @Transactional
    @Override
    public Optional<User> recoverPassword(String recoveryCode) {
        AuthorizationLinks linkFromDb;
        try {
            linkFromDb = authRepo.findByActivationCode(recoveryCode);
        } catch (DataAccessException e) {
            logger.error("RecoveryServiceImpl::recoverPassword. Stack trace: ");
            e.printStackTrace();
            return Optional.empty();
        }

        if (linkFromDb == null || linkFromDb.isUsed()) {
            return Optional.empty();
        }

        final User userFromDb = userService.findByUserId(linkFromDb.getUserId());

        final String newPassword = passGenerator.generatePassword();

        emailSender.sendNewGeneratedPassword(userFromDb, newPassword);

        userFromDb.setPassword(encoder.encode(newPassword));
        userService.updateUser(userFromDb, userFromDb);

        linkFromDb.setUsed(true);
        authRepo.updateAuthorizationLinks(linkFromDb);

        return Optional.of(userFromDb);
    }
}
