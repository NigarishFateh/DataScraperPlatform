INSERT INTO companies (id, name, website, industry, city_id, country_code) VALUES
  ('co-zalando', 'Zalando', 'https://www.zalando.com', 'E-commerce', 'DE-berlin', 'DE'),
  ('co-deliveryhero', 'Delivery Hero', 'https://www.deliveryhero.com', 'FoodTech', 'DE-berlin', 'DE'),
  ('co-hellofresh', 'HelloFresh', 'https://www.hellofresh.com', 'FoodTech', 'DE-berlin', 'DE'),
  ('co-n26', 'N26', 'https://n26.com', 'FinTech', 'DE-berlin', 'DE'),
  ('co-soundcloud', 'SoundCloud', 'https://soundcloud.com', 'Media', 'DE-berlin', 'DE'),
  ('co-contentful', 'Contentful', 'https://www.contentful.com', 'SaaS', 'DE-berlin', 'DE'),
  ('co-infosys', 'Infosys', 'https://www.infosys.com', 'IT Services', 'IN-bengaluru', 'IN'),
  ('co-tcs', 'Tata Consultancy Services', 'https://www.tcs.com', 'IT Services', 'IN-bengaluru', 'IN');

INSERT INTO company_categories (company_id, category_id) VALUES
  ('co-zalando', 'cat-ecommerce'),
  ('co-deliveryhero', 'cat-foodtech'),
  ('co-infosys', 'cat-software');
