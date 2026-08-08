package com.pizzashop.repository;

import com.pizzashop.entity.AdminAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAccessRepository extends JpaRepository<AdminAccess, String> {

    List<AdminAccess> findAllByOrderByEmailAsc();
}
