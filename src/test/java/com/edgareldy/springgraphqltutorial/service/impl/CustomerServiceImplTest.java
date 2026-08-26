package com.edgareldy.springgraphqltutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Customer;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerInput;
import com.edgareldy.springgraphqltutorial.graphql.input.CustomerPage;
import com.edgareldy.springgraphqltutorial.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for CustomerServiceImpl: every public method's nominal path,
 * duplicate email rejected on create/update, with CustomerRepository mocked
 * and no Spring context.
 * <p>
 * Created by Edgar Muhamyangabo on 8/26/26
 * Author : Edgar Muhamyangabo
 * Date : 8/26/26
 * Project : spring-graphql-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

	@Mock
	private CustomerRepository customerRepository;

	private CustomerServiceImpl customerService;

	@BeforeEach
	void setUp() {
		customerService = new CustomerServiceImpl(customerRepository);
	}

	private Customer sampleCustomer(Long id) {
		return Customer.builder()
				.id(id)
				.firstName("Jane")
				.lastName("Doe")
				.telephone("555-0100")
				.email("jane.doe@example.com")
				.address("1 Main Street")
				.build();
	}

	private CustomerInput sampleInput() {
		return new CustomerInput("Jane", "Doe", "555-0100", "jane.doe@example.com", "1 Main Street");
	}

	@Test
	void findAllReturnsAPageBuiltFromTheRepositoryPage() {
		Customer customer = sampleCustomer(1L);
		when(customerRepository.findAll(PageRequest.of(0, 20)))
				.thenReturn(new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1));

		CustomerPage page = customerService.findAll(0, 20);

		assertThat(page.content()).containsExactly(customer);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.page()).isEqualTo(0);
		assertThat(page.size()).isEqualTo(20);
	}

	@Test
	void findByIdReturnsCustomer() {
		Customer customer = sampleCustomer(1L);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		assertThat(customerService.findById(1L)).isEqualTo(customer);
	}

	@Test
	void findByIdThrowsWhenCustomerMissing() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createSavesNewCustomer() {
		CustomerInput input = sampleInput();
		when(customerRepository.existsByEmail(input.email())).thenReturn(false);
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Customer created = customerService.create(input);

		assertThat(created.getFirstName()).isEqualTo("Jane");
		assertThat(created.getLastName()).isEqualTo("Doe");
		assertThat(created.getTelephone()).isEqualTo("555-0100");
		assertThat(created.getEmail()).isEqualTo("jane.doe@example.com");
		assertThat(created.getAddress()).isEqualTo("1 Main Street");
	}

	@Test
	void createRejectsDuplicateEmail() {
		CustomerInput input = sampleInput();
		when(customerRepository.existsByEmail(input.email())).thenReturn(true);

		assertThatThrownBy(() -> customerService.create(input)).isInstanceOf(BusinessRuleException.class);

		verify(customerRepository, never()).save(any(Customer.class));
	}

	@Test
	void updateChangesCustomerFields() {
		Customer customer = sampleCustomer(1L);
		CustomerInput input = new CustomerInput("Janet", "Doe", "555-0199", "janet.doe@example.com", "2 Elm Street");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.existsByEmail(input.email())).thenReturn(false);
		when(customerRepository.save(customer)).thenReturn(customer);

		Customer updated = customerService.update(1L, input);

		assertThat(updated.getFirstName()).isEqualTo("Janet");
		assertThat(updated.getTelephone()).isEqualTo("555-0199");
		assertThat(updated.getEmail()).isEqualTo("janet.doe@example.com");
		assertThat(updated.getAddress()).isEqualTo("2 Elm Street");
	}

	@Test
	void updateThrowsWhenCustomerMissing() {
		CustomerInput input = sampleInput();
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.update(99L, input)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateRejectsEmailAlreadyUsedByAnotherCustomer() {
		Customer customer = sampleCustomer(1L);
		CustomerInput input = new CustomerInput("Jane", "Doe", "555-0100", "taken@example.com", "1 Main Street");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

		assertThatThrownBy(() -> customerService.update(1L, input)).isInstanceOf(BusinessRuleException.class);

		verify(customerRepository, never()).save(any(Customer.class));
	}

	@Test
	void updateAllowsKeepingTheSameEmailUnchanged() {
		Customer customer = sampleCustomer(1L);
		CustomerInput input = sampleInput();
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.save(customer)).thenReturn(customer);

		Customer updated = customerService.update(1L, input);

		assertThat(updated.getEmail()).isEqualTo("jane.doe@example.com");
		verify(customerRepository, never()).existsByEmail(any());
	}

	@Test
	void deleteRemovesCustomer() {
		Customer customer = sampleCustomer(1L);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		customerService.delete(1L);

		verify(customerRepository).delete(customer);
	}

	@Test
	void deleteThrowsWhenCustomerMissing() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);

		verify(customerRepository, never()).delete(any(Customer.class));
	}

}
