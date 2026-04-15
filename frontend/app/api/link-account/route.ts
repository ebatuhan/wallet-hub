import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../lib/gateway";

export async function POST(request: NextRequest) {
  return proxyGatewayRequest(request, "/api/plaid/mock", {
    method: "POST",
  });
}
