package com.tunaforce.company.repository;

import com.tunaforce.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    @Query("SELECT c FROM Company c WHERE (:name IS NULL OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
	    "AND (:hubId IS NULL OR c.hubId = :hubId)")
    List<Company> search(String name, UUID hubId);
}
