package com.SmartContact.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.SmartContact.Model.User;
import com.SmartContact.Repository.UserRepo;

public class UserDetailsServiceImpl implements UserDetailsService{

	@Autowired
	private UserRepo userrepo;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userrepo.getUserByUserName(username);
		if(user == null) {
			throw new UsernameNotFoundException("Couldn't found user !!");
		}
		
		CustomUserDetails custom = new CustomUserDetails(user);
		
		return custom;
	}

}
