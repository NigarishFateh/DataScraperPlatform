/**
 * Domain model for a country reference row.
 */
package com.datascraper.location.domain;

/**
 * Country reference row (ISO 3166-1 alpha-2 code).
 */
public record Country(String code, String name) {
}
