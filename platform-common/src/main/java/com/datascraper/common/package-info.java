/**
 * Shared library across Lead Intelligence microservices.
 * <p>
 * Put only stable contracts here: DTOs, enums, API error shapes.
 * Do NOT put business logic, JPA entities, or Spring configuration here —
 * that would create a distributed monolith ("common jar" anti-pattern).
 */
package com.datascraper.common;
