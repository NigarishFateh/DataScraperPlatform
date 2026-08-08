import type { LeadershipPerson } from "../../types/leadership";

/**
 * Builds a SpreadsheetML (.xls) workbook Excel opens natively — no extra npm dependency.
 */
export function downloadLeadershipExcel(
  rows: LeadershipPerson[],
  fileName = "leadership-results.xls",
): void {
  const header = ["Brand", "Leader", "Title", "Source", "Website", "Ticker", "Found"];
  const body = rows.map((row) => [
    row.companyName ?? "",
    row.found ? row.leaderName ?? "" : "",
    row.found ? row.leadershipTitle ?? "" : "",
    row.source ?? "",
    row.website ?? "",
    row.ticker ?? "",
    row.found ? "Yes" : "No",
  ]);

  const sheetRows = [header, ...body]
    .map(
      (cells) =>
        `<Row>${cells
          .map((cell) => `<Cell><Data ss:Type="String">${escapeXml(String(cell))}</Data></Cell>`)
          .join("")}</Row>`,
    )
    .join("");

  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Worksheet ss:Name="Leadership">
  <Table>${sheetRows}</Table>
 </Worksheet>
</Workbook>`;

  const blob = new Blob([xml], { type: "application/vnd.ms-excel" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName.endsWith(".xls") ? fileName : `${fileName}.xls`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function escapeXml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}
