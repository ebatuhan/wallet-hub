import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

import { buildLogoutUrl, clearSessionCookies } from "../../../../lib/auth";

export async function GET(request: NextRequest) {
  const target = buildLogoutUrl(request.nextUrl.origin);
  const response = NextResponse.redirect(target);

  clearSessionCookies(response);

  return response;
}
