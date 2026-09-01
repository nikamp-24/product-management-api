package com.pooja.productmanagement.repository;

import com.pooja.productmanagement.entity.Role;
import com.pooja.productmanagement.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

}
