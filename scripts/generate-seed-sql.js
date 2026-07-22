const fs = require("fs");

const countries = [
  ["DE", "Germany"], ["NL", "Netherlands"], ["BE", "Belgium"], ["FR", "France"], ["ES", "Spain"],
  ["IT", "Italy"], ["PT", "Portugal"], ["NO", "Norway"], ["SE", "Sweden"], ["DK", "Denmark"],
  ["FI", "Finland"], ["IE", "Ireland"], ["PL", "Poland"], ["AT", "Austria"], ["CH", "Switzerland"],
];

const cities = {
  DE: ["Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf"],
  NL: ["Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven"],
  BE: ["Brussels", "Antwerp", "Ghent", "Leuven"],
  FR: ["Paris", "Lyon", "Marseille", "Toulouse", "Nantes", "Lille"],
  ES: ["Madrid", "Barcelona", "Valencia", "Seville", "Bilbao"],
  IT: ["Milan", "Rome", "Turin", "Bologna", "Florence"],
  PT: ["Lisbon", "Porto", "Braga", "Coimbra"],
  NO: ["Oslo", "Bergen", "Trondheim"],
  SE: ["Stockholm", "Gothenburg", "Malmö", "Uppsala"],
  DK: ["Copenhagen", "Aarhus", "Odense"],
  FI: ["Helsinki", "Espoo", "Tampere", "Oulu"],
  IE: ["Dublin", "Cork", "Galway", "Limerick"],
  PL: ["Warsaw", "Kraków", "Wrocław", "Gdańsk", "Poznań"],
  AT: ["Vienna", "Graz", "Linz", "Salzburg"],
  CH: ["Zurich", "Geneva", "Basel", "Bern", "Lausanne"],
};

function cityId(code, name) {
  return `${code}-${name.toLowerCase().replace(/ /g, "-")}`;
}

function esc(value) {
  return value.replace(/'/g, "''");
}

let locationSql = "INSERT INTO countries (code, name) VALUES\n";
locationSql += countries.map((c) => `  ('${c[0]}', '${esc(c[1])}')`).join(",\n") + ";\n\n";
locationSql += "INSERT INTO cities (id, name, country_code) VALUES\n";
const cityRows = [];
for (const [code, names] of Object.entries(cities)) {
  for (const name of names) {
    cityRows.push(`  ('${cityId(code, name)}', '${esc(name)}', '${code}')`);
  }
}
locationSql += cityRows.join(",\n") + ";\n";

fs.writeFileSync("location-service/src/main/resources/db/migration/V2__seed_locations.sql", locationSql);

const categories = [
  ["software-dev", "Software Development"], ["web-dev", "Web Development"], ["cloud", "Cloud"],
  ["devops", "DevOps"], ["cyber", "Cyber Security"], ["ai", "Artificial Intelligence"],
  ["ml", "Machine Learning"], ["data-eng", "Data Engineering"], ["erp", "ERP"], ["crm", "CRM"],
  ["fintech", "FinTech"], ["healthtech", "HealthTech"], ["edtech", "EdTech"],
  ["recruitment", "Recruitment"], ["consulting", "IT Consulting"],
  ["digital-xform", "Digital Transformation"], ["automation", "Automation"],
  ["blockchain", "Blockchain"], ["mobile", "Mobile Development"], ["saas", "SaaS"],
  ["api", "API Development"],
];

let categorySql = "INSERT INTO categories (id, name) VALUES\n";
categorySql += categories.map((c) => `  ('${c[0]}', '${esc(c[1])}')`).join(",\n") + ";\n";

fs.writeFileSync("category-service/src/main/resources/db/migration/V2__seed_categories.sql", categorySql);

console.log("Generated seed SQL files");
