import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

import { clearSessionCookies, createAuthRequest, setAuthFlowCookies } from "../../../../lib/auth";

export async function GET(request: NextRequest) {
  const mode = request.nextUrl.searchParams.get("mode") === "register" ? "register" : "login";
  const authRequest = createAuthRequest(request.nextUrl.origin, mode);
  const response = NextResponse.redirect(authRequest.url);

  clearSessionCookies(response);
  setAuthFlowCookies(response, authRequest.verifier, authRequest.state);

  return response;
}
