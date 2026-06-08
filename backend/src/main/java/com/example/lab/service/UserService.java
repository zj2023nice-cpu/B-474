package com.example.lab.service;

import com.example.lab.entity.User;
import com.example.lab.repository.UserRepository;
import com.example.lab.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;

    public User login(String username, String hashedPassword) {
        logger.info("验证用户登录: 用户名={}", username);
        
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            User existingUser = user.get();
            logger.info("用户存在: 用户名={}", username);
            
            if (PasswordUtil.verifyPassword(hashedPassword, existingUser.getSalt(), existingUser.getPassword())) {
                logger.info("登录验证通过: 用户名={}", username);
                return existingUser;
            } else {
                logger.warn("登录验证失败: 用户名={} - 密码不匹配", username);
            }
        } else {
            logger.warn("登录验证失败: 用户名={} - 用户不存在", username);
        }
        return null;
    }

    public User save(User user) {
        if (user.getSalt() == null || user.getSalt().isEmpty()) {
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(user.getPassword(), salt);
            user.setSalt(salt);
            user.setPassword(hashedPassword);
        }
        return userRepository.save(user);
    }

    public User updatePassword(User user, String newPassword) {
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(newPassword, salt);
        user.setSalt(salt);
        user.setPassword(hashedPassword);
        return userRepository.save(user);
    }
    
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
