package com.chococo.backend.dto.pairing;

import com.chococo.backend.entity.CoffeeBean;

public record CoffeeBeanDto(Long id, String name, String roastLevel, String origin, String description) {

    public static CoffeeBeanDto from(CoffeeBean bean) {
        return new CoffeeBeanDto(
                bean.getId(),
                bean.getName(),
                bean.getRoastLevel().dbValue(),
                bean.getOrigin(),
                bean.getDescription());
    }
}
