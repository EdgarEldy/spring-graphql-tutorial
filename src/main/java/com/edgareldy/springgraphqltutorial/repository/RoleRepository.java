package com.edgareldy.springgraphqltutorial.repository;

import java.util.List;
import java.util.Optional;

import com.edgareldy.springgraphqltutorial.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for Role.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByRoleName(String roleName);

	boolean existsByRoleName(String roleName);

	@Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id IN :ids")
	List<Role> findAllWithPermissionsByIdIn(@Param("ids") Iterable<Long> ids);

}
