//  src/services/auditApi.ts
export interface AuditLogItem {
  id: string;
  activity: string;
  linkedItem?: string;
  user: string;
  timestamp: string;            // ISO string
  status: 'SUCCESS' | 'FAILED';
}

/* ───────── helpers ───────── */

const API_BASE = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const authHeaders = (): HeadersInit => ({
  Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`,
  'Content-Type': 'application/json'
});

/*  type-guard that keeps only real strings  */
const isString = (v: unknown): v is string =>
  typeof v === 'string' && v.trim().length > 0;


function buildQuery(
  params: Record<string, string | null | undefined>
): string {
  // explicit guard – narrows each tuple from
  //   [string,string | null | undefined]  →  [string,string]
  function isPair(entry: [string, string | null | undefined]):
    entry is [string, string] {
    return typeof entry[1] === 'string' && entry[1].trim().length > 0;
  }

  const cleaned = Object.entries(params).filter(isPair);   // now [string,string][]
  return new URLSearchParams(cleaned).toString();
}
/* ───────── API ───────── */

export const auditApi = {
  async list(params: Record<string, string | null | undefined> = {}):
    Promise<AuditLogItem[]> {

    const url = `${API_BASE}/api/audit?${buildQuery(params)}`;
    const res = await fetch(url, { headers: authHeaders() });

    if (!res.ok) throw new Error(`API ${res.status}`);
    return res.json();
  },

exportCsv(params: Record<string, string | null | undefined> = {}) {
  const url = `${API_BASE}/api/audit/export?${buildQuery(params)}`;
  window.open(url, '_blank', 'noopener,noreferrer');           // open first
  // then add the header via fetch‐stream or build the CSV in list()
}
};
