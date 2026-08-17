package com.chococo.backend.repository;

import com.chococo.backend.entity.CoffeeBean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {
}
