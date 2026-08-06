-- Remove legacy uppercase NL-* city ids when lowercase catalog ids already exist.
DELETE FROM cities c
WHERE c.country_code = 'NL'
  AND c.id ~ '^[A-Z]{2}-'
  AND EXISTS (
      SELECT 1 FROM cities x
      WHERE x.country_code = 'NL' AND lower(x.id) = lower(c.id) AND x.id <> c.id
  );
