package com.example.authservice.service;

import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  public UserDetails loadUserByUsername(String cic) throws UsernameNotFoundException {
    com.example.authservice.models.User user = userRepository.findByCIC(cic);
    if (user == null) {
      throw new UsernameNotFoundException("User not found with username: " + cic);
    }
    return new User(user.getCIC(), user.getPassword(), new ArrayList<>());
  }
}
