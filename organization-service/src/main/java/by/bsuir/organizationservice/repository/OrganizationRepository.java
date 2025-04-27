package by.bsuir.organizationservice.repository;

import by.bsuir.organizationservice.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    Organization findByInn(String inn);
}
