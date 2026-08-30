import { backendEnabled, backendErrorResponse, backendRequest } from "@/lib/backend";

export async function GET() {
  if (!backendEnabled()) return Response.json({ status: "ok", backend: "demo" });
  try {
    const backend = await backendRequest<{ status: string }>("/health");
    return Response.json({ status: "ok", backend: backend.status });
  } catch (error) {
    return backendErrorResponse(error);
  }
}
