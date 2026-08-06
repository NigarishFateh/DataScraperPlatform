-- Prefer lowercase catalog city ids; drop legacy Uppercase-XX duplicates.
DELETE FROM cities c
WHERE c.id ~ '^[A-Z]{2}-'
  AND EXISTS (
      SELECT 1
      FROM cities x
      WHERE lower(x.id) = lower(c.id)
        AND x.id <> c.id
  );

-- Normalize common umlaut / spelling twins onto the lowercase ASCII id.
DELETE FROM cities c
WHERE c.id IN (
  'DE-düsseldorf',
  'SE-malmö',
  'PL-kraków',
  'PL-wrocław',
  'PL-gdańsk',
  'PL-poznań'
);
