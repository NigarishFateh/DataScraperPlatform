package com.datascraper.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "countries")
public class CountryEntity {

    @Id
    @Column(length = 2)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    protected CountryEntity() {
    }

    public CountryEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
