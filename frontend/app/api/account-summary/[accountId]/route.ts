import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../../lib/gateway";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ accountId: string }> },
) {
  const { accountId } = await context.params;

  return proxyGatewayRequest(
    request,
    `/api/dashboard/accounts/${accountId}/summary${request.nextUrl.search}`,
  );
}
