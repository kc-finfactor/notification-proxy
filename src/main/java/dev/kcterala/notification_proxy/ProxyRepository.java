package dev.kcterala.notification_proxy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProxyRepository extends JpaRepository<Proxy, Integer> {
    List<Proxy> findByTeam(final String team);
}
