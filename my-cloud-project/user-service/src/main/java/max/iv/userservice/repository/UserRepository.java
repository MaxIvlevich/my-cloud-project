package max.iv.userservice.repository;
import max.iv.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository  extends JpaRepository<User,Long> {
    List<User> findByCompanyId(Long companyId);
    List<User> findByIdIn(List<Long> ids);
}
