package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for UserServiceImpl: every public method's nominal path plus
 * every business rule (duplicate email, wrong credentials, disabled/locked
 * account, expired or already used tokens) is exercised with a mocked
 * repository layer, no Spring context involved.
 * <p>
 * Created by Edgar Muhamyangabo on 8/25/26
 * Author : Edgar Muhamyangabo
 * Date : 8/25/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private ActivationTokenRepository activationTokenRepository;

	@Mock
	private BlacklistedTokenRepository blacklistedTokenRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userService = new UserServiceImpl(userRepository, roleRepository, activationTokenRepository,
				blacklistedTokenRepository, passwordResetTokenRepository, passwordEncoder, jwtService);
	}

	private User buildUser() {
		return User.builder()
				.id(1L)
				.firstName("Ada")
				.lastName("Lovelace")
				.email("ada@example.com")
				.password("encoded-password")
				.enabled(true)
				.accountLocked(false)
				.build();
	}

	@Test
	void _01_ShouldCreateDisabledUserAndActivationToken_WhenUserRegisters() {
		RegisterInput input = new RegisterInput("Ada", "Lovelace", "ada@example.com", "secret");
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuthPayload payload = userService.register(input);

		assertThat(payload.token()).isNull();
		assertThat(payload.user().getEmail()).isEqualTo("ada@example.com");
		assertThat(payload.user().isEnabled()).isFalse();
		verify(activationTokenRepository).save(any(ActivationToken.class));
	}

	@Test
	void _02_ShouldRejectRegistration_WhenEmailIsDuplicate() {
		RegisterInput input = new RegisterInput("Ada", "Lovelace", "ada@example.com", "secret");
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.register(input)).isInstanceOf(BusinessRuleException.class);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void _03_ShouldEnableUserAndValidateToken_WhenAccountIsActivated() {
		User user = buildUser();
		user.setEnabled(false);
		ActivationToken token = ActivationToken.builder()
				.id(10L)
				.user(user)
				.token("abc")
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(1))
				.build();
		when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

		boolean result = userService.activateAccount("abc");

		assertThat(result).isTrue();
		assertThat(user.isEnabled()).isTrue();
		assertThat(token.getValidatedAt()).isNotNull();
		verify(userRepository).save(user);
		verify(activationTokenRepository).save(token);
	}

	@Test
	void _04_ShouldRejectActivation_WhenTokenIsUnknown() {
		when(activationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.activateAccount("missing"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _05_ShouldRejectActivation_WhenTokenIsAlreadyUsed() {
		ActivationToken token = ActivationToken.builder()
				.id(10L)
				.user(buildUser())
				.token("abc")
				.createdAt(LocalDateTime.now())
				.expiresAt(LocalDateTime.now().plusHours(1))
				.validatedAt(LocalDateTime.now())
				.build();
		when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> userService.activateAccount("abc")).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _06_ShouldRejectActivation_WhenTokenIsExpired() {
		ActivationToken token = ActivationToken.builder()
				.id(10L)
				.user(buildUser())
				.token("abc")
				.createdAt(LocalDateTime.now().minusDays(2))
				.expiresAt(LocalDateTime.now().minusHours(1))
				.build();
		when(activationTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> userService.activateAccount("abc")).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _07_ShouldReturnToken_WhenCredentialsAreValid() {
		User user = buildUser();
		LoginInput input = new LoginInput("ada@example.com", "secret");
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-password")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");

		AuthPayload payload = userService.login(input);

		assertThat(payload.token()).isEqualTo("jwt-token");
		assertThat(payload.user()).isEqualTo(user);
	}

	@Test
	void _08_ShouldRejectLogin_WhenEmailIsUnknown() {
		LoginInput input = new LoginInput("missing@example.com", "secret");
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.login(input)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _09_ShouldRejectLogin_WhenPasswordIsWrong() {
		User user = buildUser();
		LoginInput input = new LoginInput("ada@example.com", "wrong");
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

		assertThatThrownBy(() -> userService.login(input)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _10_ShouldRejectLogin_WhenAccountIsDisabled() {
		User user = buildUser();
		user.setEnabled(false);
		LoginInput input = new LoginInput("ada@example.com", "secret");
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-password")).thenReturn(true);

		assertThatThrownBy(() -> userService.login(input)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _11_ShouldRejectLogin_WhenAccountIsLocked() {
		User user = buildUser();
		user.setAccountLocked(true);
		LoginInput input = new LoginInput("ada@example.com", "secret");
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret", "encoded-password")).thenReturn(true);

		assertThatThrownBy(() -> userService.login(input)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _12_ShouldBlacklistToken_WhenLogoutIsCalledFirstTime() {
		User user = buildUser();
		String rawToken = "raw-jwt";
		when(jwtService.extractJti(rawToken)).thenReturn("jti-1");
		when(blacklistedTokenRepository.existsByJti("jti-1")).thenReturn(false);
		when(jwtService.extractEmail(rawToken)).thenReturn("ada@example.com");
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		when(jwtService.extractIssuedAt(rawToken)).thenReturn(LocalDateTime.now());
		when(jwtService.extractExpiration(rawToken)).thenReturn(LocalDateTime.now().plusHours(1));

		boolean result = userService.logout(rawToken);

		assertThat(result).isTrue();
		ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
		verify(blacklistedTokenRepository).save(captor.capture());
		assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
		assertThat(captor.getValue().getUser()).isEqualTo(user);
	}

	@Test
	void _13_ShouldStayIdempotent_WhenTokenIsAlreadyBlacklisted() {
		String rawToken = "raw-jwt";
		when(jwtService.extractJti(rawToken)).thenReturn("jti-1");
		when(blacklistedTokenRepository.existsByJti("jti-1")).thenReturn(true);

		boolean result = userService.logout(rawToken);

		assertThat(result).isTrue();
		verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
	}

	@Test
	void _14_ShouldCreateResetToken_WhenUserExists() {
		User user = buildUser();
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		boolean result = userService.requestPasswordReset("ada@example.com");

		assertThat(result).isTrue();
		verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
	}

	@Test
	void _15_ShouldReturnFalse_WhenPasswordResetEmailIsUnknown() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		boolean result = userService.requestPasswordReset("missing@example.com");

		assertThat(result).isFalse();
		verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
	}

	@Test
	void _16_ShouldUpdatePasswordAndConsumeToken_WhenPasswordIsReset() {
		User user = buildUser();
		PasswordResetToken resetToken = PasswordResetToken.builder()
				.id(5L)
				.user(user)
				.token("reset-token")
				.type("PASSWORD_RESET")
				.expiryDate(LocalDateTime.now().plusHours(1))
				.build();
		when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));
		when(passwordEncoder.encode("new-secret")).thenReturn("new-encoded");

		boolean result = userService.resetPassword("reset-token", "new-secret");

		assertThat(result).isTrue();
		assertThat(user.getPassword()).isEqualTo("new-encoded");
		verify(userRepository).save(user);
		verify(passwordResetTokenRepository).delete(resetToken);
	}

	@Test
	void _17_ShouldRejectPasswordReset_WhenTokenIsUnknown() {
		when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.resetPassword("missing", "new-secret"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _18_ShouldRejectPasswordReset_WhenTokenIsExpired() {
		PasswordResetToken resetToken = PasswordResetToken.builder()
				.id(5L)
				.user(buildUser())
				.token("reset-token")
				.type("PASSWORD_RESET")
				.expiryDate(LocalDateTime.now().minusHours(1))
				.build();
		when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));

		assertThatThrownBy(() -> userService.resetPassword("reset-token", "new-secret"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void _19_ShouldReturnUser_WhenFindingByEmail() {
		User user = buildUser();
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		assertThat(userService.findByEmail("ada@example.com")).isEqualTo(user);
	}

	@Test
	void _20_ShouldThrowNotFound_WhenEmailIsMissing() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.findByEmail("missing@example.com"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _21_ShouldBuildUserPage_WhenSpringDataPageIsReturned() {
		User user = buildUser();
		Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
		when(userRepository.findAll(PageRequest.of(0, 20))).thenReturn(page);

		UserPage result = userService.findAll(0, 20);

		assertThat(result.content()).containsExactly(user);
		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.totalPages()).isEqualTo(1);
		assertThat(result.page()).isEqualTo(0);
		assertThat(result.size()).isEqualTo(20);
	}

	@Test
	void _22_ShouldReturnUser_WhenUserExists() {
		User user = buildUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThat(userService.findById(1L)).isEqualTo(user);
	}

	@Test
	void _23_ShouldThrowNotFound_WhenUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _24_ShouldEnableUserImmediately_WhenUserIsCreated() {
		CreateUserInput input = new CreateUserInput("Grace", "Hopper", "grace@example.com", "secret");
		when(userRepository.existsByEmail("grace@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret")).thenReturn("encoded");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User created = userService.create(input);

		assertThat(created.isEnabled()).isTrue();
		assertThat(created.isAccountLocked()).isFalse();
		assertThat(created.getEmail()).isEqualTo("grace@example.com");
	}

	@Test
	void _25_ShouldRejectCreation_WhenEmailIsDuplicate() {
		CreateUserInput input = new CreateUserInput("Grace", "Hopper", "grace@example.com", "secret");
		when(userRepository.existsByEmail("grace@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.create(input)).isInstanceOf(BusinessRuleException.class);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void _26_ShouldChangeProfileFields_WhenUserIsUpdated() {
		User user = buildUser();
		UpdateUserInput input = new UpdateUserInput("Augusta", "King", "augusta@example.com");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User updated = userService.update(1L, input);

		assertThat(updated.getFirstName()).isEqualTo("Augusta");
		assertThat(updated.getLastName()).isEqualTo("King");
		assertThat(updated.getEmail()).isEqualTo("augusta@example.com");
	}

	@Test
	void _27_ShouldThrowNotFound_WhenUpdatedUserIsMissing() {
		UpdateUserInput input = new UpdateUserInput("Augusta", "King", "augusta@example.com");
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.update(99L, input)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _28_ShouldSetAccountLocked_WhenUserIsLocked() {
		User user = buildUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User locked = userService.lock(1L);

		assertThat(locked.isAccountLocked()).isTrue();
	}

	@Test
	void _29_ShouldThrowNotFound_WhenLockedUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.lock(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _30_ShouldClearAccountLocked_WhenUserIsUnlocked() {
		User user = buildUser();
		user.setAccountLocked(true);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User unlocked = userService.unlock(1L);

		assertThat(unlocked.isAccountLocked()).isFalse();
	}

	@Test
	void _31_ShouldThrowNotFound_WhenUnlockedUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.unlock(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _32_ShouldRemoveUser_WhenUserExists() {
		User user = buildUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		userService.delete(1L);

		verify(userRepository).delete(user);
	}

	@Test
	void _33_ShouldThrowNotFound_WhenDeletedUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void _34_ShouldAddRoleToUser_WhenRoleIsAssigned() {
		User user = buildUser();
		Role role = Role.builder().id(2L).roleName("ADMIN").permissions(Set.of()).build();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
		when(userRepository.save(user)).thenReturn(user);

		User result = userService.assignRoleToUser(1L, 2L);

		assertThat(result.getRoles()).contains(role);
	}

	@Test
	void _35_ShouldThrowNotFound_WhenAssignmentUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.assignRoleToUser(99L, 2L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _36_ShouldThrowNotFound_WhenAssignmentRoleIsMissing() {
		User user = buildUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.assignRoleToUser(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _37_ShouldRemoveRoleFromUser_WhenRoleIsRemoved() {
		Role role = Role.builder().id(2L).roleName("ADMIN").permissions(Set.of()).build();
		User user = buildUser();
		user.getRoles().add(role);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
		when(userRepository.save(user)).thenReturn(user);

		User result = userService.removeRoleFromUser(1L, 2L);

		assertThat(result.getRoles()).doesNotContain(role);
	}

	@Test
	void _38_ShouldThrowNotFound_WhenRemovalUserIsMissing() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.removeRoleFromUser(99L, 2L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void _39_ShouldThrowNotFound_WhenRemovalRoleIsMissing() {
		User user = buildUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.removeRoleFromUser(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
