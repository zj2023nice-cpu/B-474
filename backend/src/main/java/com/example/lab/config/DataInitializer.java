package com.example.lab.config;

import com.example.lab.entity.User;
import com.example.lab.repository.UserRepository;
import com.example.lab.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                String frontEndHashed = PasswordUtil.hashPasswordWithoutSalt("admin");
                String salt = PasswordUtil.generateSalt();
                String backEndHashed = PasswordUtil.hashPassword(frontEndHashed, salt);
                admin.setSalt(salt);
                admin.setPassword(backEndHashed);
                admin.setRole("ADMIN");
                admin.setName("系统管理员");
                userRepository.save(admin);
            }
            
            if (userRepository.findByUsername("teacher").isEmpty()) {
                User teacher = new User();
                teacher.setUsername("teacher");
                String frontEndHashed = PasswordUtil.hashPasswordWithoutSalt("123456");
                String salt = PasswordUtil.generateSalt();
                String backEndHashed = PasswordUtil.hashPassword(frontEndHashed, salt);
                teacher.setSalt(salt);
                teacher.setPassword(backEndHashed);
                teacher.setRole("TEACHER");
                teacher.setName("张老师");
                userRepository.save(teacher);
            }
            
            if (userRepository.findByUsername("student").isEmpty()) {
                User student = new User();
                student.setUsername("student");
                String frontEndHashed = PasswordUtil.hashPasswordWithoutSalt("123456");
                String salt = PasswordUtil.generateSalt();
                String backEndHashed = PasswordUtil.hashPassword(frontEndHashed, salt);
                student.setSalt(salt);
                student.setPassword(backEndHashed);
                student.setRole("STUDENT");
                student.setName("李同学");
                userRepository.save(student);
            }
        };
    }
}
