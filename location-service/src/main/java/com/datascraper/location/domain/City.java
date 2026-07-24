/**
 * Domain model for a city belonging to a country.
 */
package com.datascraper.location.domain;

/**
 * City belonging to a country. Stable id is used by Company Service later.
 */
public record City(String id, String name, String countryCode) {
}
