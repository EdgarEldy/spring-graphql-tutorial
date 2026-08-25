package com.edgareldy.springgraphqltutorial.service.impl;

import java.util.List;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import com.edgareldy.springgraphqltutorial.exception.BusinessRuleException;
import com.edgareldy.springgraphqltutorial.exception.ResourceNotFoundException;
import com.edgareldy.springgraphqltutorial.graphql.input.PermissionInput;
import com.edgareldy.springgraphqltutorial.repository.PermissionRepository;
import com.edgareldy.springgraphqltutorial.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permission administration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
@Service
public class PermissionServiceImpl implements PermissionService {

	private final PermissionRepository permissionRepository;

	public PermissionServiceImpl(PermissionRepository permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	@Override
	public List<Permission> findAll() {
		return permissionRepository.findAll();
	}

	@Override
	@Transactional
	public Permission create(PermissionInput input) {
		if (permissionRepository.existsByResourceAndAction(input.resource(), input.action())) {
			throw new BusinessRuleException(
					"Permission already exists for resource " + input.resource() + " and action " + input.action());
		}
		return permissionRepository
				.save(Permission.builder().resource(input.resource()).action(input.action()).build());
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Permission permission = permissionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
		permissionRepository.delete(permission);
	}

}
