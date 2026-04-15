import type { NextRequest } from "next/server";

import { proxyGatewayRequest } from "../../../../lib/gateway";

export async function PUT(
  request: NextRequest,
  context: { params: Promise<{ budgetId: string }> },
) {
  const { budgetId } = await context.params;
  const payload = (await request.json()) as unknown;

  return proxyGatewayRequest(request, `/api/budgets/${budgetId}`, {
    method: "PUT",
    body: payload,
  });
}

export async function DELETE(
  request: NextRequest,
  context: { params: Promise<{ budgetId: string }> },
) {
  const { budgetId } = await context.params;

  return proxyGatewayRequest(request, `/api/budgets/${budgetId}`, {
    method: "DELETE",
  });
}
