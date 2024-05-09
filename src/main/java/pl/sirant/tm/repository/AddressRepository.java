package pl.sirant.tm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import pl.sirant.tm.Address;

public interface AddressRepository {

  Slice<Address> findAll(String query, Pageable pageable);
}
