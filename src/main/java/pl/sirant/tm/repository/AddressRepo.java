package pl.sirant.tm.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import pl.sirant.tm.Address;
import pl.sirant.tm.Address_;

@Repository
@RequiredArgsConstructor
class AddressRepo implements AddressRepository {

  private final JpaAddressRepository jpaAddressRepository;

  public Slice<Address> findAll(String query, Pageable pageable) {
    return jpaAddressRepository.findAll(new TextSearchableSpecification(query), pageable);
  }

  private record TextSearchableSpecification(String query) implements Specification<Address> {
    @Override
    public Predicate toPredicate(Root<Address> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
      if (this.query != null && !this.query.isEmpty()) {
        query.orderBy(SimilarityMatchUtil.similarityOrder(cb, this.query, root.get(Address_.street), root.get(Address_.city), root.get(Address_.note)));
      }
      return null;
    }
  }

}
