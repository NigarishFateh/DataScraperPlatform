export type ExportStatus = "PENDING" | "GENERATING" | "READY" | "FAILED" | "EXPIRED";

export type ExportFormat = "EXCEL" | "CSV" | "JSON" | "PDF" | "XML" | "XLSX";

export type ExportResponse = {
  id: string;
  jobId: string;
  format: ExportFormat;
  status: ExportStatus;
  fileName: string | null;
  rowCount: number;
  fileSizeBytes: number;
  downloadUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
};
