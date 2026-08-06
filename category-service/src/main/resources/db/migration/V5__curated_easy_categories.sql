-- Keep only easy-to-scrape local-business categories; Cleaning Companies stays first in API sort.

DELETE FROM categories
WHERE id NOT IN (
  'cleaning',
  'dental',
  'dental-lab',
  'orthodontics',
  'restaurant',
  'cafe',
  'coffee-shop',
  'bakery',
  'hotel',
  'gym',
  'beauty-salon',
  'barbershop',
  'spa',
  'pharmacy',
  'veterinary',
  'physiotherapy',
  'hospital',
  'clinic',
  'plumbing',
  'hvac',
  'electrical-contractor',
  'roofing',
  'landscaping',
  'pest-control',
  'handyman',
  'locksmith',
  'auto-repair',
  'car-rental',
  'law-firm',
  'real-estate-agency',
  'florist',
  'laundry'
);

INSERT INTO categories (id, name)
SELECT v.id, v.name
FROM (VALUES
  ('cleaning', 'Cleaning Companies'),
  ('dental', 'Dental Clinics'),
  ('dental-lab', 'Dental Laboratories'),
  ('orthodontics', 'Orthodontics'),
  ('restaurant', 'Restaurants'),
  ('cafe', 'Cafes'),
  ('coffee-shop', 'Coffee Shops'),
  ('bakery', 'Bakeries'),
  ('hotel', 'Hotels'),
  ('gym', 'Gyms & Fitness Centers'),
  ('beauty-salon', 'Beauty Salons'),
  ('barbershop', 'Barbershops'),
  ('spa', 'Spas'),
  ('pharmacy', 'Pharmacy'),
  ('veterinary', 'Veterinary'),
  ('physiotherapy', 'Physiotherapy'),
  ('hospital', 'Hospitals'),
  ('clinic', 'Medical Clinics'),
  ('plumbing', 'Plumbing'),
  ('hvac', 'HVAC'),
  ('electrical-contractor', 'Electrical Contractors'),
  ('roofing', 'Roofing'),
  ('landscaping', 'Landscaping'),
  ('pest-control', 'Pest Control'),
  ('handyman', 'Handyman Services'),
  ('locksmith', 'Locksmiths'),
  ('auto-repair', 'Auto Repair'),
  ('car-rental', 'Car Rental'),
  ('law-firm', 'Law Firms'),
  ('real-estate-agency', 'Real Estate Agencies'),
  ('florist', 'Florists'),
  ('laundry', 'Laundry & Dry Cleaning')
) AS v(id, name)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.id = v.id);

UPDATE categories SET name = 'Cleaning Companies' WHERE id = 'cleaning';
