package com.edgareldy.springgraphqltutorial.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateProfileInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UpdateUserRolesInput;
import com.edgareldy.springgraphqltutorial.graphql.input.UserPage;
import com.edgareldy.springgraphqltutorial.repository.ActivationTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.BlacklistedTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.PasswordResetTokenRepository;
import com.edgareldy.springgraphqltutorial.repository.RoleRepository;
import com.edgareldy.springgraphqltutorial.repository.UserRepository;
import com.edgareldy.springgraphqltutorial.security.JwtService;
import com.edgareldy.springgraphqltutorial.service.EmailService;
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
	private final EmailService emailService;

	public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
			ActivationTokenRepository activationTokenRepository,
			BlacklistedTokenRepository blacklistedTokenRepository,
			PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService, EmailService emailService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.activationTokenRepository = activationTokenRepository;
		this.blacklistedTokenRepository = blacklistedTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.emailService = emailService;
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
		emailService.sendActivationEmail(user.getEmail(), activationToken.getToken());

		// No token: the account is disabled until activateAccount runs, so a JWT
		// issued here would authenticate an account that cannot use it yet.
		return new AuthPayload(null, null, null, 0L, user);
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
	@Transactional(readOnly = true)
	public AuthPayload login(LoginInput input) {
		// Same message whether the email is unknown or the password is wrong, so
		// a caller can never use this mutation to probe which emails are registered.
		User user = userRepository.findByEmailWithRolesAndPermissions(input.email())
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
		String accessToken = jwtService.generateToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		return new AuthPayload(accessToken, "Bearer", refreshToken, jwtService.getExpirationMs(), user);
	}

	@Override
	@Transactional
	public boolean logout(String rawToken) {
		String jti = jwtService.extractJti(rawToken);
		// Already blacklisted: treat a repeat logout call as a success rather than
		// failing on the jti unique constraint by inserting it a second time.
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
		// Returns false rather than a NOT_FOUND error for an unknown email, so this
		// mutation cannot be used to probe which addresses have an account here.
		return userRepository.findByEmail(email).map(user -> {
			PasswordResetToken resetToken = PasswordResetToken.builder()
					.user(user)
					.token(UUID.randomUUID().toString())
					.type("PASSWORD_RESET")
					.expiryDate(LocalDateTime.now().plusHours(1))
					.build();
			passwordResetTokenRepository.save(resetToken);
			emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
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
	public User updateRoles(Long userId, UpdateUserRolesInput input) {
		User user = getUserOrThrow(userId);

		List<Long> addIds = input.add() != null ? input.add() : List.of();
		List<Long> removeIds = input.remove() != null ? input.remove() : List.of();
		if (addIds.isEmpty() && removeIds.isEmpty()) {
			throw new BusinessRuleException("At least one role id must be specified in add or remove");
		}

		Set<Long> addSet = new HashSet<>(addIds);
		for (Long id : removeIds) {
			if (addSet.contains(id)) {
				throw new BusinessRuleException("Role id " + id + " appears in both add and remove");
			}
		}

		List<Long> allIds = new ArrayList<>(addIds);
		allIds.addAll(removeIds);
		List<Role> found = roleRepository.findAllById(allIds);
		if (found.size() != allIds.size()) {
			Set<Long> foundIds = found.stream().map(Role::getId).collect(Collectors.toSet());
			List<Long> unknown = allIds.stream().filter(id -> !foundIds.contains(id)).toList();
			throw new ResourceNotFoundException("Role(s) not found: " + unknown);
		}

		Map<Long, Role> roleMap = found.stream().collect(Collectors.toMap(Role::getId, r -> r));
		addIds.forEach(id -> user.getRoles().add(roleMap.get(id)));
		removeIds.forEach(id -> user.getRoles().remove(roleMap.get(id)));
		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthPayload refreshToken(String rawRefreshToken) {
		if (!jwtService.isRefreshToken(rawRefreshToken)) {
			throw new BusinessRuleException("Invalid refresh token");
		}
		String jti = jwtService.extractJti(rawRefreshToken);
		if (blacklistedTokenRepository.existsByJti(jti)) {
			throw new BusinessRuleException("Refresh token has been revoked");
		}
		String email = jwtService.extractEmail(rawRefreshToken);
		User user = userRepository.findByEmailWithRolesAndPermissions(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
		if (!user.isEnabled()) {
			throw new BusinessRuleException("Account is not activated");
		}
		if (user.isAccountLocked()) {
			throw new BusinessRuleException("Account is locked");
		}
		String newAccessToken = jwtService.generateToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user);
		return new AuthPayload(newAccessToken, "Bearer", newRefreshToken, jwtService.getExpirationMs(), user);
	}

	@Override
	@Transactional
	public User updateProfile(String email, UpdateProfileInput input) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
		if (input.firstName() != null) {
			user.setFirstName(input.firstName());
		}
		if (input.lastName() != null) {
			user.setLastName(input.lastName());
		}
		if (input.email() != null && !input.email().equals(email)) {
			if (userRepository.existsByEmail(input.email())) {
				throw new BusinessRuleException("Email already in use: " + input.email());
			}
			user.setEmail(input.email());
		}
		return userRepository.save(user);
	}

	private User getUserOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

}
