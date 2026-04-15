import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../lib/gateway";

export async function GET(request: NextRequest) {
  return proxyGatewayRequest(request, "/api/budgets");
}

export async function POST(request: NextRequest) {
  const payload = (await request.json()) as unknown;

  return proxyGatewayRequest(request, "/api/budgets", {
    method: "POST",
    body: payload,
  });
}
