package com.netcraker.services.impl;

import com.netcraker.exceptions.FailedToRegisterException;
import com.netcraker.exceptions.FindException;
import com.netcraker.exceptions.UpdateException;
import com.netcraker.model.AuthorizationLinks;
import com.netcraker.model.Role;
import com.netcraker.model.User;
import com.netcraker.repositories.UserRepository;
import com.netcraker.repositories.impl.AuthorizationRepositoryImpl;
import com.netcraker.repositories.impl.RoleRepositoryImpl;
import com.netcraker.repositories.impl.UserRoleRepositoryImpl;
import com.netcraker.services.AuthEmailSenderService;
import com.netcraker.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@PropertySource("classpath:email-messages.properties")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AuthorizationRepositoryImpl authorizationRepositoryImpl;
    private final RoleRepositoryImpl roleRepository;
    private final UserRoleRepositoryImpl userRoleRepositoryImpl;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailSenderService emailSender;

    @Override
    public User createUsualUser(User user) {
        user.setEnabled(false);
        final User registered = createUser(user);
        final AuthorizationLinks authorizationLink = authorizationRepositoryImpl.creteAuthorizationLinks(registered);
        emailSender.sendActivationCode(user, authorizationLink);
        return registered;
    }

    @Override
    public boolean activateUser(String token) {
        AuthorizationLinks authorizationLinks;
        try {
            authorizationLinks = authorizationRepositoryImpl.findByActivationCode(token);
        } catch (DataAccessException e) {
            e.printStackTrace();
            return false;
        }

        if (authorizationLinks == null || authorizationLinks.isUsed()) {
            return false;
        }
        System.out.println("Auth link has user's id:" + authorizationLinks.getUserId());
        Optional<User> userOpt = userRepository.getById(authorizationLinks.getUserId());

        if (!userOpt.isPresent()) {
            return false;
        }
        User user = userOpt.get();
        authorizationLinks.setUsed(true);
        user.setEnabled(true);
        userRepository.update(user);
        authorizationRepositoryImpl.updateAuthorizationLinks(authorizationLinks);
        return true;
    }

    @Override
    public User createAdminModerator(User user, List<Role> roles) {
        user.setEnabled(true);
        final User registered = createUser(user);
        for (Role role : roles) {
            Optional<Role> roleFromDB = roleRepository.findByName(role.getName());
            if (!roleFromDB.isPresent()) {
                throw new FindException("Role not found");
            }
            Role roleFind = roleFromDB.get();
            userRoleRepositoryImpl.insert(registered, roleFind);
        }

        return user;
    }

    private User createUser(User user) {
        Optional<User> userFromDB = userRepository.findByEmail(user.getEmail());
        if (userFromDB.isPresent()) {
            throw new FailedToRegisterException("Email is already used");
        }
        //for hashing
        // user.setPassword(passwordEncoder.encode(user.getPassword()));

        final User registered = userRepository.insert(user)
                .orElseThrow(() -> new FailedToRegisterException("Error in creating user! Email is free, but creation query failure."));

        System.out.println("user is created with id: " + registered.getUserId());

        return registered;
    }

    @Override
    public List<User> searchUser(String searchExpression, Optional<User> currentUser) {
        // TODO should we set user roles here?
        Role user = roleRepository.findByName("USER").get();
        if (!currentUser.isPresent() || roleRepository.getAllRoleById(currentUser.get().getUserId()).contains(user)) {
            return userRepository.findByEmailOrFullNameFilterByRole("%" + searchExpression + "%", user);
        } else {
            List<Role> allRoles = roleRepository.getAllRoles();
            allRoles.remove(user);
            return userRepository.findByEmailOrFullNameFilterByRoleWithout("%" + searchExpression + "%", user);
        }
    }

    @Override
    public User findByUserId(int userId) {
        return userRepository.getById(userId).orElse(null);
    }

    @Override
    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        assert user != null;
        final List<Role> roles = roleRepository.getAllRoleById(user.getUserId());
        user.setRoles(roles);
        return user;
    }

    @Override
    public void updateUser(User oldUser, User newUser) {
        System.out.println(newUser + " to update");
        userRepository.update(newUser);
    }

    @Override
    public void updateAdminModerator(User newUser, List<Role> roles) {
        userRepository.update(newUser);
        for (Role role : roles) {
            Optional<Role> roleFromDB = roleRepository.findByName(role.getName());
            if (!roleFromDB.isPresent()) {
                throw new FindException("Role not found");
            }
            Role roleFind = roleFromDB.get();
            userRoleRepositoryImpl.update(newUser, roleFind);
        }
    }

    @Override
    public void deleteAdminModerator(int id) {
        userRepository.delete(id);
    }

    public boolean equalsPassword(User user, String rawPassword) {
        System.out.println("Old password: " + user.getPassword() + " new password: " + rawPassword);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public User changePassword(int userId, String oldPass, String newPass) {
        final User user = this.userRepository.getById(userId)
                .orElseThrow(() -> new FindException("Cannot find user with such id"));

        if (!equalsPassword(user, oldPass))
            throw new UpdateException("Wrong entered old password");

        user.setPassword(passwordEncoder.encode(newPass));

        return userRepository.update(user)
                .orElseThrow(() -> new UpdateException("Cannot update password"));
    }
}