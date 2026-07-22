import type { Country } from "../../types/catalog";

type CountrySelectProps = {
  countries: Country[];
  value: string | null;
  loading?: boolean;
  onChange: (countryCode: string | null) => void;
};

export function CountrySelect({ countries, value, loading, onChange }: CountrySelectProps) {
  return (
    <label className="block">
      <span className="sr-only">Country</span>
      <select
        className="w-full rounded-lg border border-white/10 bg-ink-900/80 px-3 py-2.5 text-sm text-mist-100 outline-none transition focus:border-signal/50"
        value={value ?? ""}
        disabled={loading}
        onChange={(event) => onChange(event.target.value || null)}
      >
        <option value="">{loading ? "Loading countries…" : "Select country"}</option>
        {countries.map((country) => (
          <option key={country.code} value={country.code}>
            {country.name}
          </option>
        ))}
      </select>
    </label>
  );
}
