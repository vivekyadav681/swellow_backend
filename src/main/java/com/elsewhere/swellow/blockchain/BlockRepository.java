package com.elsewhere.swellow.blockchain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Optional<Block> findByBlockIndex(Long blockIndex);
    Optional<Block> findFirstByOrderByBlockIndexDesc();
    List<Block> findAllByOrderByBlockIndexAsc();
}
