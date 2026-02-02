package com.example.auth.service;

import com.example.auth.repository.UserRepository;
import com.example.common.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ১. ইউজার খুঁজে বের করা
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // ২. ডুপ্লিকেট রিমুভ করার জন্য Set ব্যবহার করা (Production Standard)
        // যেহেতু একজন ইউজারের একাধিক রোলে একই পারমিশন থাকতে পারে
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    // রোল (e.g., ROLE_ADMIN)
                    Stream<SimpleGrantedAuthority> roleStream = Stream.of(
                            new SimpleGrantedAuthority("ROLE_" + role.getName())
                    );

                    // পারমিশন (e.g., READ, WRITE)
                    Stream<SimpleGrantedAuthority> permissionStream = role.getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getName()));

                    return Stream.concat(roleStream, permissionStream);
                })
                .collect(Collectors.toSet()); // List এর বদলে Set ব্যবহার করুন

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
