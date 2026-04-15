import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

import {
  clearAuthFlowCookies,
  createSessionFromCode,
  readAuthFlowState,
  setSessionCookies,
} from "../../../../lib/auth";

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  const state = request.nextUrl.searchParams.get("state");
  const authState = readAuthFlowState(request);

  if (!code) {
    const response = NextResponse.redirect(new URL("/sign-in?error=missing_code", request.url));
    clearAuthFlowCookies(response);
    return response;
  }

  if (!state || !authState.state || state !== authState.state || !authState.verifier) {
    const response = NextResponse.redirect(new URL("/sign-in?error=state", request.url));
    clearAuthFlowCookies(response);
    return response;
  }

  try {
    const session = await createSessionFromCode(request.nextUrl.origin, code, authState.verifier);
    const response = NextResponse.redirect(new URL("/app", request.url));

    setSessionCookies(response, session);
    clearAuthFlowCookies(response);

    return response;
  } catch (error) {
    console.error("Keycloak auth callback failed", error);
    const response = NextResponse.redirect(new URL("/sign-in?error=oauth", request.url));
    clearAuthFlowCookies(response);
    return response;
  }
}
