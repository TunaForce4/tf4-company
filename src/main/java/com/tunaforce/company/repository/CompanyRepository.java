package com.tunaforce.company.repository;

import com.tunaforce.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    @Query(value = "SELECT * FROM p_company c WHERE " +
            "(:name IS NULL OR LOWER(c.company_name) LIKE LOWER('%' || :name || '%')) " +
            "AND (:hubId IS NULL OR c.hub_id = CAST(:hubId AS UUID)) " +
            "AND c.deleted_at IS NULL",
            nativeQuery = true)
    List<Company> search(String name, UUID hubId);

    // Soft-delete 고려: deletedAt IS NULL
    List<Company> findByCompanyIdInAndDeletedAtIsNull(List<UUID> companyIds);

    Optional<Company> findFirstByUserIdAndDeletedAtIsNull(UUID userId);
}
