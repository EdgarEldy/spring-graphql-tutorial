package com.edgareldy.springgraphqltutorial.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import com.edgareldy.springgraphqltutorial.entity.ActivationToken;
import com.edgareldy.springgraphqltutorial.entity.BlacklistedToken;
import com.edgareldy.springgraphqltutorial.entity.PasswordResetToken;
import com.edgareldy.springgraphqltutorial.entity.Role;
import com.edgareldy.springgraphqltutorial.entity.User;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.AuthPayload;
import com.edgareldy.springgraphqltutorial.graphql.input.CreateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.LoginInput;
import com.edgareldy.springgraphqltutorial.graphql.input.RegisterInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UserPage;
import com.edgareldy.springgraphqltutorial.repository.ActivationTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.PasswordResetTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import com.edgareldy.springgraphqltutorial.security.JwtService;
import com.edgareldy.springgraphqltutorial.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication flows and user administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Service
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final ActivationTokenRepository activationTokenRepository;
	private final BlacklistedTokenRepository blacklistedTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
			ActivationTokenRepository activationTokenRepository,
			BlacklistedTokenRepository blacklistedTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.activationTokenRepository = activationTokenRepository;
		this.blacklistedTokenRepository = blacklistedTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Override
	@Transactional
	public AuthPayload register(RegisterInput input) {
		if (userRepository.existsByEmail(input.email())) {
			throw new BusinessRuleException("Email already in use: " + input.email());
		}
		User user = User.builder()
				.firstName(input.firstName())
				.lastName(input.lastName())
				.email(input.email())
				.password(passwordEncoder.encode(input.password()))
				.enabled(false)
				.accountLocked(false)
				.build();
		user = userRepository.save(user);

		ActivationToken activationToken = ActivationToken.builder()
				.user(user)
				.token(UUID.randomUUID().toString())
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(24))
				.build();
		activationTokenRepository.save(activationToken);

		return new AuthPayload(null, user);
	}

	@Override
	@Transactional
	public boolean activateAccount(String token) {
		ActivationToken activationToken = activationTokenRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Activation token not found: " + token));
		if (activationToken.getValidatedAt() != null) {
			throw new BusinessRuleException("Activation token has already been used");
		}
		if (activationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BusinessRuleException("Activation token has expired");
		}
		User user = activationToken.getUser();
		user.setEnabled(true);
		userRepository.save(user);
		activationToken.setValidatedAt(LocalDateTime.now());
		activationTokenRepository.save(activationToken);
		return true;
	}

	@Override
	public AuthPayload login(LoginInput input) {
		User user = userRepository.findByEmail(input.email())
				.orElseThrow(() -> new BusinessRuleException("Invalid email or password"));
		if (!passwordEncoder.matches(input.password(), user.getPassword())) {
			throw new BusinessRuleException("Invalid email or password");
		}
		if (!user.isEnabled()) {
			throw new BusinessRuleException("Account is not activated");
		}
		if (user.isAccountLocked()) {
			throw new BusinessRuleException("Account is locked");
		}
		String token = jwtService.generateToken(user);
		return new AuthPayload(token, user);
	}

	@Override
	@Transactional
	public boolean logout(String rawToken) {
		String jti = jwtService.extractJti(rawToken);
		if (blacklistedTokenRepository.existsByJti(jti)) {
			return true;
		}
		User user = findByEmail(jwtService.extractEmail(rawToken));
		BlacklistedToken blacklistedToken = BlacklistedToken.builder()
				.user(user)
				.token(rawToken)
				.jti(jti)
				.blacklistedAt(LocalDateTime.now())
				.createdAt(jwtService.extractIssuedAt(rawToken))
				.expiresAt(jwtService.extractExpiration(rawToken))
				.build();
		blacklistedTokenRepository.save(blacklistedToken);
		return true;
	}

	@Override
	@Transactional
	public boolean requestPasswordReset(String email) {
		return userRepository.findByEmail(email).map(user -> {
			PasswordResetToken resetToken = PasswordResetToken.builder()
					.user(user)
					.token(UUID.randomUUID().toString())
					.type("PASSWORD_RESET")
					.expiryDate(LocalDateTime.now().plusHours(1))
					.build();
			passwordResetTokenRepository.save(resetToken);
			return true;
		}).orElse(false);
	}

	@Override
	@Transactional
	public boolean resetPassword(String token, String newPassword) {
		PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Password reset token not found: " + token));
		if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
			throw new BusinessRuleException("Password reset token has expired");
		}
		User user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		passwordResetTokenRepository.delete(resetToken);
		return true;
	}

	@Override
	public User findByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
	}

	@Override
	public UserPage findAll(int page, int size) {
		Page<User> result = userRepository.findAll(PageRequest.of(page, size));
		return new UserPage(result.getContent(), result.getTotalElements(), result.getTotalPages(), page, size);
	}

	@Override
	public User findById(Long id) {
		return getUserOrThrow(id);
	}

	@Override
	@Transactional
	public User create(CreateUserInput input) {
		if (userRepository.existsByEmail(input.email())) {
			throw new BusinessRuleException("Email already in use: " + input.email());
		}
		User user = User.builder()
				.firstName(input.firstName())
				.lastName(input.lastName())
				.email(input.email())
				.password(passwordEncoder.encode(input.password()))
				.enabled(true)
				.accountLocked(false)
				.build();
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public User update(Long id, UpdateUserInput input) {
		User user = getUserOrThrow(id);
		user.setFirstName(input.firstName());
		user.setLastName(input.lastName());
		user.setEmail(input.email());
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public User lock(Long id) {
		User user = getUserOrThrow(id);
		user.setAccountLocked(true);
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public User unlock(Long id) {
		User user = getUserOrThrow(id);
		user.setAccountLocked(false);
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		userRepository.delete(getUserOrThrow(id));
	}

	@Override
	@Transactional
	public User assignRoleToUser(Long userId, Long roleId) {
		User user = getUserOrThrow(userId);
		Role role = getRoleOrThrow(roleId);
		user.getRoles().add(role);
		return userRepository.save(user);
	}

	@Override
	@Transactional
	public User removeRoleFromUser(Long userId, Long roleId) {
		User user = getUserOrThrow(userId);
		Role role = getRoleOrThrow(roleId);
		user.getRoles().remove(role);
		return userRepository.save(user);
	}

	private User getUserOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	private Role getRoleOrThrow(Long id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
	}

}
