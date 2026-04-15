import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../lib/gateway";

export async function GET(request: NextRequest) {
  return proxyGatewayRequest(request, "/api/transactions/categories/primary");
}
