import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../lib/gateway";

export async function POST(request: NextRequest) {
  const payload = (await request.json()) as unknown;

  return proxyGatewayRequest(request, "/api/assistant/chat", {
    method: "POST",
    body: payload,
  });
}
