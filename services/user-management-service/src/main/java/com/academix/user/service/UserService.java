package com.academix.user.service;

import com.academix.user.model.User;
import com.academix.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;


    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }


    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }


    @Transactional(readOnly = true)
    public List<User> findAll() {
        log.debug("Finding all users");
        return userRepository.findAll();
    }


    public User save(User user) {
        log.debug("Saving user: {}", user.getUsername());
        return userRepository.save(user);
    }


    public void deleteById(Long id) {
        log.debug("Deleting user by ID: {}", id);
        userRepository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }


    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


    @Transactional(readOnly = true)
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
