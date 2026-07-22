package com.datascraper.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cities")
public class CityEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    protected CityEntity() {
    }

    public CityEntity(String id, String name, String countryCode) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
