package com.edgareldy.springgraphqltutorial.repository;

import com.edgareldy.springgraphqltutorial.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Permission.
 * <p>
 * Created by Edgar Muhamyangabo on 8/24/26
 * Author : Edgar Muhamyangabo
 * Date : 8/24/26
 * Project : spring-graphql-tutorial
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

	boolean existsByResourceAndAction(String resource, String action);

}
