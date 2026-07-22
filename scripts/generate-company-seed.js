const fs = require("fs");
const path = require("path");

/** @type {[string, string, string, string, string, string, string[]][]} */
const companies = [
  ["co-zalando", "Zalando", "https://www.zalando.com", "E-commerce", "DE-berlin", "DE", ["software-dev", "saas"]],
  ["co-deliveryhero", "Delivery Hero", "https://www.deliveryhero.com", "FoodTech", "DE-berlin", "DE", ["mobile", "saas"]],
  ["co-hellofresh", "HelloFresh", "https://www.hellofresh.com", "FoodTech", "DE-berlin", "DE", ["saas", "data-eng"]],
  ["co-n26", "N26", "https://n26.com", "FinTech", "DE-berlin", "DE", ["fintech", "mobile"]],
  ["co-soundcloud", "SoundCloud", "https://soundcloud.com", "Media", "DE-berlin", "DE", ["software-dev", "mobile"]],
  ["co-contentful", "Contentful", "https://www.contentful.com", "SaaS", "DE-berlin", "DE", ["saas", "api"]],
  ["co-omio", "Omio", "https://www.omio.com", "Travel", "DE-berlin", "DE", ["software-dev", "mobile"]],
  ["co-adjust", "Adjust", "https://www.adjust.com", "AdTech", "DE-berlin", "DE", ["data-eng", "mobile"]],
  ["co-auto1", "AUTO1 Group", "https://www.auto1-group.com", "Automotive", "DE-berlin", "DE", ["software-dev", "saas"]],
  ["co-traderepublic", "Trade Republic", "https://traderepublic.com", "FinTech", "DE-berlin", "DE", ["fintech", "mobile"]],
  ["co-celonis", "Celonis", "https://www.celonis.com", "SaaS", "DE-munich", "DE", ["saas", "data-eng", "ai"]],
  ["co-personio", "Personio", "https://www.personio.com", "HR Tech", "DE-munich", "DE", ["saas", "cloud"]],
  ["co-siemens", "Siemens", "https://www.siemens.com", "Industrial", "DE-munich", "DE", ["software-dev", "automation"]],
  ["co-freeletics", "Freeletics", "https://www.freeletics.com", "HealthTech", "DE-munich", "DE", ["mobile", "healthtech"]],
  ["co-check24", "CHECK24", "https://www.check24.de", "Comparison", "DE-munich", "DE", ["web-dev", "saas"]],
  ["co-flixbus", "Flix", "https://www.flixbus.com", "Mobility", "DE-munich", "DE", ["software-dev", "mobile"]],
  ["co-jimdo", "Jimdo", "https://www.jimdo.com", "SaaS", "DE-hamburg", "DE", ["saas", "web-dev"]],
  ["co-innogames", "InnoGames", "https://www.innogames.com", "Gaming", "DE-hamburg", "DE", ["software-dev", "mobile"]],
  ["co-statista", "Statista", "https://www.statista.com", "Data", "DE-hamburg", "DE", ["data-eng", "saas"]],
  ["co-aboutyou", "ABOUT YOU", "https://www.aboutyou.com", "E-commerce", "DE-hamburg", "DE", ["software-dev", "saas"]],
  ["co-newwork", "New Work SE", "https://www.new-work.se", "HR Tech", "DE-hamburg", "DE", ["saas", "recruitment"]],
  ["co-adesso", "Adesso SE", "https://www.adesso.de", "Consulting", "DE-frankfurt", "DE", ["consulting", "software-dev"]],
  ["co-deutscheboerse", "Deutsche Börse", "https://www.deutsche-boerse.com", "FinTech", "DE-frankfurt", "DE", ["fintech", "data-eng"]],
  ["co-teamviewer", "TeamViewer", "https://www.teamviewer.com", "SaaS", "DE-stuttgart", "DE", ["saas", "cloud"]],
  ["co-sap", "SAP", "https://www.sap.com", "Enterprise Software", "DE-stuttgart", "DE", ["erp", "saas", "cloud"]],
  ["co-bosch", "Bosch", "https://www.bosch.com", "Industrial", "DE-stuttgart", "DE", ["automation", "software-dev"]],
  ["co-trivago", "trivago", "https://www.trivago.com", "Travel", "DE-düsseldorf", "DE", ["software-dev", "data-eng"]],
  ["co-rewe-digital", "REWE digital", "https://www.rewe-digital.com", "Retail Tech", "DE-cologne", "DE", ["software-dev", "cloud"]],
  ["co-adyen", "Adyen", "https://www.adyen.com", "Payments", "NL-amsterdam", "NL", ["fintech", "api"]],
  ["co-booking", "Booking.com", "https://www.booking.com", "Travel", "NL-amsterdam", "NL", ["software-dev", "cloud"]],
  ["co-mollie", "Mollie", "https://www.mollie.com", "Payments", "NL-amsterdam", "NL", ["fintech", "api"]],
  ["co-tomtom", "TomTom", "https://www.tomtom.com", "Navigation", "NL-amsterdam", "NL", ["software-dev", "mobile"]],
  ["co-backbase", "Backbase", "https://www.backbase.com", "FinTech", "NL-amsterdam", "NL", ["fintech", "saas"]],
  ["co-bird", "Bird", "https://bird.com", "Communications", "NL-amsterdam", "NL", ["api", "saas"]],
  ["co-picnic", "Picnic", "https://www.picnic.app", "Retail", "NL-amsterdam", "NL", ["software-dev", "data-eng"]],
  ["co-coolblue", "Coolblue", "https://www.coolblue.nl", "E-commerce", "NL-rotterdam", "NL", ["software-dev", "cloud"]],
  ["co-rabobank", "Rabobank", "https://www.rabobank.com", "Banking", "NL-utrecht", "NL", ["fintech", "cloud"]],
  ["co-asml", "ASML", "https://www.asml.com", "Semiconductor", "NL-eindhoven", "NL", ["software-dev", "automation"]],
  ["co-philips", "Philips", "https://www.philips.com", "HealthTech", "NL-eindhoven", "NL", ["healthtech", "software-dev"]],
  ["co-kpn", "KPN", "https://www.kpn.com", "Telecom", "NL-the-hague", "NL", ["cloud", "devops"]],
  ["co-imec", "imec", "https://www.imec-int.com", "R&D", "BE-leuven", "BE", ["ai", "software-dev"]],
  ["co-odoo", "Odoo", "https://www.odoo.com", "ERP", "BE-brussels", "BE", ["erp", "saas"]],
  ["co-collibra", "Collibra", "https://www.collibra.com", "Data", "BE-brussels", "BE", ["data-eng", "saas"]],
  ["co-cegeka", "Cegeka", "https://www.cegeka.com", "Consulting", "BE-antwerp", "BE", ["consulting", "cloud"]],
  ["co-materialise", "Materialise", "https://www.materialise.com", "3D Printing", "BE-leuven", "BE", ["software-dev", "healthtech"]],
  ["co-capgemini", "Capgemini", "https://www.capgemini.com", "Consulting", "FR-paris", "FR", ["consulting", "cloud"]],
  ["co-dassault", "Dassault Systèmes", "https://www.3ds.com", "Software", "FR-paris", "FR", ["software-dev", "saas"]],
  ["co-criteo", "Criteo", "https://www.criteo.com", "AdTech", "FR-paris", "FR", ["data-eng", "ai"]],
  ["co-blablacar", "BlaBlaCar", "https://www.blablacar.com", "Mobility", "FR-paris", "FR", ["mobile", "software-dev"]],
  ["co-ledger", "Ledger", "https://www.ledger.com", "Crypto", "FR-paris", "FR", ["blockchain", "fintech"]],
  ["co-atos", "Atos", "https://atos.net", "Consulting", "FR-paris", "FR", ["consulting", "cloud"]],
  ["co-ovhcloud", "OVHcloud", "https://www.ovhcloud.com", "Cloud", "FR-lille", "FR", ["cloud", "devops"]],
  ["co-sopra", "Sopra Steria", "https://www.soprasteria.com", "Consulting", "FR-lyon", "FR", ["consulting", "digital-xform"]],
  ["co-worldline", "Worldline", "https://worldline.com", "Payments", "FR-marseille", "FR", ["fintech", "api"]],
  ["co-airbus", "Airbus", "https://www.airbus.com", "Aerospace", "FR-toulouse", "FR", ["software-dev", "automation"]],
  ["co-telefonica", "Telefónica", "https://www.telefonica.com", "Telecom", "ES-madrid", "ES", ["cloud", "software-dev"]],
  ["co-cabify", "Cabify", "https://cabify.com", "Mobility", "ES-madrid", "ES", ["mobile", "software-dev"]],
  ["co-jobandtalent", "Jobandtalent", "https://jobandtalent.com", "HR Tech", "ES-madrid", "ES", ["recruitment", "saas"]],
  ["co-glovo", "Glovo", "https://glovoapp.com", "Delivery", "ES-barcelona", "ES", ["mobile", "software-dev"]],
  ["co-typeform", "Typeform", "https://www.typeform.com", "SaaS", "ES-barcelona", "ES", ["saas", "api"]],
  ["co-wallapop", "Wallapop", "https://www.wallapop.com", "Marketplace", "ES-barcelona", "ES", ["mobile", "software-dev"]],
  ["co-indra", "Indra", "https://www.indracompany.com", "Consulting", "ES-madrid", "ES", ["consulting", "software-dev"]],
  ["co-bbva", "BBVA", "https://www.bbva.com", "Banking", "ES-bilbao", "ES", ["fintech", "cloud"]],
  ["co-reply", "Reply", "https://www.reply.com", "Consulting", "IT-turin", "IT", ["consulting", "ai"]],
  ["co-bendingspoons", "Bending Spoons", "https://bendingspoons.com", "Apps", "IT-milan", "IT", ["mobile", "saas"]],
  ["co-teamsystem", "TeamSystem", "https://www.teamsystem.com", "ERP", "IT-milan", "IT", ["erp", "saas"]],
  ["co-eng", "Engineering", "https://www.eng.it", "Consulting", "IT-rome", "IT", ["consulting", "software-dev"]],
  ["co-sorint", "SORINT.lab", "https://www.sorint.it", "Consulting", "IT-bologna", "IT", ["devops", "cloud"]],
  ["co-farfetch", "Farfetch", "https://www.farfetch.com", "E-commerce", "PT-porto", "PT", ["software-dev", "saas"]],
  ["co-talkdesk", "Talkdesk", "https://www.talkdesk.com", "SaaS", "PT-lisbon", "PT", ["saas", "ai"]],
  ["co-outsystems", "OutSystems", "https://www.outsystems.com", "Low-code", "PT-lisbon", "PT", ["saas", "web-dev"]],
  ["co-feedzai", "Feedzai", "https://feedzai.com", "FinTech", "PT-coimbra", "PT", ["ai", "fintech"]],
  ["co-critical", "Critical Software", "https://www.criticalsoftware.com", "Software", "PT-coimbra", "PT", ["software-dev", "cyber"]],
  ["co-blip", "Blip", "https://blip.pt", "Consulting", "PT-braga", "PT", ["consulting", "cloud"]],
  ["co-opera", "Opera", "https://www.opera.com", "Browser", "NO-oslo", "NO", ["software-dev", "mobile"]],
  ["co-kahoot", "Kahoot!", "https://kahoot.com", "EdTech", "NO-oslo", "NO", ["edtech", "saas"]],
  ["co-cognite", "Cognite", "https://www.cognite.com", "Industrial AI", "NO-oslo", "NO", ["ai", "data-eng"]],
  ["co-visma", "Visma", "https://www.visma.com", "SaaS", "NO-oslo", "NO", ["saas", "erp"]],
  ["co-tietoevry", "Tietoevry", "https://www.tietoevry.com", "Consulting", "NO-bergen", "NO", ["consulting", "cloud"]],
  ["co-spotify", "Spotify", "https://www.spotify.com", "Media", "SE-stockholm", "SE", ["software-dev", "data-eng"]],
  ["co-klarna", "Klarna", "https://www.klarna.com", "FinTech", "SE-stockholm", "SE", ["fintech", "api"]],
  ["co-ericsson", "Ericsson", "https://www.ericsson.com", "Telecom", "SE-stockholm", "SE", ["software-dev", "cloud"]],
  ["co-truecaller", "Truecaller", "https://www.truecaller.com", "Communications", "SE-stockholm", "SE", ["mobile", "ai"]],
  ["co-volvo", "Volvo Cars", "https://www.volvocars.com", "Automotive", "SE-gothenburg", "SE", ["software-dev", "automation"]],
  ["co-mentimeter", "Mentimeter", "https://www.mentimeter.com", "SaaS", "SE-stockholm", "SE", ["saas", "web-dev"]],
  ["co-maersk", "Maersk", "https://www.maersk.com", "Logistics", "DK-copenhagen", "DK", ["software-dev", "cloud"]],
  ["co-unity", "Unity Technologies", "https://unity.com", "Gaming", "DK-copenhagen", "DK", ["software-dev", "mobile"]],
  ["co-trustpilot", "Trustpilot", "https://www.trustpilot.com", "SaaS", "DK-copenhagen", "DK", ["saas", "api"]],
  ["co-vestas", "Vestas", "https://www.vestas.com", "Energy", "DK-aarhus", "DK", ["software-dev", "data-eng"]],
  ["co-nokia", "Nokia", "https://www.nokia.com", "Telecom", "FI-espoo", "FI", ["software-dev", "cloud"]],
  ["co-supercell", "Supercell", "https://supercell.com", "Gaming", "FI-helsinki", "FI", ["mobile", "software-dev"]],
  ["co-wolt", "Wolt", "https://wolt.com", "Delivery", "FI-helsinki", "FI", ["mobile", "software-dev"]],
  ["co-relex", "RELEX Solutions", "https://www.relexsolutions.com", "Retail Tech", "FI-helsinki", "FI", ["ai", "saas"]],
  ["co-qt", "Qt Group", "https://www.qt.io", "Software", "FI-oulu", "FI", ["software-dev", "mobile"]],
  ["co-stripe-ie", "Stripe", "https://stripe.com", "Payments", "IE-dublin", "IE", ["fintech", "api"]],
  ["co-intercom", "Intercom", "https://www.intercom.com", "SaaS", "IE-dublin", "IE", ["saas", "api"]],
  ["co-workday-ie", "Workday", "https://www.workday.com", "ERP", "IE-dublin", "IE", ["erp", "saas"]],
  ["co-version1", "Version 1", "https://www.version1.com", "Consulting", "IE-cork", "IE", ["consulting", "cloud"]],
  ["co-soti", "SOTI", "https://www.soti.net", "MDM", "IE-galway", "IE", ["mobile", "saas"]],
  ["co-allegro", "Allegro", "https://allegro.pl", "E-commerce", "PL-poznań", "PL", ["software-dev", "cloud"]],
  ["co-asseco", "Asseco Poland", "https://asseco.com", "Software", "PL-kraków", "PL", ["software-dev", "erp"]],
  ["co-cdprojekt", "CD Projekt", "https://www.cdprojekt.com", "Gaming", "PL-warsaw", "PL", ["software-dev", "mobile"]],
  ["co-comarch", "Comarch", "https://www.comarch.com", "ERP", "PL-kraków", "PL", ["erp", "saas"]],
  ["co-netguru", "Netguru", "https://www.netguru.com", "Consulting", "PL-poznań", "PL", ["web-dev", "consulting"]],
  ["co-text", "Text", "https://text.com", "SaaS", "PL-wrocław", "PL", ["saas", "api"]],
  ["co-intive", "intive", "https://www.intive.com", "Consulting", "PL-gdańsk", "PL", ["consulting", "software-dev"]],
  ["co-bitpanda", "Bitpanda", "https://www.bitpanda.com", "Crypto", "AT-vienna", "AT", ["fintech", "blockchain"]],
  ["co-tricentis", "Tricentis", "https://www.tricentis.com", "Testing", "AT-vienna", "AT", ["devops", "saas"]],
  ["co-raiffeisen", "Raiffeisen Bank International", "https://www.rbinternational.com", "Banking", "AT-vienna", "AT", ["fintech", "cloud"]],
  ["co-ams", "ams OSRAM", "https://ams.com", "Semiconductor", "AT-graz", "AT", ["software-dev", "automation"]],
  ["co-abb", "ABB", "https://global.abb", "Industrial", "CH-zurich", "CH", ["automation", "software-dev"]],
  ["co-logitech", "Logitech", "https://www.logitech.com", "Hardware", "CH-lausanne", "CH", ["software-dev", "mobile"]],
  ["co-swisscom", "Swisscom", "https://www.swisscom.ch", "Telecom", "CH-bern", "CH", ["cloud", "devops"]],
  ["co-sonarsource", "SonarSource", "https://www.sonarsource.com", "DevTools", "CH-geneva", "CH", ["devops", "saas"]],
  ["co-temenos", "Temenos", "https://www.temenos.com", "FinTech", "CH-geneva", "CH", ["fintech", "saas"]],
  ["co-roche", "Roche", "https://www.roche.com", "Pharma", "CH-basel", "CH", ["healthtech", "data-eng"]],
];

function esc(value) {
  return value.replace(/'/g, "''");
}

let sql = "-- Real European IT companies (replaces runtime demo seeder)\n";
sql += "INSERT INTO companies (id, name, website, industry, city_id, country_code) VALUES\n";
sql += companies
  .map(
    ([id, name, website, industry, cityId, countryCode]) =>
      `  ('${id}', '${esc(name)}', '${esc(website)}', '${esc(industry)}', '${cityId}', '${countryCode}')`,
  )
  .join(",\n");
sql += ";\n\nINSERT INTO company_categories (company_id, category_id) VALUES\n";

const categoryRows = [];
for (const [id, , , , , , categories] of companies) {
  for (const categoryId of categories) {
    categoryRows.push(`  ('${id}', '${categoryId}')`);
  }
}
sql += categoryRows.join(",\n") + ";\n";

const outputPath = path.join(
  __dirname,
  "..",
  "company-service",
  "src",
  "main",
  "resources",
  "db",
  "migration",
  "V4__seed_companies.sql",
);
fs.writeFileSync(outputPath, sql, "utf8");
console.log(`Wrote ${companies.length} companies to ${outputPath}`);
